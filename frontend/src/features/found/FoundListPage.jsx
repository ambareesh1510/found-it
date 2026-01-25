import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ItemPhoto from '../../ui/ItemPhoto';
import { formatEventTime } from '../../core/eventTime';
import { formatRewardCents } from '../../core/reports';
import { fetchMissingReports } from '../../core/services';
import { useContactPoster } from './useContactPoster';

/**
 * "I Found an Item": open LOST reports. Shows only fields safe for public viewing
 * (the server strips detailLocation for non-posters). Contact opens a chat with the poster.
 */
export default function FoundListPage({ user }) {
  const navigate = useNavigate();
  const { contactingId, error: contactError, contact } = useContactPoster();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    let cancelled = false;
    fetchMissingReports()
      .then((items) => {
        if (!cancelled) setReports(items ?? []);
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Hide your own reports: contacting yourself makes no sense.
  const visible = reports.filter((report) => report.posterId !== user?.id);
  const error = loadError || contactError;

  return (
    <main className="flow-page">
      <div className="flow-inner">
        {/* Tab root: the tab bar handles navigation, so there is no back button or breadcrumb. */}
        <header className="flow-header">
          <h1 className="flow-title">I Found an Item</h1>
        </header>

        <button type="button" className="flow-btn flow-btn--found" onClick={() => navigate('/found/report')}>
          Report an item I found
        </button>

        {error && (
          <div className="flow-banner flow-banner--danger" role="alert">
            {error}
          </div>
        )}

        {loading && <p className="flow-hint">Loading reports…</p>}

        {!loading && !loadError && visible.length === 0 && (
          <div className="flow-empty">
            <p className="flow-empty__title">No open missing-item reports right now.</p>
            <p>Check back later, or report an item you found.</p>
          </div>
        )}

        {visible.map((report) => {
          const reward = formatRewardCents(report.rewardCents);
          return (
            <article key={report.itemId} className="flow-result">
              <ItemPhoto src={report.imageUrl} alt={report.title} />
              <div className="flow-result__body">
                <div className="flow-chip-row">
                  <span className="flow-chip">{report.category}</span>
                  {reward && <span className="flow-chip flow-chip--reward">Reward: {reward}</span>}
                </div>
                <h2 className="flow-result__title">{report.title}</h2>
                <p className="flow-result__meta">
                  {report.locationText || 'Location not given'} · {formatEventTime(report.eventTime)}
                </p>
                {report.description && <p className="flow-result__meta">{report.description}</p>}
                <button
                  type="button"
                  className="flow-btn flow-btn--found flow-btn--small"
                  onClick={() => contact(report.itemId)}
                  disabled={contactingId !== null}
                >
                  {contactingId === report.itemId ? 'Opening chat…' : 'Contact'}
                </button>
              </div>
            </article>
          );
        })}
      </div>
    </main>
  );
}
