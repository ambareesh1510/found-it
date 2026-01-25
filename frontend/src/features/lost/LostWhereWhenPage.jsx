import { useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import FlowHeader from '../../ui/FlowHeader';
import Field from '../../ui/Field';
import LocationField from '../reports/LocationField';
import { LOST_CATEGORIES } from '../../core/categories';
import { EMPTY_LOCATION } from '../../core/location';
import { toLocalDateInputValue } from '../../core/eventTime';
import { buildLostSearchRequest, buildTimelineSearchRequest, validateLostDeclaration } from '../../core/lostSearch';
import { runLostSearch } from '../../core/services';
import {
  captureTimelinePoint,
  getRecentTimelinePoints,
  isTimelineEnabled,
  setTimelineEnabled,
} from '../../platform/locationTimeline';

const ALL_FIELDS = ['location', 'date', 'time'];

/** Step 2 of I Lost: where and when. Runs the search and hands the results to the results screen. */
export default function LostWhereWhenPage({ user }) {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const category = params.get('category') ?? '';

  const [location, setLocation] = useState(EMPTY_LOCATION);
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [touched, setTouched] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [mode, setMode] = useState('manual');
  const [timelineReady, setTimelineReady] = useState(() => isTimelineEnabled(user.id));
  const [timelineUnavailable, setTimelineUnavailable] = useState(false);

  if (!LOST_CATEGORIES.includes(category)) {
    return <Navigate to="/lost" replace />;
  }

  const declaration = { category, location, date, time };
  const validation = mode === 'timeline'
    ? { valid: timelineReady, errors: {} }
    : validateLostDeclaration(declaration);
  const errorFor = (field) => (touched[field] ? validation.errors[field] : undefined);
  const touch = (field) => setTouched((prev) => ({ ...prev, [field]: true }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (mode === 'manual') setTouched(Object.fromEntries(ALL_FIELDS.map((field) => [field, true])));
    if (!validation.valid || submitting) return;

    setSubmitting(true);
    setSearchError('');
    try {
      const payload = mode === 'timeline'
        ? buildTimelineSearchRequest({ category, timelinePoints: getRecentTimelinePoints(user.id) })
        : buildLostSearchRequest(declaration);
      if (mode === 'timeline' && payload.timelinePoints.length === 0) {
        throw new Error('No Timeline locations are available from the previous 24 hours. Keep FindIt open to record one.');
      }
      const result = await runLostSearch(payload);
      navigate('/lost/results', { state: { declaration: { ...declaration, useTimeline: mode === 'timeline' }, result } });
    } catch (err) {
      setSearchError(err.message || 'The search did not finish. Please try again.');
      setSubmitting(false);
    }
  };

  return (
    <main className="flow-page">
      <div className="flow-inner">
        <FlowHeader breadcrumb="Step 2 of 2" title="Where and when?" />

        <form className="flow-card flow-form" onSubmit={handleSubmit} noValidate>
          {/* The item is part of the search, so it sits at the top of this card instead of in its own section. */}
          <div className="flow-item-row">
              <span className="flow-label">Item category</span>
            <div className="flow-item-row__controls">
              <span className="flow-chip">{category}</span>
              <button type="button" className="flow-link" onClick={() => navigate('/lost')}>
                Change category
              </button>
            </div>
          </div>

          <fieldset className="flow-field flow-fieldset">
            <legend className="flow-label">How should we search?</legend>
            <div className="flow-options">
              <label className={`flow-option${mode === 'manual' ? ' flow-option--selected' : ''}`}>
                <input type="radio" name="search-mode" value="manual" checked={mode === 'manual'} onChange={() => setMode('manual')} />
                <span>
                  <strong>Enter a place and time</strong>
                </span>
              </label>
              <label
                className={`flow-option${mode === 'timeline' ? ' flow-option--selected' : ''}`}
                aria-disabled={timelineUnavailable}
              >
                <input
                  type="radio"
                  name="search-mode"
                  value="timeline"
                  checked={mode === 'timeline'}
                  disabled={timelineUnavailable}
                  onChange={async () => {
                    setSearchError('');
                    try {
                      setTimelineEnabled(user.id, true);
                      await captureTimelinePoint(user.id, Date.now(), true);
                      setTimelineReady(true);
                      setMode('timeline');
                    } catch (error) {
                      setTimelineEnabled(user.id, false);
                      setTimelineReady(false);
                      setTimelineUnavailable(true);
                      setSearchError(error.message || 'Location access is required for Timeline search.');
                    }
                  }}
                />
                <span>
                  <strong>Use my location history</strong>
                  <small>Search the last 24 hours within half a mile of places recorded while FindIt was open.</small>
                  {timelineUnavailable && <small>Location permission was denied. Enable it in browser settings to try again.</small>}
                </span>
              </label>
            </div>
          </fieldset>

          <LocationField
            id="lost-location"
            label="Where did you lose it?"
            required
            disabled={mode === 'timeline'}
            value={location}
            error={errorFor('location')}
            onChange={(next) => {
              touch('location');
              setLocation(next);
            }}
          />

          <Field id="lost-date" label="When did you lose it?" required error={errorFor('date')}>
            <input
              id="lost-date"
              type="date"
              className={`flow-input${errorFor('date') ? ' flow-input--invalid' : ''}`}
              value={date}
              max={toLocalDateInputValue()}
              disabled={mode === 'timeline'}
              onChange={(event) => setDate(event.target.value)}
              onBlur={() => touch('date')}
            />
          </Field>

          <Field id="lost-time" label="About what time?" required error={errorFor('time')}>
            <input
              id="lost-time"
              type="time"
              className={`flow-input${errorFor('time') ? ' flow-input--invalid' : ''}`}
              value={time}
              disabled={mode === 'timeline'}
              onChange={(event) => setTime(event.target.value)}
              onBlur={() => touch('time')}
            />
          </Field>

          {searchError && (
            <div className="flow-banner flow-banner--danger" role="alert">
              {searchError}
            </div>
          )}

          <button type="submit" className="flow-btn flow-btn--lost" disabled={!validation.valid || submitting}>
            {submitting ? 'Searching…' : 'Search found items'}
          </button>
        </form>
      </div>
    </main>
  );
}
