import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import FlowHeader from '../../ui/FlowHeader';
import ItemPhoto from '../../ui/ItemPhoto';
import { formatEventTime } from '../../core/eventTime';
import { useContactPoster } from '../found/useContactPoster';

/**
 * Results of "I Lost an Item". Reads the search from router state. If the page is opened
 * without it (refresh or a bookmark), it sends the user back to step 1.
 */
export default function LostResultsPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  // Hooks must run before the early return below.
  const { contactingId, error: contactError, contact } = useContactPoster();

  if (!state?.result) {
    return <Navigate to="/lost" replace />;
  }

  const { declaration, result } = state;
  const results = result.results ?? [];
  const count = results.length;

  return (
    <main className="flow-page">
      <div className="flow-inner">
        <FlowHeader breadcrumb="Search results" title="I Lost an Item" />

        <div className="flow-card">
          <span className="flow-chip">{declaration.category}</span>
          <p className="flow-result__meta">
            {declaration.useTimeline
              ? 'Timeline · previous 24 hours · within 0.5 miles'
              : `${declaration.location?.text || 'Chosen location'} · ${declaration.date} at ${declaration.time}`}
          </p>
        </div>

        <p className="flow-breadcrumb">
          {count === 0 ? 'No matching found items yet.' : `${count} matching found item${count === 1 ? '' : 's'}`}
        </p>

        {contactError && (
          <div className="flow-banner flow-banner--danger" role="alert">
            {contactError}
          </div>
        )}

        {count === 0 && (
          <div className="flow-empty">
            <p className="flow-empty__title">Nothing matches yet.</p>
            <p>Report it as lost so people who find it can see your report.</p>
          </div>
        )}

        {results.map((item) => (
          <article key={item.itemId} className="flow-result">
            <ItemPhoto src={item.imageUrl} alt={item.title} />
            <div className="flow-result__body">
              <span className="flow-chip">{item.category}</span>
              <h2 className="flow-result__title">{item.title}</h2>
              <p className="flow-result__meta">
                {item.locationText || 'Location not given'} · {formatEventTime(item.eventTime)}
              </p>
              {item.description && <p className="flow-result__meta">{item.description}</p>}
              <button
                type="button"
                className="flow-btn flow-btn--found flow-btn--small"
                onClick={() => contact(item.itemId)}
                disabled={contactingId !== null}
              >
                {contactingId === item.itemId ? 'Opening chat…' : 'Contact'}
              </button>
            </div>
          </article>
        ))}

        <button
          type="button"
          className="flow-btn flow-btn--lost"
          onClick={() => navigate('/lost/report', { state: { prefill: declaration } })}
        >
          Report as lost
        </button>
      </div>
    </main>
  );
}
