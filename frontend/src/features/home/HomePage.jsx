import { useLocation, useNavigate } from 'react-router-dom';
import { IconPin, IconSearch } from '../../ui/icons';
import '../../ui/flows.css';

export default function HomePage() {
  const navigate = useNavigate();
  const { state } = useLocation();

  return (
    <main className="flow-page home-page">
      <div className="home-inner">
        <header className="home-intro">
          <p className="home-eyebrow">Get started</p>
          <h1 className="home-heading">What brings you to FindIt?</h1>
        </header>

        {state?.notice && <div className="flow-banner">{state.notice}</div>}

        <button type="button" className="home-panel home-panel--lost" onClick={() => navigate('/lost')}>
          <span className="home-panel__icon">
            <IconPin />
          </span>
          <span className="home-panel__body">
            <strong className="home-panel__title">I Lost an Item</strong>
            <span className="home-panel__desc">Search reports from people who found something nearby.</span>
          </span>
          <span className="home-panel__cta">Start searching &rarr;</span>
        </button>

        <button type="button" className="home-panel home-panel--found" onClick={() => navigate('/found')}>
          <span className="home-panel__icon">
            <IconSearch />
          </span>
          <span className="home-panel__body">
            <strong className="home-panel__title">I Found an Item</strong>
            <span className="home-panel__desc">
              See missing-item reports and contact the owner, or report what you found.
            </span>
          </span>
          <span className="home-panel__cta">Browse &amp; report &rarr;</span>
        </button>
      </div>
    </main>
  );
}
