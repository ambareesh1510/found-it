import { useEffect, useRef, useState } from 'react';
import { useJsApiLoader } from '@react-google-maps/api';
import Field from '../../ui/Field';
import { EMPTY_LOCATION, isValidLocation } from '../../core/location';
import { attachPlaceAutocomplete } from '../../platform/places';
import { getCurrentCoordinates } from '../../platform/geolocation';

// Same loader id and libraries as the map view, so the Google script loads once for the whole app.
const MAP_LOADER_ID = 'lost-and-found-google-map';
const GOOGLE_MAP_LIBRARIES = ['places'];
const CURRENT_LOCATION_TEXT = 'Current location';

/**
 * Place picker used by both flows. Emits `{ text, lat, lng }`.
 * Typing clears the coordinates until a suggestion is picked, so a stale point is never submitted.
 */
export default function LocationField({ id, label, required = false, value = EMPTY_LOCATION, onChange, error, hint, disabled = false }) {
  const inputRef = useRef(null);
  const onChangeRef = useRef(onChange);
  const [locating, setLocating] = useState(false);
  const [locateError, setLocateError] = useState('');
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';
  const { isLoaded } = useJsApiLoader({
    id: MAP_LOADER_ID,
    googleMapsApiKey: apiKey,
    libraries: GOOGLE_MAP_LIBRARIES,
  });

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    if (!isLoaded || disabled) return undefined;
    const detach = attachPlaceAutocomplete(inputRef.current, (place) => {
      setLocateError('');
      onChangeRef.current(place);
    });
    return detach || undefined;
  }, [disabled, isLoaded]);

  const handleTyping = (event) => {
    onChange({ text: event.target.value, lat: null, lng: null });
  };

  const handleUseCurrentLocation = async () => {
    setLocating(true);
    setLocateError('');
    try {
      const { lat, lng } = await getCurrentCoordinates();
      onChange({ text: value.text?.trim() || CURRENT_LOCATION_TEXT, lat, lng });
    } catch (err) {
      setLocateError(err.message);
    } finally {
      setLocating(false);
    }
  };

  const pinned = isValidLocation(value);
  const shownError = error || locateError;

  return (
    <Field
      id={id}
      label={label}
      required={required}
      error={shownError}
      hint={hint || (pinned ? 'Location set.' : undefined)}
    >
      <div className="flow-location-row">
        <input
          id={id}
          ref={inputRef}
          type="text"
          className={`flow-input${shownError ? ' flow-input--invalid' : ''}`}
          value={value.text ?? ''}
          onChange={handleTyping}
          placeholder="Search a building, street, or landmark"
          autoComplete="off"
          aria-invalid={Boolean(shownError)}
          disabled={disabled}
        />
        <button
          type="button"
          className="flow-btn flow-btn--secondary flow-btn--small"
          onClick={handleUseCurrentLocation}
          disabled={disabled || locating}
        >
          {locating ? 'Finding you…' : 'Use my current location'}
        </button>
      </div>
    </Field>
  );
}
