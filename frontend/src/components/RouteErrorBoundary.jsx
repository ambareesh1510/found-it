import { Component } from 'react';

class RouteErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error) {
    console.error('Route render failed.', error);
  }

  render() {
    const { children, isAuthenticated } = this.props;

    if (this.state.hasError) {
      return (
        <div style={{ textAlign: 'center', marginTop: '50px', padding: '0 16px' }}>
          <h2>Something went wrong while loading this page.</h2>
          <p>Please try again. If it keeps happening, return to the home screen.</p>
          <button
            type="button"
            onClick={() => window.location.assign(isAuthenticated ? '/' : '/login')}
          >
            Return to {isAuthenticated ? 'home' : 'login'}
          </button>
        </div>
      );
    }

    return children;
  }
}

export default RouteErrorBoundary;
