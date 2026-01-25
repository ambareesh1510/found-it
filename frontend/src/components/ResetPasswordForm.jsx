import { authFetch } from '../core/http.js';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from '../api';
import { isPasswordValid } from '../passwordPolicy';
import PasswordRules from './PasswordRules';
import './auth.css';

// Reached from the emailed link: /reset-password/:token
function ResetPasswordForm() {
  const { token = '' } = useParams();
  const [formData, setFormData] = useState({ password: '', confirmPassword: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const { password, confirmPassword } = formData;
  const passwordOk = isPasswordValid(password);
  const passwordsMatch = confirmPassword !== '' && password === confirmPassword;
  const canSubmit = passwordOk && passwordsMatch && !submitting;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setError('');
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, password }),
      });

      if (response.ok) {
        setDone(true);
        return;
      }

      const body = await response.json().catch(() => null);
      if (response.status === 400) {
        setError(body?.message || 'Please check your new password and try again.');
      } else {
        setError('We could not reset your password. Please try again.');
      }
    } catch (err) {
      console.error('Reset password error:', err);
      setError(NETWORK_ERROR_MESSAGE);
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <div className="auth-page">
        <main className="auth-card">
          <h1 className="auth-brand">Found it</h1>
          <h2 className="auth-heading">Password updated</h2>
          <div className="auth-alert auth-alert--success" role="status">
            Your password has been reset. You can now log in with your new password.
          </div>
          <Link className="auth-btn auth-btn--primary auth-btn--link" to="/login">
            Go to login
          </Link>
        </main>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <main className="auth-card">
        <h1 className="auth-brand">Found it</h1>
        <h2 className="auth-heading">Set a new password</h2>
        <p className="auth-subtext">Choose a new password for your account.</p>

        {error && (
          <div className="auth-alert auth-alert--error" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="reset-password">New password</label>
            <input
              className="auth-input"
              type={showPassword ? 'text' : 'password'}
              id="reset-password"
              name="password"
              autoComplete="new-password"
              value={password}
              onChange={handleChange}
              aria-invalid={password !== '' && !passwordOk ? 'true' : undefined}
              aria-describedby="reset-password-rules"
              required
            />
            <PasswordRules password={password} id="reset-password-rules" />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="reset-confirm-password">Confirm new password</label>
            <input
              className="auth-input"
              type={showPassword ? 'text' : 'password'}
              id="reset-confirm-password"
              name="confirmPassword"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={handleChange}
              aria-invalid={confirmPassword !== '' && !passwordsMatch ? 'true' : undefined}
              aria-describedby="reset-confirm-hint"
              required
            />
            {confirmPassword !== '' && (
              <p
                id="reset-confirm-hint"
                className={`auth-hint ${passwordsMatch ? 'auth-hint--ok' : 'auth-hint--bad'}`}
              >
                {passwordsMatch ? 'Passwords match.' : 'Passwords do not match.'}
              </p>
            )}
          </div>

          <label className="auth-checkbox">
            <input
              type="checkbox"
              checked={showPassword}
              onChange={(e) => setShowPassword(e.target.checked)}
            />
            Show passwords
          </label>

          <button type="submit" className="auth-btn auth-btn--primary" disabled={!canSubmit}>
            {submitting ? 'Saving...' : 'Reset Password'}
          </button>
        </form>

        <p className="auth-footer">
          <Link className="auth-link" to="/login">Back to login</Link>
        </p>
      </main>
    </div>
  );
}

export default ResetPasswordForm;
