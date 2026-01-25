import { useState } from 'react';
import Field from '../../ui/Field';
import LocationField from './LocationField';
import { LOST_CATEGORIES } from '../../core/categories';
import { EMPTY_LOCATION } from '../../core/location';
import { toLocalDateInputValue } from '../../core/eventTime';
import { validatePhotoFile } from '../../core/photos';
import { POST_TYPES, buildCreateItemRequest, isPhotoSource, validateReport } from '../../core/reports';
import { createReport } from '../../core/services';
import { readFileAsDataUrl } from '../../platform/photos';

const COPY = {
  [POST_TYPES.LOST]: {
    locationLabel: 'Last seen near',
    dateLabel: 'When did you lose it?',
    submitLabel: 'Post lost report',
    buttonClass: 'flow-btn--lost',
  },
  [POST_TYPES.FOUND]: {
    locationLabel: 'Where did you find it?',
    dateLabel: 'When did you find it?',
    submitLabel: 'Post found report',
    buttonClass: 'flow-btn--found',
  },
};

const ALL_FIELDS = ['title', 'category', 'imageUrl', 'reward', 'location', 'date', 'time'];

function initialValues(prefill) {
  return {
    title: '',
    category: prefill?.category ?? '',
    description: '',
    imageUrl: '',
    rewardDollars: '',
    location: prefill?.location ?? EMPTY_LOCATION,
    date: prefill?.date ?? '',
    time: prefill?.time ?? '',
  };
}

/**
 * Shared "report an item" form for both flows. Owns no navigation: it calls `onSubmitted` on success
 * and the page decides where to go. The only type-specific parts are `postType` and the copy table above.
 */
export default function ReportForm({ postType, prefill = null, banner = null, onSubmitted }) {
  const copy = COPY[postType];
  const isLost = postType === POST_TYPES.LOST;
  const [values, setValues] = useState(() => initialValues(prefill));
  const [touched, setTouched] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [photoError, setPhotoError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const validation = validateReport({ postType, ...values });
  const errorFor = (field) => (touched[field] ? validation.errors[field] : undefined);
  const touch = (field) => setTouched((prev) => ({ ...prev, [field]: true }));
  const setField = (field) => (event) => setValues((prev) => ({ ...prev, [field]: event.target.value }));

  const photoIsUpload = values.imageUrl.startsWith('data:');

  const handlePhotoFile = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = ''; // lets the same file be picked again after a removal
    setPhotoError('');
    const problem = validatePhotoFile(file);
    if (problem) {
      setPhotoError(problem);
      return;
    }
    try {
      const dataUrl = await readFileAsDataUrl(file);
      touch('imageUrl');
      setValues((prev) => ({ ...prev, imageUrl: dataUrl }));
    } catch (err) {
      setPhotoError(err.message);
    }
  };

  const handleRemovePhoto = () => {
    setPhotoError('');
    setValues((prev) => ({ ...prev, imageUrl: '' }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setTouched(Object.fromEntries(ALL_FIELDS.map((field) => [field, true])));
    if (!validation.valid || submitting) return;

    setSubmitting(true);
    setSubmitError('');
    try {
      await createReport(buildCreateItemRequest({ postType, ...values }));
      onSubmitted();
    } catch (err) {
      setSubmitError(err.message || 'Could not post the report. Please try again.');
      setSubmitting(false);
    }
  };

  return (
    <form className="flow-card flow-form" onSubmit={handleSubmit} noValidate>
      {banner && <div className="flow-banner">{banner}</div>}

      <Field id="report-title" label="Item name" required error={errorFor('title')}>
        <input
          id="report-title"
          type="text"
          className={`flow-input${errorFor('title') ? ' flow-input--invalid' : ''}`}
          value={values.title}
          onChange={setField('title')}
          onBlur={() => touch('title')}
          placeholder="e.g. Black wallet, AirPods case"
          maxLength={255}
        />
      </Field>

      <Field id="report-category" label="Category" required error={errorFor('category')}>
        <select
          id="report-category"
          className="flow-select"
          value={values.category}
          onChange={setField('category')}
          onBlur={() => touch('category')}
        >
          <option value="">Choose a category</option>
          {LOST_CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>
      </Field>

      <Field
        id="report-photo-link"
        label="Photo (optional)"
        error={errorFor('imageUrl') || photoError}
        hint="Upload an image or paste a link. A clear photo helps people recognise the item."
      >
        {values.imageUrl && isPhotoSource(values.imageUrl) && (
          <img className="flow-photo-preview" src={values.imageUrl} alt="Selected photo preview" />
        )}
        {/* Upload and link side by side: a file button, then the link input filling the rest of the row. */}
        <div className="flow-photo-row">
          <label className="flow-btn flow-btn--secondary flow-btn--small flow-file-button">
            {photoIsUpload ? 'Replace photo' : 'Upload photo'}
            <input
              type="file"
              accept="image/*"
              className="flow-visually-hidden"
              onChange={handlePhotoFile}
            />
          </label>
          <input
            id="report-photo-link"
            type="url"
            inputMode="url"
            className={`flow-input${errorFor('imageUrl') ? ' flow-input--invalid' : ''}`}
            value={photoIsUpload ? '' : values.imageUrl}
            onChange={(event) => {
              setPhotoError('');
              setValues((prev) => ({ ...prev, imageUrl: event.target.value }));
            }}
            onBlur={() => touch('imageUrl')}
            placeholder={photoIsUpload ? 'Photo uploaded' : 'or paste a link: https://'}
          />
        </div>
        {values.imageUrl && (
          <div className="flow-photo-actions">
            <button type="button" className="flow-btn flow-btn--secondary flow-btn--small" onClick={handleRemovePhoto}>
              Remove photo
            </button>
          </div>
        )}
      </Field>

      <Field id="report-description" label="Description">
        <textarea
          id="report-description"
          className="flow-textarea"
          value={values.description}
          onChange={setField('description')}
          placeholder="Color, brand, distinguishing marks. Keep private details out."
        />
      </Field>

      {isLost && (
        <Field
          id="report-reward"
          label="Reward (optional)"
          error={errorFor('reward')}
          hint="Shown on your report so finders know there is a reward. Leave blank for none."
        >
          <div className="flow-input-prefix">
            <span aria-hidden="true">$</span>
            <input
              id="report-reward"
              type="text"
              inputMode="decimal"
              className={`flow-input${errorFor('reward') ? ' flow-input--invalid' : ''}`}
              value={values.rewardDollars}
              onChange={setField('rewardDollars')}
              onBlur={() => touch('reward')}
              placeholder="0.00"
            />
          </div>
        </Field>
      )}

      <LocationField
        id="report-location"
        label={copy.locationLabel}
        required
        value={values.location}
        error={errorFor('location')}
        onChange={(location) => {
          touch('location');
          setValues((prev) => ({ ...prev, location }));
        }}
      />

      <Field id="report-date" label={copy.dateLabel} required error={errorFor('date')}>
        <input
          id="report-date"
          type="date"
          className={`flow-input${errorFor('date') ? ' flow-input--invalid' : ''}`}
          value={values.date}
          max={toLocalDateInputValue()}
          onChange={setField('date')}
          onBlur={() => touch('date')}
        />
      </Field>

      <Field id="report-time" label="Time" required error={errorFor('time')}>
        <input
          id="report-time"
          type="time"
          className={`flow-input${errorFor('time') ? ' flow-input--invalid' : ''}`}
          value={values.time}
          onChange={setField('time')}
          onBlur={() => touch('time')}
        />
      </Field>

      {submitError && (
        <div className="flow-banner flow-banner--danger" role="alert">
          {submitError}
        </div>
      )}

      <button
        type="submit"
        className={`flow-btn ${copy.buttonClass}`}
        disabled={!validation.valid || submitting}
      >
        {submitting ? 'Posting…' : copy.submitLabel}
      </button>
    </form>
  );
}
