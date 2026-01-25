// PLATFORM SEAM. Google Places autocomplete for the web build.
// The iOS build can replace this with a native place search that calls `onPlace` with the same shape:
// `{ text, lat, lng }`. `attachPlaceAutocomplete` returns a detach function (or null when Places is unavailable).

/**
 * Attaches Google Places autocomplete to an <input>. Calls `onPlace({ text, lat, lng })` when the user
 * picks a suggestion. Returns a cleanup function, or null if the Places library has not loaded.
 */
export function attachPlaceAutocomplete(input, onPlace) {
  const google = globalThis.google;
  if (!input || !google?.maps?.places) return null;

  const autocomplete = new google.maps.places.Autocomplete(input, {
    fields: ['formatted_address', 'name', 'geometry'],
  });
  const listener = autocomplete.addListener('place_changed', () => {
    const place = autocomplete.getPlace();
    const point = place.geometry?.location;
    if (!point) return;
    onPlace({
      text: place.formatted_address || place.name || input.value,
      lat: point.lat(),
      lng: point.lng(),
    });
  });

  return () => {
    listener.remove?.();
    google.maps.event.clearInstanceListeners(autocomplete);
  };
}
