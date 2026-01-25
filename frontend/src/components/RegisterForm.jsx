import { authFetch } from '../core/http.js';
import { useState } from 'react';
import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from '../api';
import { isPasswordValid } from '../passwordPolicy';
import PasswordRules from './PasswordRules';
import './auth.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function RegisterForm({ onRegisterSuccess, onSwitchToLogin }) {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState(false);
  const [resendMessage, setResendMessage] = useState('');
  const [resending, setResending] = useState(false);

  const { username, email, password, confirmPassword } = formData;
  const emailValid = EMAIL_PATTERN.test(email.trim());
  const passwordOk = isPasswordValid(password);
  const passwordsMatch = confirmPassword !== '' && password === confirmPassword;
  const canSubmit =
    username.trim() !== '' && emailValid && passwordOk && passwordsMatch && !submitting;

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
      const response = await authFetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: username.trim().toLowerCase(),
          email: email.trim().toLowerCase(),
          password,
        }),
      });

      if (response.ok) {
        setCreated(true);
        return;
      }

      const body = await response.json().catch(() => null);
      if (response.status === 409) {
        setError('An account with this email or username already exists.');
      } else if (response.status === 400) {
        setError(body?.message || 'Please check your details and try again.');
      } else {
        setError('Registration failed. Please try again.');
      }
    } catch (err) {
      console.error('Register error:', err);
      setError(NETWORK_ERROR_MESSAGE);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    setResendMessage('');
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim().toLowerCase() }),
      });
      setResendMessage(
        response.ok
          ? 'If that account is waiting for verification, a new link has been sent.'
          : 'We could not resend the email. Please try again.'
      );
    } catch (err) {
      console.error('Resend verification error:', err);
      setResendMessage(NETWORK_ERROR_MESSAGE);
    } finally {
      setResending(false);
    }
  };

  if (created) {
    return (
      <div className="auth-page">
        <main className="auth-card">
          <h1 className="auth-brand">Found it</h1>
          <h2 className="auth-heading">Check your email</h2>
          <p className="auth-subtext">
            We sent a verification link to <strong>{email.trim()}</strong>. Open it to activate your
            account, then log in.
          </p>

          {resendMessage && (
            <div className="auth-alert auth-alert--success" role="status">
              {resendMessage}
            </div>
          )}

          <button
            type="button"
            className="auth-btn auth-btn--secondary"
            onClick={handleResend}
            disabled={resending}
          >
            {resending ? 'Sending...' : 'Resend verification email'}
          </button>
          <button type="button" className="auth-btn auth-btn--primary auth-btn--spaced" onClick={onRegisterSuccess}>
            Go to login
          </button>
        </main>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <main className="auth-card">
        <h1 className="auth-brand">Found it</h1>
        <h2 className="auth-heading">Register</h2>
        <p className="auth-subtext">Create an account to report and track items.</p>

        {error && (
          <div className="auth-alert auth-alert--error" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="register-username">Name</label>
            <input
              className="auth-input"
              type="text"
              id="register-username"
              name="username"
              autoComplete="name"
              value={username}
              onChange={handleChange}
              required
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="register-email">Email</label>
            <input
              className="auth-input"
              type="email"
              id="register-email"
              name="email"
              autoComplete="email"
              inputMode="email"
              autoCapitalize="none"
              spellCheck={false}
              value={email}
              onChange={handleChange}
              aria-invalid={email !== '' && !emailValid ? 'true' : undefined}
              aria-describedby="register-email-hint"
              required
            />
            {email !== '' && !emailValid && (
              <p id="register-email-hint" className="auth-hint auth-hint--bad">
                Enter a valid email address.
              </p>
            )}
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="register-password">Password</label>
            <input
              className="auth-input"
              type={showPassword ? 'text' : 'password'}
              id="register-password"
              name="password"
              autoComplete="new-password"
              value={password}
              onChange={handleChange}
              aria-invalid={password !== '' && !passwordOk ? 'true' : undefined}
              aria-describedby="register-password-rules"
              required
            />
            <PasswordRules password={password} id="register-password-rules" />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="register-confirm-password">Confirm password</label>
            <input
              className="auth-input"
              type={showPassword ? 'text' : 'password'}
              id="register-confirm-password"
              name="confirmPassword"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={handleChange}
              aria-invalid={confirmPassword !== '' && !passwordsMatch ? 'true' : undefined}
              aria-describedby="register-confirm-hint"
              required
            />
            {confirmPassword !== '' && (
              <p
                id="register-confirm-hint"
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
            {submitting ? 'Creating account...' : 'Sign Up'}
          </button>
        </form>

        <div className="auth-divider">or</div>

        <a className="auth-btn-google" href={`${API_BASE_URL}/oauth2/authorization/google`}>
          <img
            src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg"
            alt=""
            className="google-icon"
          />
          Sign up with Google
        </a>

        <p className="auth-footer">
          Already have an account?
          <button type="button" className="auth-link" onClick={onSwitchToLogin}>
            Log In
          </button>
        </p>
      </main>
    </div>
  );
}

export default RegisterForm;
