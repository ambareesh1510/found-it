// Photo policy shared by every platform. How the file is read lives in platform/photos.js.
export const MAX_PHOTO_BYTES = 4 * 1024 * 1024;

/** Returns an error message for a rejected photo file, or null when it is acceptable. */
export function validatePhotoFile(file) {
  if (!file) return 'Choose a photo to upload.';
  if (!file.type || !file.type.startsWith('image/')) return 'Choose an image file (JPG, PNG, or similar).';
  if (file.size > MAX_PHOTO_BYTES) return 'Choose a photo under 4 MB.';
  return null;
}
