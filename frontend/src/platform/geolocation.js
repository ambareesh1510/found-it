// PLATFORM SEAM. Uses @capacitor/geolocation: native GPS on iOS, the browser Geolocation API on the web.
import { Geolocation } from '@capacitor/geolocation';

/** Resolves the device position as `{ lat, lng }`. Rejects with a message the screens can show. */
export async function getCurrentCoordinates() {
  try {
    const position = await Geolocation.getCurrentPosition({ enableHighAccuracy: true, timeout: 10000 });
    return { lat: position.coords.latitude, lng: position.coords.longitude };
  } catch {
    throw new Error(
      'Location access was denied or is unavailable. Allow it in your settings, or pick a place from the list instead.',
    );
  }
}
