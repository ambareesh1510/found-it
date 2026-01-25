import { authFetch } from '../core/http.js';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../core/config.js';
import './CreatePostForm.css';

function CreatePostForm() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
    imageUrl: '',
    latitude: '',
    longitude: '',
    locationText: '',
    detailLocation: '',
    eventTime: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSubmitting(true);

    try {
      const payload = {
        title: formData.title,
        description: formData.description || null,
        category: formData.category || null,
        imageUrl: formData.imageUrl,
        latitude: Number(formData.latitude),
        longitude: Number(formData.longitude),
        locationText: formData.locationText || null,
        detailLocation: formData.detailLocation || null,
        eventTime: formData.eventTime ? new Date(formData.eventTime).toISOString() : null
      };

      const response = await authFetch(`${API_BASE_URL}/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        setErrorMsg('Failed to create post. Please check your inputs and try again.');
        return;
      }

      navigate('/');
    } catch {
      setErrorMsg('Could not connect to server.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="create-post-page">
      <div className="create-post-card">
        <h1 className="app-title">GT Lost and Found</h1>
        <h2 className="create-post-heading">Create Item Post</h2>

        <form className="create-post-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input id="title" name="title" type="text" value={formData.title} onChange={handleChange} required />
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea id="description" name="description" value={formData.description} onChange={handleChange} rows="3" />
          </div>

          <div className="form-group">
            <label htmlFor="category">Category</label>
            <input id="category" name="category" type="text" value={formData.category} onChange={handleChange} />
          </div>

          <div className="form-group">
            <label htmlFor="imageUrl">Image URL</label>
            <input id="imageUrl" name="imageUrl" type="url" value={formData.imageUrl} onChange={handleChange} required />
          </div>

          <div className="form-group">
            <label htmlFor="latitude">Latitude</label>
            <input id="latitude" name="latitude" type="number" step="any" value={formData.latitude} onChange={handleChange} required />
          </div>

          <div className="form-group">
            <label htmlFor="longitude">Longitude</label>
            <input id="longitude" name="longitude" type="number" step="any" value={formData.longitude} onChange={handleChange} required />
          </div>

          <div className="form-group">
            <label htmlFor="locationText">Location</label>
            <input id="locationText" name="locationText" type="text" value={formData.locationText} onChange={handleChange} placeholder="e.g., CULC" />
          </div>

          <div className="form-group">
            <label htmlFor="detailLocation">Detailed Location</label>
            <input id="detailLocation" name="detailLocation" type="text" value={formData.detailLocation} onChange={handleChange} placeholder="e.g., 3rd floor" />
          </div>

          <div className="form-group">
            <label htmlFor="eventTime">Event Time</label>
            <input id="eventTime" name="eventTime" type="datetime-local" value={formData.eventTime} onChange={handleChange} />
          </div>

          {errorMsg && <p className="create-post-error">{errorMsg}</p>}

          <div className="create-post-actions">
            <button type="button" className="btn-cancel" onClick={() => navigate('/')}>Cancel</button>
            <button type="submit" className="btn-submit" disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Post'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreatePostForm;
