/** A place the user picked: display text plus coordinates. Coordinates are null until a place is chosen. */
export const EMPTY_LOCATION = Object.freeze({ text: '', lat: null, lng: null });

export function isValidLocation(location) {
  return (
    Boolean(location) &&
    Number.isFinite(location.lat) &&
    Number.isFinite(location.lng) &&
    location.lat >= -90 &&
    location.lat <= 90 &&
    location.lng >= -180 &&
    location.lng <= 180
  );
}
