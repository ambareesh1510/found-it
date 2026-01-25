import { authFetch } from '../core/http.js';
import { useState } from 'react';
import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from '../api';
import './auth.css';

function ForgotPasswordForm({ onBackToLogin, onSwitchToRegister }) {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const canSubmit = email.trim() !== '' && !submitting;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setError('');
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim().toLowerCase() }),
      });

      if (response.ok) {
        setSent(true);
      } else {
        setError('We could not send the reset link. Please try again.');
      }
    } catch (err) {
      console.error('Forgot password error:', err);
      setError(NETWORK_ERROR_MESSAGE);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <main className="auth-card">
        <h1 className="auth-brand">Found it</h1>
        <h2 className="auth-heading">Forgot password?</h2>

        {sent ? (
          <>
            <div className="auth-alert auth-alert--success" role="status">
              If an account exists for that email, a reset link is on its way.
            </div>
            <button type="button" className="auth-btn auth-btn--primary" onClick={onBackToLogin}>
              Back to login
            </button>
          </>
        ) : (
          <>
            <p className="auth-subtext">
              Enter your email address and we will send you a link to reset your password.
            </p>

            {error && (
              <div className="auth-alert auth-alert--error" role="alert">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              <div className="auth-field">
                <label className="auth-label" htmlFor="reset-email">Email</label>
                <input
                  className="auth-input"
                  type="email"
                  id="reset-email"
                  name="email"
                  autoComplete="email"
                  inputMode="email"
                  autoCapitalize="none"
                  spellCheck={false}
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (error) setError('');
                  }}
                  required
                />
              </div>

              <button type="submit" className="auth-btn auth-btn--primary" disabled={!canSubmit}>
                {submitting ? 'Sending...' : 'Send Reset Link'}
              </button>
            </form>

            <p className="auth-footer">
              Don&apos;t have an account yet?
              <button type="button" className="auth-link" onClick={onSwitchToRegister}>
                Register
              </button>
            </p>
          </>
        )}
      </main>
    </div>
  );
}

export default ForgotPasswordForm;
