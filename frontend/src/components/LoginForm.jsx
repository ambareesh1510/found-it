import { authFetch } from '../core/http.js';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from '../core/config.js';
import { ApiError } from '../core/http.js';
import { signInWithGoogle, signInWithApple } from '../core/services.js';
import { getNativeSignIn } from '../platform/socialSignIn.js';
import { isNativeApp } from '../platform/session.js';
import './auth.css';

const INCORRECT_CREDENTIALS_MESSAGE = 'Incorrect email or password.';
const GOOGLE_CANCELLED_MESSAGE = 'Google sign-in was cancelled or failed. Try again, or log in with your email and password.';
const NATIVE_SIGN_IN_FAILED_MESSAGE = 'Sign-in was cancelled or failed. Try again, or log in with your email and password.';

function LoginForm({ onLoginSuccess, onSwitchToRegister, onForgotPassword }) {
  const [searchParams] = useSearchParams();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  // The backend redirects here with ?oauthError=google when the Google consent screen is cancelled.
  const [error, setError] = useState(
    searchParams.get('oauthError') ? GOOGLE_CANCELLED_MESSAGE : ''
  );
  // True when the backend says the account exists but has not verified its email (403).
  const [unverified, setUnverified] = useState(false);
  const [resendMessage, setResendMessage] = useState('');
  const [resending, setResending] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const canSubmit = formData.email.trim() !== '' && formData.password !== '' && !submitting;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError('');
    if (unverified) setUnverified(false);
    if (resendMessage) setResendMessage('');
  };

  const handleResend = async () => {
    setResending(true);
    setResendMessage('');
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formData.email.trim().toLowerCase() }),
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

  // Native app only: the system Google or Apple sheet returns an ID token that the server verifies.
  const handleNativeSignIn = async (provider) => {
    setSubmitting(true);
    setError('');
    setUnverified(false);
    setResendMessage('');
    try {
      const { idToken, authorizationCode } = await getNativeSignIn(provider);
      const data = provider === 'apple'
        ? await signInWithApple(idToken, authorizationCode)
        : await signInWithGoogle(idToken);
      const problem = await onLoginSuccess(data);
      if (problem) setError(problem);
    } catch (err) {
      console.error(`${provider} sign-in error:`, err);
      setError(err instanceof ApiError && err.status !== 0 ? err.message : NATIVE_SIGN_IN_FAILED_MESSAGE);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setError('');
    setUnverified(false);
    setResendMessage('');
    try {
      const response = await authFetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          usernameOrEmail: formData.email.trim().toLowerCase(),
          password: formData.password,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        const problem = await onLoginSuccess(data);
        if (problem) setError(problem);
        return;
      }

      const body = await response.json().catch(() => null);
      if (response.status === 400) {
        setError(body?.message || 'Please check your details and try again.');
      } else if (response.status === 403) {
        // Correct password, but email not verified yet.
        setUnverified(true);
        setError(body?.message || 'Account not verified. Please verify your account.');
      } else if (response.status === 401) {
        setError(INCORRECT_CREDENTIALS_MESSAGE);
      } else {
        setError('Login failed. Please try again.');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError(NETWORK_ERROR_MESSAGE);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <main className="auth-card">
        <h1 className="auth-brand">Found it</h1>
        <h2 className="auth-heading">Login</h2>
        <p className="auth-subtext">Sign in to report and find lost items.</p>

        {error && (
          <div className="auth-alert auth-alert--error" role="alert">
            {error}
            {unverified && (
              <div className="auth-alert__action">
                <button
                  type="button"
                  className="auth-link"
                  onClick={handleResend}
                  disabled={resending}
                >
                  {resending ? 'Sending...' : 'Resend verification email'}
                </button>
              </div>
            )}
          </div>
        )}

        {resendMessage && (
          <div className="auth-alert auth-alert--success" role="status">
            {resendMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="login-email">Email</label>
            <input
              className="auth-input"
              type="email"
              id="login-email"
              name="email"
              autoComplete="username"
              inputMode="email"
              autoCapitalize="none"
              spellCheck={false}
              value={formData.email}
              onChange={handleChange}
              aria-invalid={error ? 'true' : undefined}
              required
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="login-password">Password</label>
            <input
              className="auth-input"
              type={showPassword ? 'text' : 'password'}
              id="login-password"
              name="password"
              autoComplete="current-password"
              value={formData.password}
              onChange={handleChange}
              aria-invalid={error ? 'true' : undefined}
              required
            />
          </div>

          <label className="auth-checkbox">
            <input
              type="checkbox"
              checked={showPassword}
              onChange={(e) => setShowPassword(e.target.checked)}
            />
            Show password
          </label>

          <div className="auth-forgot">
            <button type="button" className="auth-link" onClick={onForgotPassword}>
              Forgot password?
            </button>
          </div>

          <button type="submit" className="auth-btn auth-btn--primary" disabled={!canSubmit}>
            {submitting ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-divider">or</div>

        {isNativeApp() ? (
          <>
            <button
              type="button"
              className="auth-btn auth-btn--apple"
              onClick={() => handleNativeSignIn('apple')}
              disabled={submitting}
            >
              Sign in with Apple
            </button>
            <button
              type="button"
              className="auth-btn-google"
              onClick={() => handleNativeSignIn('google')}
              disabled={submitting}
            >
              Sign in with Google
            </button>
          </>
        ) : (
          <a className="auth-btn-google" href={`${API_BASE_URL}/oauth2/authorization/google`}>
            <img
              src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg"
              alt=""
              className="google-icon"
            />
            Sign in with Google
          </a>
        )}

        <p className="auth-footer">
          Don&apos;t have an account yet?
          <button type="button" className="auth-link" onClick={onSwitchToRegister}>
            Register
          </button>
        </p>
      </main>
    </div>
  );
}

export default LoginForm;
