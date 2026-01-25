// PLATFORM SEAM. Web reads a picked file into a data URL with FileReader.
// The native app takes the photo with the system camera (or photo library) and returns the same data URL.
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';

export function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Could not read that photo. Try another one.'));
    reader.readAsDataURL(file);
  });
}

/**
 * Native only: opens the camera / photo library and resolves a JPEG data URL.
 * Resized to 1600px wide to keep the base64 payload small. Rejects with `error.cancelled = true` when the user backs out.
 */
export async function takePhotoDataUrl() {
  try {
    const photo = await Camera.getPhoto({
      resultType: CameraResultType.DataUrl,
      source: CameraSource.Prompt,
      quality: 80,
      width: 1600,
      correctOrientation: true,
    });
    return photo.dataUrl;
  } catch (error) {
    const cancelled = /cancel/i.test(String(error?.message ?? ''));
    const wrapped = new Error(cancelled ? 'Photo cancelled.' : 'Could not open the camera. Check camera permission in Settings.');
    wrapped.cancelled = cancelled;
    throw wrapped;
  }
}
