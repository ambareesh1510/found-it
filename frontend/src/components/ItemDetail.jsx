import { authFetch } from '../core/http.js';
import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { API_BASE_URL } from '../core/config.js';
import { confirmItemResolution } from '../core/services.js';
import './ItemDetail.css';

const STATUS_LABELS = {
  OPEN: 'Open',
  PENDING_RESOLUTION: 'Pending confirmation',
  RESOLVED: 'Resolved',
};

function formatItemLocation(locationText, detailLocation) {
  const base = locationText?.trim() || 'Unknown location';
  const detail = detailLocation?.trim();
  return detail ? `${base} ${detail}` : base;
}

function ItemDetail({ user }) {
  const { itemId } = useParams();
  const navigate = useNavigate();

  const [item, setItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [chatError, setChatError] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [resolveError, setResolveError] = useState('');

  useEffect(() => {
    const fetchItem = async () => {
      try {
        const response = await authFetch(`${API_BASE_URL}/items/${itemId}`, {
          credentials: 'include',
        });
        if (!response.ok) throw new Error('Item not found');
        const data = await response.json();
        setItem(data);
      } catch {
        setError('Could not load item details.');
      } finally {
        setLoading(false);
      }
    };

    fetchItem();
  }, [itemId]);

  const handleChat = async () => {
    setChatLoading(true);
    setChatError('');

    try {
      const response = await authFetch(`${API_BASE_URL}/chat/threads`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          itemId: item.itemId,
          participantUserId: item.posterId
        })
      });

      if (!response.ok) {
        const errData = await response.json();
        throw new Error(errData.message || 'Failed to open chat.');
      }

      const thread = await response.json();
      navigate(`/chat?threadId=${thread.threadId}`);
    } catch (err) {
      setChatError(err.message || 'Could not open chat.');
    } finally {
      setChatLoading(false);
    }
  };

  const handleConfirmResolution = async () => {
    setResolveLoading(true);
    setResolveError('');
    try {
      const updated = await confirmItemResolution(item.itemId);
      setItem((prev) => ({ ...prev, status: updated.status }));
    } catch (err) {
      setResolveError(err?.message || 'Could not confirm the resolution.');
    } finally {
      setResolveLoading(false);
    }
  };

  const handleDelete = async () => {
    setConfirmingDelete(false);
    setDeleteLoading(true);
    setDeleteError('');

    try {
      const response = await authFetch(`${API_BASE_URL}/items/${itemId}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        navigate('/');
      } else {
        let message = 'Failed to delete item.';
        try {
          const errData = await response.json();
          message = errData.message || message;
        } catch {
          const text = await response.text();
          if (text) message = text;
        }
        setDeleteError(message);
      }
    } catch {
      setDeleteError('Could not connect to the server.');
    } finally {
      setDeleteLoading(false);
    }
  };

  if (loading) return <div className="detail-loading">Loading...</div>;
  if (error) return <div className="detail-error">{error}</div>;

  const isOwner = user && item.posterId === user.id;

  return (
    <div className="detail-container">
      <div className="detail-card">

        <header className="detail-header">
          <button className="back-btn" onClick={() => navigate('/')}>&larr; Back</button>
          {isOwner && (
            <div className="owner-actions">
              <button className="edit-btn" onClick={() => navigate(`/items/${itemId}/edit`)}>
                Edit Item
              </button>
              {confirmingDelete ? (
                <span className="delete-confirm" role="group" aria-label="Confirm delete">
                  <span>Delete this item? This cannot be undone.</span>
                  <button className="delete-btn" onClick={handleDelete} disabled={deleteLoading}>
                    {deleteLoading ? 'Deleting...' : 'Yes, delete'}
                  </button>
                  <button className="back-btn" onClick={() => setConfirmingDelete(false)} disabled={deleteLoading}>
                    Cancel
                  </button>
                </span>
              ) : (
                <button className="delete-btn" onClick={() => setConfirmingDelete(true)} disabled={deleteLoading}>
                  Delete Item
                </button>
              )}
            </div>
          )}
        </header>

        {deleteError && <div className="delete-error">{deleteError}</div>}

        <div className="detail-images">
          {item.imageUrl && <img src={item.imageUrl} alt={item.title} className="detail-image" />}
          {item.imageUrl2 && (
            <img src={item.imageUrl2} alt={`${item.title} (2)`} className="detail-image" />
          )}
        </div>

        <div className="detail-body">
          <h2 className="detail-title">{item.title}</h2>

          {item.category && (
            <p className="detail-meta">Category: <strong>{item.category}</strong></p>
          )}

          {item.tags && item.tags.length > 0 && (
            <div className="detail-tags">
              {item.tags.map(tag => (
                <span key={tag} className="detail-tag">🏷 {tag}</span>
              ))}
            </div>
          )}

          {item.description && (
            <p className="detail-description">{item.description}</p>
          )}

          <div className="detail-info">
            {(item.locationText || item.detailLocation) && (
              <p>📍 <strong>Location:</strong> {formatItemLocation(item.locationText, item.detailLocation)}</p>
            )}
            {item.eventTime && (
              <p>🕐 <strong>When:</strong> {new Date(item.eventTime).toLocaleString()}</p>
            )}
            <p>👤 <strong>Posted by:</strong> {item.posterUsername}</p>
            <p>📌 <strong>Status:</strong> {STATUS_LABELS[item.status] || item.status}</p>
          </div>

          {!isOwner && item.status === 'OPEN' && (
            <div className="chat-section">
              {chatError && <div className="chat-error">{chatError}</div>}
              <button className="chat-btn" onClick={handleChat} disabled={chatLoading}>
                {chatLoading ? 'Opening...' : 'Chat'}
              </button>
            </div>
          )}

          {/* The chosen counterparty can confirm the resolution here as well as from chat. */}
          {!isOwner && item.status === 'PENDING_RESOLUTION' && item.resolvedWithUserId === user?.id && (
            <div className="chat-section">
              {resolveError && <div className="chat-error">{resolveError}</div>}
              <p>{item.posterUsername} marked this item as resolved with you.</p>
              <button className="chat-btn" onClick={handleConfirmResolution} disabled={resolveLoading}>
                {resolveLoading ? 'Confirming...' : 'Confirm resolution'}
              </button>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}

export default ItemDetail;
