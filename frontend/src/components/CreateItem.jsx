import { authFetch } from '../core/http.js';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Autocomplete, GoogleMap, MarkerF, useJsApiLoader } from '@react-google-maps/api';
import { API_BASE_URL } from '../core/config.js';
import { isNativeApp } from '../platform/session.js';
import { takePhotoDataUrl } from '../platform/photos.js';
import './CreateItem.css';

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Failed to read image file.'));
    reader.readAsDataURL(file);
  });
}

function toDateTimeLocalValue(value) {
  if (!value) return '';
  const date = new Date(value);
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

const DEFAULT_CENTER = { lat: 33.7756, lng: -84.3963 };
const GOOGLE_MAP_LIBRARIES = ['places'];

// ── Camera modal ──────────────────────────────────────────────────────────────
function CameraModal({ onCapture, onClose }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      .then(stream => {
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play();
          setReady(true);
        }
      })
      .catch(() => setError('Camera access denied or unavailable.'));

    return () => {
      streamRef.current?.getTracks().forEach(t => t.stop());
    };
  }, []);

  const capture = () => {
    const video = videoRef.current;
    if (!video) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
    streamRef.current?.getTracks().forEach(t => t.stop());
    onCapture(dataUrl);
  };

  return (
    <div className="camera-overlay" onClick={onClose}>
      <div className="camera-modal" onClick={e => e.stopPropagation()}>
        <button className="camera-close" onClick={onClose}>✕</button>
        {error ? (
          <p className="camera-error">{error}</p>
        ) : (
          <>
            <video ref={videoRef} className="camera-video" playsInline muted />
            {ready && (
              <button className="camera-capture" onClick={capture}>📸 Capture</button>
            )}
          </>
        )}
      </div>
    </div>
  );
}

// ── Single image slot ─────────────────────────────────────────────────────────
function ImageSlot({ label, preview, fileInputId, onChange, onCamera, onRemove }) {
  return (
    <div className="image-slot">
      <span className="slot-label">{label}</span>
      {preview ? (
        <div className="image-preview-container">
          <img src={preview} alt="Preview" className="image-preview" />
          <button type="button" className="remove-image-btn" onClick={onRemove}>Remove</button>
        </div>
      ) : (
        <div className="slot-empty">
          <label htmlFor={fileInputId} className="slot-upload-btn">📁 Upload</label>
          <button type="button" className="slot-camera-btn" onClick={onCamera}>📷 Camera</button>
        </div>
      )}
      <input
        id={fileInputId}
        type="file"
        accept="image/*"
        className="file-input-hidden"
        onChange={onChange}
      />
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────
function CreateItem({ user }) {
  const navigate = useNavigate();
  const { itemId } = useParams();
  const isEditing = Boolean(itemId);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEditing);
  const [error, setError] = useState('');
  const autocompleteRef = useRef(null);
  const geocoderRef = useRef(null);

  // Image slot 1
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [currentImageUrl, setCurrentImageUrl] = useState('');

  // Image slot 2
  const [imageFile2, setImageFile2] = useState(null);
  const [imagePreview2, setImagePreview2] = useState(null);
  const [currentImageUrl2, setCurrentImageUrl2] = useState('');

  // Camera
  const [cameraSlot, setCameraSlot] = useState(null); // 1 | 2 | null

  const googleMapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';
  const { isLoaded: isMapLoaded, loadError: mapLoadError } = useJsApiLoader({
    id: 'lost-and-found-google-map',
    googleMapsApiKey,
    libraries: GOOGLE_MAP_LIBRARIES
  });
  const hasGoogleMaps = Boolean(googleMapsApiKey) && isMapLoaded && Boolean(window.google?.maps);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
    locationText: '',
    detailLocation: '',
    eventTime: '',
    latitude: 33.7756,
    longitude: -84.3963
  });

  useEffect(() => {
    if (!isEditing) { setInitialLoading(false); return; }

    setInitialLoading(true);
    setError('');

    const loadItem = async () => {
      try {
        const response = await authFetch(`${API_BASE_URL}/items/${itemId}`, { credentials: 'include' });
        if (!response.ok) throw new Error('Failed to load item.');
        const item = await response.json();

        if (user && item.posterId !== user.id) {
          setError('You can only edit items you created.');
          return;
        }

        setFormData({
          title: item.title || '',
          description: item.description || '',
          category: item.category || '',
          locationText: item.locationText || '',
          detailLocation: item.detailLocation || '',
          eventTime: toDateTimeLocalValue(item.eventTime),
          latitude: item.latitude ?? 33.7756,
          longitude: item.longitude ?? -84.3963
        });
        setCurrentImageUrl(item.imageUrl || '');
        setImagePreview(item.imageUrl || null);
        setCurrentImageUrl2(item.imageUrl2 || '');
        setImagePreview2(item.imageUrl2 || null);
      } catch {
        setError('Could not load the item for editing.');
      } finally {
        setInitialLoading(false);
      }
    };

    loadItem();
  }, [isEditing, itemId, user]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const updateLocationFromPlace = useCallback((place) => {
    const lat = place?.geometry?.location?.lat?.();
    const lng = place?.geometry?.location?.lng?.();
    if (typeof lat !== 'number' || typeof lng !== 'number') {
      setError('Please pick a suggested place or click on the map.');
      return;
    }
    const locationLabel = place?.name || place?.formatted_address || '';
    setFormData((prev) => ({ ...prev, latitude: lat, longitude: lng, locationText: locationLabel || prev.locationText }));
    setError('');
  }, []);

  const geocodeAddress = useCallback((address) => {
    if (!address?.trim() || !window.google?.maps) return;
    const geocoder = geocoderRef.current || new window.google.maps.Geocoder();
    geocoderRef.current = geocoder;
    geocoder.geocode({ address }, (results, status) => {
      if (status !== 'OK' || !results?.[0]) return;
      updateLocationFromPlace(results[0]);
    });
  }, [updateLocationFromPlace]);

  const reverseGeocodeLocation = useCallback((lat, lng) => {
    if (!window.google?.maps) return;
    const geocoder = geocoderRef.current || new window.google.maps.Geocoder();
    geocoderRef.current = geocoder;
    geocoder.geocode({ location: { lat, lng } }, (results, status) => {
      if (status !== 'OK' || !results?.[0]) return;
      updateLocationFromPlace(results[0]);
    });
  }, [updateLocationFromPlace]);

  const handlePlaceChanged = () => {
    const autocomplete = autocompleteRef.current;
    if (!autocomplete) return;
    updateLocationFromPlace(autocomplete.getPlace());
  };

  const handleImageChange = (slot) => (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) { setError('Please select a valid image file.'); return; }
    if (slot === 1) { setImageFile(file); setImagePreview(URL.createObjectURL(file)); }
    else            { setImageFile2(file); setImagePreview2(URL.createObjectURL(file)); }
    setError('');
  };

  const applyCapturedPhoto = (slot, dataUrl) => {
    if (slot === 1) { setImageFile(null); setCurrentImageUrl(dataUrl); setImagePreview(dataUrl); }
    else            { setImageFile2(null); setCurrentImageUrl2(dataUrl); setImagePreview2(dataUrl); }
  };

  const handleCameraCapture = (dataUrl) => {
    applyCapturedPhoto(cameraSlot, dataUrl);
    setCameraSlot(null);
  };

  // Native app: the system camera sheet. Web: the in-page camera modal.
  const openCamera = async (slot) => {
    if (!isNativeApp()) {
      setCameraSlot(slot);
      return;
    }
    try {
      applyCapturedPhoto(slot, await takePhotoDataUrl());
      setError('');
    } catch (err) {
      if (!err.cancelled) setError(err.message);
    }
  };

  const removeImage = (slot) => {
    if (slot === 1) {
      setImageFile(null); setImagePreview(null); setCurrentImageUrl('');
      document.getElementById('image-upload-1').value = '';
    } else {
      setImageFile2(null); setImagePreview2(null); setCurrentImageUrl2('');
      document.getElementById('image-upload-2').value = '';
    }
  };

  const handleMapClick = (e) => {
    if (!e.latLng) return;
    const lat = e.latLng.lat();
    const lng = e.latLng.lng();
    setFormData((prev) => ({ ...prev, latitude: lat, longitude: lng }));
    reverseGeocodeLocation(lat, lng);
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.title.trim()) { setError('Title is required.'); return; }
    if (!isEditing && !imageFile && !imagePreview) { setError('Please upload at least one image.'); return; }
    if (isEditing && !imageFile && !currentImageUrl) { setError('Please upload at least one image.'); return; }

    setLoading(true);

    try {
      const uploadedImageUrl  = imageFile  ? await readFileAsDataUrl(imageFile)  : currentImageUrl;
      const uploadedImageUrl2 = imageFile2 ? await readFileAsDataUrl(imageFile2) : (currentImageUrl2 || null);

      const formattedEventTime = formData.eventTime
        ? new Date(formData.eventTime).toISOString()
        : new Date().toISOString();

      const payload = {
        itemId: isEditing ? Number(itemId) : undefined,
        title: formData.title,
        description: formData.description,
        category: formData.category || 'General',
        locationText: formData.locationText,
        detailLocation: formData.detailLocation || null,
        eventTime: formattedEventTime,
        latitude: Number(formData.latitude),
        longitude: Number(formData.longitude),
        imageUrl: uploadedImageUrl,
        imageUrl2: uploadedImageUrl2
      };

      const response = await authFetch(
        isEditing ? `${API_BASE_URL}/items/${itemId}` : `${API_BASE_URL}/items`,
        {
          method: isEditing ? 'PATCH' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify(payload)
        }
      );

      if (response.ok) {
        navigate(isEditing ? `/items/${itemId}` : '/');
      } else {
        let message = isEditing ? 'Failed to update item.' : 'Failed to create post.';
        try {
          const errorData = await response.json();
          message = errorData.message || message;
        } catch {
          const text = await response.text();
          if (text) message = text;
        }
        setError(message);
      }
    } catch (err) {
      console.error('Submission error:', err);
      setError('Could not connect to the server.');
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) return <div className="create-item-container">Loading item...</div>;

  const mapCenter = {
    lat: Number(formData.latitude) || DEFAULT_CENTER.lat,
    lng: Number(formData.longitude) || DEFAULT_CENTER.lng
  };

  return (
    <div className="create-item-container">
      <div className="create-item-card">

        <header className="create-header">
          <button className="back-btn" onClick={() => navigate('/')}>&larr; Back</button>
          <h2>{isEditing ? 'Edit Item' : 'Post Lost Items'}</h2>
        </header>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit} className="create-form">

          {/* ── Two image slots ── */}
          <div className="form-group">
            <label className="input-label">Item Photos * <span className="label-hint">(up to 2 — upload or use camera)</span></label>
            <div className="image-slots-row">
              <ImageSlot
                label="Photo 1"
                preview={imagePreview}
                fileInputId="image-upload-1"
                onChange={handleImageChange(1)}
                onCamera={() => openCamera(1)}
                onRemove={() => removeImage(1)}
              />
              <ImageSlot
                label="Photo 2 (optional)"
                preview={imagePreview2}
                fileInputId="image-upload-2"
                onChange={handleImageChange(2)}
                onCamera={() => openCamera(2)}
                onRemove={() => removeImage(2)}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="input-label">Title *</label>
            <input type="text" name="title" value={formData.title} onChange={handleChange} placeholder="e.g., Blue Yeti Water Bottle" required />
          </div>

          <div className="form-row">
            <div className="form-group half">
              <label className="input-label">Category</label>
              <select name="category" value={formData.category} onChange={handleChange}>
                <option value="">Select Category...</option>
                <option value="Electronics">Electronics</option>
                <option value="IDs">IDs &amp; Wallets</option>
                <option value="Bags">Bags &amp; Backpacks</option>
                <option value="Clothing">Clothing</option>
                <option value="Keys">Keys</option>
                <option value="Bottles">Water Bottle</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="form-group half">
              <label className="input-label">Location</label>
              {hasGoogleMaps ? (
                <Autocomplete onLoad={(ac) => { autocompleteRef.current = ac; }} onPlaceChanged={handlePlaceChanged}>
                  <input type="text" name="locationText" value={formData.locationText} onChange={handleChange} onBlur={(e) => geocodeAddress(e.target.value)} placeholder="e.g., CULC" />
                </Autocomplete>
              ) : (
                <input type="text" name="locationText" value={formData.locationText} onChange={handleChange} placeholder="e.g., CULC" />
              )}
            </div>
            <div className="form-group half">
              <label className="input-label">Detailed Location</label>
              <input type="text" name="detailLocation" value={formData.detailLocation} onChange={handleChange} placeholder="e.g., 3rd floor" />
            </div>
          </div>

          <div className="form-group">
            <label className="input-label">Date &amp; Time Found</label>
            <input type="datetime-local" name="eventTime" value={formData.eventTime} onChange={handleChange} />
          </div>

          <div className="form-group location-group">
            <label className="input-label">Item Location</label>
            <p className="location-help">Click on the map to set the item's location.</p>
            {googleMapsApiKey ? (
              mapLoadError ? (
                <p className="map-message">Failed to load Google Map.</p>
              ) : !hasGoogleMaps ? (
                <p className="map-message">Loading map...</p>
              ) : (
                <GoogleMap mapContainerClassName="create-location-map" center={mapCenter} zoom={15} onClick={handleMapClick} options={{ streetViewControl: false, mapTypeControl: false }}>
                  <MarkerF position={mapCenter} />
                </GoogleMap>
              )
            ) : (
              <p className="map-message">Add VITE_GOOGLE_MAPS_API_KEY to enable map-based location picking.</p>
            )}
            <div className="coords-row">
              <span>Latitude: {Number(formData.latitude).toFixed(6)}</span>
              <span>Longitude: {Number(formData.longitude).toFixed(6)}</span>
            </div>
          </div>

          <div className="form-group">
            <label className="input-label">Description</label>
            <textarea name="description" value={formData.description} onChange={handleChange} placeholder="Provide any identifying details..." rows="4" />
          </div>

          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? (isEditing ? 'Updating...' : 'Posting...') : (isEditing ? 'Update Item' : 'Post Item')}
          </button>
        </form>
      </div>

      {cameraSlot && (
        <CameraModal
          onCapture={handleCameraCapture}
          onClose={() => setCameraSlot(null)}
        />
      )}
    </div>
  );
}

export default CreateItem;
