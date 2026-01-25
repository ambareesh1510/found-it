import { authFetch } from '../core/http.js';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../core/config.js';
import { markItemResolved } from '../core/services.js';
import './Profile.css';
import {
  captureTimelinePoint,
  isTimelineEnabled,
  setTimelineEnabled,
} from '../platform/locationTimeline';

function formatItemLocation(locationText, detailLocation) {
  const base = locationText?.trim() || 'Unknown location';
  const detail = detailLocation?.trim();
  return detail ? `${base} ${detail}` : base;
}

const STATUS_LABELS = {
  OPEN: 'Open',
  PENDING_RESOLUTION: 'Pending confirmation',
  RESOLVED: 'Resolved',
};

function statusLabel(status) {
  return STATUS_LABELS[status] || status;
}

/** Item thumbnail with a text placeholder when there is no photo or it fails to load. */
function MyItemPhoto({ src, alt }) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    return <div className="my-item-image my-item-image--empty" role="img" aria-label="No photo">No photo</div>;
  }
  return <img src={src} alt={alt} className="my-item-image" onError={() => setFailed(true)} />;
}

function Profile({ user, onLogout, onDeleteAccount }) {
  const [activeTab, setActiveTab] = useState('myProfile');
  const [items, setItems] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [itemsError, setItemsError] = useState('');
  const [showClaimModal, setShowClaimModal] = useState(false);
  const [claimCandidates, setClaimCandidates] = useState([]);
  const [selectedClaimant, setSelectedClaimant] = useState(null);
  const [claimModalItemId, setClaimModalItemId] = useState(null);
  const [claimModalLoading, setClaimModalLoading] = useState(false);
  const [resolveError, setResolveError] = useState('');
  const [resolveSubmitting, setResolveSubmitting] = useState(false);
  const [timelineEnabled, setTimelineEnabledState] = useState(() => isTimelineEnabled(user?.id));
  const [timelineError, setTimelineError] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  // On success the parent signs the user out and navigates away, so this component unmounts.
  const handleDeleteAccount = async () => {
    setDeleting(true);
    setDeleteError('');
    try {
      await onDeleteAccount();
    } catch (err) {
      setDeleteError(err?.message || 'Could not delete your account. Please try again.');
      setDeleting(false);
    }
  };
  const navigate = useNavigate();

  const userName = user?.username || "GT Student";
  const userEmail = user?.email || "student@gatech.edu";
  //const userPic = user?.picture || "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";
  const userPic = user?.picture || `https://ui-avatars.com/api/?name=${userName}&background=random&color=fff&size=128`;

  useEffect(() => {
    if (activeTab !== 'myItems' || !user?.id) return;

    const fetchMyItems = async () => {
      setItemsLoading(true);
      setItemsError('');

      try {
        const response = await authFetch(`${API_BASE_URL}/items?userId=${user.id}`, {
          method: 'GET',
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error('Failed to load your items.');
        }

        const data = await response.json();
        setItems(Array.isArray(data) ? data : []);
      } catch {
        setItemsError('Could not load your items right now.');
      } finally {
        setItemsLoading(false);
      }
    };

    fetchMyItems();
  }, [activeTab, user?.id]);

  // Items are grouped first by what they are (Lost vs Found), then split into active and resolved.
  const groupedItems = useMemo(() => {
    const byType = { LOST: [], FOUND: [] };
    for (const item of items) {
      const bucket = item.postType === 'FOUND' ? byType.FOUND : byType.LOST;
      bucket.push(item);
    }
    const split = (list) => ({
      active: list.filter((item) => item.status !== 'RESOLVED'),
      resolved: list.filter((item) => item.status === 'RESOLVED'),
    });
    return { LOST: split(byType.LOST), FOUND: split(byType.FOUND) };
  }, [items]);

  const closeClaimModal = () => {
    setShowClaimModal(false);
    setClaimCandidates([]);
    setSelectedClaimant(null);
    setClaimModalItemId(null);
    setResolveError('');
  };

  const handleMarkResolved = async (itemId) => {
    setResolveError('');
    try {
      setClaimModalLoading(true);
      setClaimModalItemId(itemId);
      setSelectedClaimant(null);
      setShowClaimModal(true);

      const response = await authFetch(`${API_BASE_URL}/chat/threads?itemId=${itemId}`, {
        method: 'GET',
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error('Could not load the people who messaged about this item.');
      }

      const threads = await response.json();
      const candidates = threads
        .map(thread => ({
          userId: thread.otherUserId,
          username: thread.otherUsername,
          picture: thread.otherUserPicture
        }))
        .filter((candidate, index, self) =>
          self.findIndex(u => u.userId === candidate.userId) === index
        );

      setClaimCandidates(candidates);
    } catch (err) {
      setResolveError(err?.message || 'Could not load chat participants.');
    } finally {
      setClaimModalLoading(false);
    }
  };

  const handleConfirmResolution = async () => {
    if (!claimModalItemId) return;

    setResolveSubmitting(true);
    setResolveError('');
    try {
      const updated = await markItemResolved(claimModalItemId, selectedClaimant?.userId ?? null);
      // With a counterparty the item is now PENDING_RESOLUTION; without one it is RESOLVED.
      setItems(prev =>
        prev.map(item =>
          item.itemId === claimModalItemId
            ? { ...item, status: updated.status, resolvedWithUserId: updated.resolvedWithUserId }
            : item
        )
      );
      closeClaimModal();
    } catch (err) {
      setResolveError(err?.message || 'Could not mark this item as resolved.');
    } finally {
      setResolveSubmitting(false);
    }
  };

  const renderItemCard = (item) => {
    const isResolved = item.status === 'RESOLVED';
    const isPending = item.status === 'PENDING_RESOLUTION';
    return (
      <article key={item.itemId} className="my-item-card">
        <div className="my-item-image-wrap" onClick={() => navigate(`/items/${item.itemId}`)}>
          <MyItemPhoto src={item.imageUrl} alt={item.title} />
        </div>
        <div className="my-item-body">
          <div className="my-item-topline">
            <h4>{item.title}</h4>
            <span className={`my-item-status ${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span>
          </div>
          <p className="my-item-location">📍 {formatItemLocation(item.locationText, item.detailLocation)}</p>
          {isPending && (
            <p className="my-item-pending-note">Waiting for the other person to confirm in chat.</p>
          )}
          <div className="my-item-actions">
            {!isResolved && !isPending && (
              <button className="my-item-btn secondary" onClick={() => handleMarkResolved(item.itemId)}>
                Mark Resolved
              </button>
            )}
            <button className="my-item-btn primary" onClick={() => navigate(`/items/${item.itemId}/edit`)}>
              Edit
            </button>
          </div>
        </div>
      </article>
    );
  };

  const renderItemCategory = (heading, group) => {
    const total = group.active.length + group.resolved.length;
    return (
      <section className="item-category">
        <h3 className="item-category-title">{heading}</h3>
        {total === 0 ? (
          <p className="items-empty">Nothing here yet.</p>
        ) : (
          <>
            <section className="item-group">
              <h4 className="item-group-title">Active</h4>
              {group.active.length === 0 ? (
                <p className="items-empty">No active items.</p>
              ) : (
                <div className="my-items-grid">{group.active.map(renderItemCard)}</div>
              )}
            </section>
            <section className="item-group">
              <h4 className="item-group-title">Resolved</h4>
              {group.resolved.length === 0 ? (
                <p className="items-empty">No resolved items.</p>
              ) : (
                <div className="my-items-grid">{group.resolved.map(renderItemCard)}</div>
              )}
            </section>
          </>
        )}
      </section>
    );
  };

  const renderContent = () => {
    switch (activeTab) {
      case 'myProfile':
        return (
          <div className="tab-content">
            <h2>My Profile</h2>
            <p>Welcome back, {userName}! Here you can view your personal details.</p>
          </div>
        );
      case 'myItems':
        return (
          <div className="tab-content">
            <h2>My Items</h2>

            {itemsLoading && <p className="items-message">Loading your items...</p>}
            {itemsError && <p className="items-message error">{itemsError}</p>}

            {!itemsLoading && !itemsError && (
              <>
                {renderItemCategory('Lost Items', groupedItems.LOST)}
                {renderItemCategory('Found Items', groupedItems.FOUND)}
              </>
            )}
          </div>
        );
      case 'settings':
        return (
          <div className="tab-content">
            <h2>Settings</h2>
            <label className="skip-label">
              <input
                type="checkbox"
                checked={timelineEnabled}
                onChange={async (event) => {
                  const enabled = event.target.checked;
                  setTimelineError('');
                  setTimelineEnabled(user.id, enabled);
                  setTimelineEnabledState(enabled);
                  if (enabled) {
                    try {
                      await captureTimelinePoint(user.id, Date.now(), true);
                    } catch (error) {
                      setTimelineEnabled(user.id, false);
                      setTimelineEnabledState(false);
                      setTimelineError(error.message || 'Location access could not be enabled.');
                    }
                  }
                }}
              />
              <span>Location Access</span>
            </label>
            <p>Records coarse locations every five minutes while FindIt is open. History stays on this device for seven days.</p>
            {timelineError && <p className="items-message error" role="alert">{timelineError}</p>}
            
            {onDeleteAccount && (
              <section className="danger-zone" aria-labelledby="delete-account-heading">
                <h3 id="delete-account-heading">Delete account</h3>
                <p>
                  Your name, email, and photo are removed and you are signed out. Your posts stay
                  so conversations and claims keep their history; they show as from a deleted user.
                  Your saved searches are deleted.
                </p>
                {confirmingDelete ? (
                  <>
                    <p><strong>This cannot be undone.</strong></p>
                    <div className="danger-zone__actions">
                      <button
                        type="button"
                        className="danger-btn"
                        onClick={handleDeleteAccount}
                        disabled={deleting}
                      >
                        {deleting ? 'Deleting...' : 'Yes, delete my account'}
                      </button>
                      <button
                        type="button"
                        className="secondary-btn"
                        onClick={() => setConfirmingDelete(false)}
                        disabled={deleting}
                      >
                        Cancel
                      </button>
                    </div>
                  </>
                ) : (
                  <button type="button" className="danger-btn" onClick={() => setConfirmingDelete(true)}>
                    Delete account
                  </button>
                )}
                {deleteError && <p role="alert" className="danger-zone__error">{deleteError}</p>}
              </section>
            )}
          </div>
        );
      default:
        return <div>Select a tab</div>;
    }
  };

  return (
    <div className="profile-dashboard">
      {/* SIDEBAR */}
      <aside className="sidebar">

        <div className="sidebar-profile-info">
          <img src={userPic} alt="Profile Avatar" className="profile-avatar" />
          <div className="profile-text">
            <h3 className="profile-name">{userName}</h3>
            <p className="profile-email">{userEmail}</p>
          </div>
        </div>

        <nav className="sidebar-nav">
          <button 
            className={`nav-btn ${activeTab === 'myProfile' ? 'active' : ''}`} 
            onClick={() => setActiveTab('myProfile')}
          >
            My Profile
          </button>
          
          <button 
            className={`nav-btn ${activeTab === 'myItems' ? 'active' : ''}`} 
            onClick={() => setActiveTab('myItems')}
          >
            My Items
          </button>

          <button
            className="nav-btn"
            onClick={() => navigate('/chat')}
          >
            Chats
          </button>
          
          <button 
            className={`nav-btn ${activeTab === 'settings' ? 'active' : ''}`} 
            onClick={() => setActiveTab('settings')}
          >
            Settings
          </button>
        </nav>

        <div className="sidebar-footer">
          <button className="nav-btn logout-btn" onClick={onLogout}>
            Log Out
          </button>
        </div>
      </aside>

      {/* MAIN CONTENT AREA */}
      <main className="dashboard-content">
        {renderContent()}
      </main>

      {/* CLAIM MODAL */}
      {showClaimModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Who claimed this item?</h2>
            <p className="modal-description">Select the person who will receive this item:</p>
            
            {claimModalLoading ? (
              <p className="modal-loading">Loading participants...</p>
            ) : claimCandidates.length === 0 ? (
              <p className="modal-empty">No one has messaged about this item yet.</p>
            ) : (
              <div className="claim-candidates-list">
                {claimCandidates.map(candidate => (
                  <button
                    key={candidate.userId}
                    className={`candidate-btn ${selectedClaimant?.userId === candidate.userId ? 'selected' : ''}`}
                    onClick={() => setSelectedClaimant(candidate)}
                  >
                    <img src={candidate.picture} alt={candidate.username} className="candidate-pic" />
                    <span>{candidate.username}</span>
                  </button>
                ))}
              </div>
            )}

            <div className="modal-skip-option">
              <label className="skip-label">
                <input
                  type="checkbox"
                  checked={!selectedClaimant}
                  onChange={() => setSelectedClaimant(null)}
                />
                <span>No one (just close the item)</span>
              </label>
            </div>

            <p className="modal-hint">
              {selectedClaimant
                ? `${selectedClaimant.username} will be asked to confirm in chat before the item is resolved.`
                : 'The item will be closed right away without confirmation.'}
            </p>

            {resolveError && <p className="modal-error" role="alert">{resolveError}</p>}

            <div className="modal-actions">
              <button
                className="modal-btn secondary"
                onClick={closeClaimModal}
                disabled={resolveSubmitting}
              >
                Cancel
              </button>
              <button
                className="modal-btn primary"
                onClick={handleConfirmResolution}
                disabled={resolveSubmitting || claimModalLoading}
              >
                {resolveSubmitting ? 'Saving...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Profile;
