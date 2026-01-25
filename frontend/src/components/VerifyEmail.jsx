import { authFetch } from '../core/http.js';
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from '../api';
import './auth.css';

const MISSING_TOKEN_MESSAGE = 'This verification link is missing its token. Open the link from your email again.';

// Reached from the link in the verification email: /verify-email?token=...
function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const [status, setStatus] = useState(token ? 'loading' : 'error'); // loading | success | error
  const [message, setMessage] = useState(token ? '' : MISSING_TOKEN_MESSAGE);
  // The token is single-use. StrictMode runs effects twice in development, so guard the request.
  const requestedRef = useRef(false);

  useEffect(() => {
    if (requestedRef.current || !token) return;
    requestedRef.current = true;

    const verify = async () => {
      try {
        const response = await authFetch(`${API_BASE_URL}/auth/verify`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token }),
        });

        if (response.ok) {
          setStatus('success');
          return;
        }

        const body = await response.json().catch(() => null);
        setStatus('error');
        setMessage(body?.message || 'We could not verify your email. Please try again.');
      } catch (err) {
        console.error('Verify email error:', err);
        setStatus('error');
        setMessage(NETWORK_ERROR_MESSAGE);
      }
    };

    verify();
  }, [token]);

  return (
    <div className="auth-page">
      <main className="auth-card">
        <h1 className="auth-brand">Found it</h1>
        <h2 className="auth-heading">Verify your email</h2>

        {status === 'loading' && (
          <p className="auth-subtext" role="status">Verifying your email...</p>
        )}

        {status === 'success' && (
          <>
            <div className="auth-alert auth-alert--success" role="status">
              Your email is verified. You can now log in.
            </div>
            <Link className="auth-btn auth-btn--primary auth-btn--link" to="/login">
              Go to login
            </Link>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="auth-alert auth-alert--error" role="alert">
              {message}
            </div>
            <p className="auth-subtext">
              Log in to request a new verification link, or register again if you have not
              created an account yet.
            </p>
            <Link className="auth-btn auth-btn--primary auth-btn--link" to="/login">
              Go to login
            </Link>
          </>
        )}
      </main>
    </div>
  );
}

export default VerifyEmail;
