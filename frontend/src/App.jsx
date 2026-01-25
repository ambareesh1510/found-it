import { authFetch } from './core/http.js';
import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { tabForPath } from './core/navigation.js';
import { deleteAccount } from './core/services.js';
import { restoreSession, startSession, endSession } from './platform/session.js';
import { API_BASE_URL } from './core/config.js';
import AppTabBar from './ui/AppTabBar';
import './App.css';
import LoginForm from './components/LoginForm';
import RegisterForm from './components/RegisterForm';
import ForgotPasswordForm from './components/ForgotPasswordForm';
import ResetPasswordForm from './components/ResetPasswordForm';
import VerifyEmail from './components/VerifyEmail';
import Profile from './components/Profile';
import List from './components/List';
import CreateItem from './components/CreateItem';
import ItemDetail from './components/ItemDetail';
import Chat from './components/Chat';
import RouteErrorBoundary from './components/RouteErrorBoundary';
import HomePage from './features/home/HomePage';
import LostCategoryPage from './features/lost/LostCategoryPage';
import LostWhereWhenPage from './features/lost/LostWhereWhenPage';
import LostResultsPage from './features/lost/LostResultsPage';
import ReportLostPage from './features/lost/ReportLostPage';
import FoundListPage from './features/found/FoundListPage';
import ReportFoundPage from './features/found/ReportFoundPage';
import './ui/flows.css';
import { startTimelineCapture } from './platform/locationTimeline';

async function fetchCurrentUser() {
  const response = await authFetch(`${API_BASE_URL}/users/me`, {
    method: 'GET',
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error('Failed to load current user.');
  }

  return response.json();
}

function AppRoutes({ onLoginSuccess }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const checkSession = async () => {
      try {
        await restoreSession();
        const userData = await fetchCurrentUser();
        setUser(userData);
      } catch (error) {
        console.error("Session check failed.", error);
      } finally {
        setLoading(false);
      }
    };

    checkSession();
  }, []);

  useEffect(() => {
    if (!user?.id) return undefined;
    return startTimelineCapture(user.id, (error) => console.warn('Timeline capture failed:', error.message));
  }, [user?.id]);

  const handleLogout = async () => {
    try {
      await authFetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      });
    } catch (error) {
      console.error("Error logging out from server", error);
    } finally {
      await endSession();
      setUser(null);
      navigate('/login');
    }
  };

  // Throws an ApiError when deletion fails, so the Profile screen can show the message.
  const handleDeleteAccount = async () => {
    await deleteAccount();
    await endSession();
    setUser(null);
    navigate('/login');
  };

  if (loading) {
    return <div style={{ textAlign: 'center', marginTop: '50px' }}>Loading...</div>;
  }

  // The tab bar belongs to the signed-in app's sections only; item detail, create, and auth screens have no tabs.
  const activeTab = tabForPath(location.pathname);
  const showTabs = Boolean(user) && activeTab !== null;

  return (
    <div className={`app-container${showTabs ? ' has-tab-bar' : ''}`}>
      <RouteErrorBoundary isAuthenticated={Boolean(user)}>
        <Routes>
          <Route
            path="/"
            element={user ? <HomePage user={user} /> : <Navigate to="/login" />}
          />

          {/* Map feed (the earlier home screen), kept reachable while the new flows settle. */}
          <Route
            path="/map"
            element={user ? <List user={user} /> : <Navigate to="/login" />}
          />

          {/* I Lost an Item */}
          <Route path="/lost" element={user ? <LostCategoryPage /> : <Navigate to="/login" />} />
          <Route path="/lost/where" element={user ? <LostWhereWhenPage user={user} /> : <Navigate to="/login" />} />
          <Route path="/lost/results" element={user ? <LostResultsPage /> : <Navigate to="/login" />} />
          <Route path="/lost/report" element={user ? <ReportLostPage /> : <Navigate to="/login" />} />

          {/* I Found an Item */}
          <Route path="/found" element={user ? <FoundListPage user={user} /> : <Navigate to="/login" />} />
          <Route path="/found/report" element={user ? <ReportFoundPage /> : <Navigate to="/login" />} />

          <Route 
            path="/profile" 
            element={user ? <Profile user={user} onLogout={handleLogout} onDeleteAccount={handleDeleteAccount} /> : <Navigate to="/login" />} 
          />

          <Route
            path="/chat"
            element={user ? <Chat user={user} /> : <Navigate to="/login" />}
          />

          <Route 
            path="/create" 
            element={user ? <CreateItem user={user} /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/items/:itemId/edit" 
            element={user ? <CreateItem user={user} /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/items/:itemId" 
            element={user ? <ItemDetail user={user} /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/login" 
            element={!user ? (
              <LoginForm 
                onLoginSuccess={(data) => onLoginSuccess(data, setUser, navigate)}
                onSwitchToRegister={() => navigate('/register')}
                onForgotPassword={() => navigate('/forgot-password')}
              />
            ) : <Navigate to="/" />} 
          />

          <Route 
            path="/register" 
            element={!user ? (
              <RegisterForm 
                onRegisterSuccess={() => navigate('/login')}
                onSwitchToLogin={() => navigate('/login')}
              />
            ) : <Navigate to="/" />} 
          />

          <Route
            path="/forgot-password"
            element={!user ? (
              <ForgotPasswordForm
                onBackToLogin={() => navigate('/login')}
                onSwitchToRegister={() => navigate('/register')}
              />
            ) : <Navigate to="/" />}
          />
          {/* Public: opened from the emailed links; works whether or not the user is logged in. */}
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/reset-password/:token" element={<ResetPasswordForm />} />

          <Route path="*" element={<Navigate to={user ? '/' : '/login'} replace />} />
        </Routes>
      </RouteErrorBoundary>
      {showTabs && <AppTabBar activeTabId={activeTab.id} />}
    </div>
  );
}

function App() {
  const handleLoginSuccess = async (data, setUser, navigate) => {
    try {
      // Native: keeps the bearer token in the Keychain. Web: the cookie is already set, so this only caches the token in memory.
      if (data?.token) {
        await startSession(data.token);
      }
      const userData = await fetchCurrentUser();
      setUser(userData);
      navigate('/');
    } catch (error) {
      console.error('Failed to load user after login.', error);
      // Returned so LoginForm can show it inline instead of a popup.
      return 'Login succeeded, but your account details could not be loaded. Please try again.';
    }
  };

  return (
    <Router>
      <AppRoutes onLoginSuccess={handleLoginSuccess} />
    </Router>
  );
}

export default App;
