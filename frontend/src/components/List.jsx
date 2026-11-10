import { authFetch } from '../core/http.js';
import { getCurrentCoordinates } from '../platform/geolocation.js';
import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { GoogleMap, InfoWindowF, MarkerF, CircleF, useJsApiLoader } from '@react-google-maps/api';
import { LOST_CATEGORIES } from '../core/categories';
import { API_BASE_URL } from '../core/config.js';
import './List.css';

const CATEGORY_OPTIONS = LOST_CATEGORIES;

const STATUS_OPTIONS = ['OPEN', 'RESOLVED'];
const DEFAULT_CENTER = { lat: 33.7756, lng: -84.3963 };
const GOOGLE_MAP_LIBRARIES = ['places'];

function formatItemLocation(locationText, detailLocation) {
  const base = locationText?.trim() || 'Unknown location';
  const detail = detailLocation?.trim();
  return detail ? `${base} ${detail}` : base;
}

function markerSvgPathByCategory(category) {
  const normalized = (category || '').toLowerCase();

  if (normalized.includes('bottle')) {
    return '<path d="M20 12h8"/><path d="M21 12v4"/><path d="M27 12v4"/><rect x="18" y="16" width="12" height="20" rx="4"/>';
  }

  if (normalized.includes('key')) {
    return '<circle cx="19" cy="22" r="5"/><path d="M24 22h10"/><path d="M31 22v4"/><path d="M34 22v3"/>';
  }

  if (normalized.includes('id') || normalized.includes('wallet')) {
    return '<rect x="14" y="16" width="20" height="16" rx="3"/><path d="M14 22h20"/><circle cx="28" cy="24" r="1.6"/>';
  }

  if (normalized.includes('bag')) {
    return '<path d="M18 20v-2a6 6 0 0 1 12 0v2"/><rect x="14" y="20" width="20" height="14" rx="3"/>';
  }

  if (normalized.includes('electronic')) {
    return '<rect x="18" y="12" width="12" height="24" rx="2.5"/><circle cx="24" cy="31" r="1.4"/>';
  }

  if (normalized.includes('cloth')) {
    return '<path d="M19 15l5 4 5-4 4 4-3 4v10H18V23l-3-4z"/>';
  }

  return '<path d="M16 18l8-4 8 4-8 4z"/><path d="M16 18v10l8 4 8-4V18"/><path d="M24 22v10"/>';
}

function markerIconForCategory(category) {
  if (!window.google?.maps) {
    return undefined;
  }

  const iconPath = markerSvgPathByCategory(category);
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
      <circle cx="24" cy="24" r="21" fill="#d8d18d" stroke="#bdb46e" stroke-width="2" />
      <g fill="none" stroke="#111" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        ${iconPath}
      </g>
    </svg>
  `;

  return {
    url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    scaledSize: new window.google.maps.Size(42, 42),
    anchor: new window.google.maps.Point(21, 21)
  };
}

function getDistanceInMiles(lat1, lon1, lat2, lon2) {
  const R = 3958.8; // Radius of the earth in miles
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function List() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchFilter, setSearchFilter] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [radiusFilter, setRadiusFilter] = useState(1); // Default to 1 mile radius
  const [isLocating, setIsLocating] = useState(false);
  const [locationError, setLocationError] = useState('');
  const [selectedMarkerItemId, setSelectedMarkerItemId] = useState(null);
  const navigate = useNavigate();

  const googleMapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';
  const { isLoaded: isMapLoaded, loadError: mapLoadError } = useJsApiLoader({
    id: 'lost-and-found-google-map',
    googleMapsApiKey,
    libraries: GOOGLE_MAP_LIBRARIES
  });
  const hasGoogleMaps = Boolean(googleMapsApiKey) && isMapLoaded && Boolean(window.google?.maps);

  useEffect(() => {
    const fetchItems = async () => {
      setLoading(true);

      try {
        const params = new URLSearchParams();

        if (categoryFilter !== 'ALL') {
          params.set('category', categoryFilter);
        }
        if (statusFilter !== 'ALL') {
          params.set('status', statusFilter);
        }

        const queryString = params.toString();
        const url = queryString
          ? `${API_BASE_URL}/items?${queryString}`
          : `${API_BASE_URL}/items`;

        const response = await authFetch(url, {
          method: 'GET',
          credentials: 'include'
        });

        if (response.ok) {
          const data = await response.json();
          setItems(data);
        } else {
          console.error("Failed to fetch items");
        }
      } catch (error) {
        console.error("Error connecting to server:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchItems();
  }, [categoryFilter, statusFilter]);

  const filteredItems = useMemo(() => {
    const keyword = searchFilter.trim().toLowerCase();

    return items.filter((item) => {
      const matchKeyword =
        !keyword ||
        item.title?.toLowerCase().includes(keyword) ||
        item.description?.toLowerCase().includes(keyword) ||
        item.locationText?.toLowerCase().includes(keyword) ||
        item.detailLocation?.toLowerCase().includes(keyword);

      let matchLocation = true;
      if (userLocation) {
        if (Number.isFinite(Number(item.latitude)) && Number.isFinite(Number(item.longitude))) {
          const distance = getDistanceInMiles(
            userLocation.lat,
            userLocation.lng,
            Number(item.latitude),
            Number(item.longitude)
          );
          matchLocation = distance <= radiusFilter;
        } else {
          matchLocation = false;
        }
      }

      return matchKeyword && matchLocation;
    });
  }, [items, searchFilter, userLocation, radiusFilter]);

  const toggleLocationFilter = async () => {
    if (userLocation) {
      // Toggle off
      setUserLocation(null);
      setLocationError('');
      return;
    }

    setIsLocating(true);
    setLocationError('');
    try {
      setUserLocation(await getCurrentCoordinates());
    } catch (error) {
      setLocationError(error.message);
    } finally {
      setIsLocating(false);
    }
  };

  const mapItems = useMemo(
    () =>
      filteredItems.filter(
        (item) =>
          Number.isFinite(Number(item.latitude)) &&
          Number.isFinite(Number(item.longitude))
      ),
    [filteredItems]
  );

  const displayItems = useMemo(() => {
    if (selectedMarkerItemId == null) return filteredItems;
    return filteredItems.filter((item) => item.itemId === selectedMarkerItemId);
  }, [filteredItems, selectedMarkerItemId]);

  const selectedMarkerItem = useMemo(
    () => mapItems.find((item) => item.itemId === selectedMarkerItemId) || null,
    [mapItems, selectedMarkerItemId]
  );

  const mapCenter = useMemo(() => {
    if (userLocation) return userLocation;
    
    if (mapItems.length === 0) {
      return DEFAULT_CENTER;
    }

    const total = mapItems.reduce(
      (acc, item) => ({
        lat: acc.lat + Number(item.latitude),
        lng: acc.lng + Number(item.longitude)
      }),
      { lat: 0, lng: 0 }
    );

    return {
      lat: total.lat / mapItems.length,
      lng: total.lng / mapItems.length
    };
  }, [mapItems, userLocation]);

  const mapZoom = useMemo(() => {
  if (!userLocation) return 15;
  if (radiusFilter <= 0.5) return 15;
  if (radiusFilter <= 1) return 14;
  if (radiusFilter <= 5) return 12;
  return 11; // For 10 miles
}, [userLocation, radiusFilter]);

  return (
    <div className="list-container">
      <header className="list-header">
        <h1>Map</h1>
      </header>

      <section className="filter-section">
        <input
          type="text"
          className="filter-input"
          placeholder="Search by title, description, or location"
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
        />

        <select
          className="filter-select"
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
        >
          <option value="ALL">All Categories</option>
          {CATEGORY_OPTIONS.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>

        <select
          className="filter-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Status</option>
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>

        <button
          className={`btn-location ${userLocation ? 'active' : ''}`}
          onClick={toggleLocationFilter}
          disabled={isLocating}
        >
          {isLocating ? 'Locating...' : userLocation ? 'Clear Location' : '📍 Use My Location'}
        </button>

        {userLocation && (
          <select
            className="filter-select radius-select"
            value={radiusFilter}
            onChange={(e) => setRadiusFilter(Number(e.target.value))}
          >
            <option value={0.5}>Within 0.5 miles</option>
            <option value={1}>Within 1 mile</option>
            <option value={5}>Within 5 miles</option>
            <option value={10}>Within 10 miles</option>
          </select>
        )}
      </section>
      
      {locationError && <p className="location-error">{locationError}</p>}

      <section className="map-section">
        {!googleMapsApiKey && (
          <p className="map-message">
            Add GOOGLE_MAPS_API_KEY in your frontend environment to enable the map.
          </p>
        )}

        {googleMapsApiKey && mapLoadError && (
          <p className="map-message">Failed to load Google Map.</p>
        )}

        {googleMapsApiKey && !mapLoadError && !hasGoogleMaps && (
          <p className="map-message">Loading map...</p>
        )}

        {hasGoogleMaps && (
          <div className="map-overlay-wrap">
            <GoogleMap
              mapContainerClassName="items-map"
              center={mapCenter}
              zoom={mapZoom}
              onClick={() => setSelectedMarkerItemId(null)}
              options={{ streetViewControl: false, mapTypeControl: false }}
            >
              {mapItems.map((item) => (
                <MarkerF
                  key={`marker-${item.itemId}`}
                  position={{ lat: Number(item.latitude), lng: Number(item.longitude) }}
                  onClick={() => setSelectedMarkerItemId(item.itemId)}
                  icon={markerIconForCategory(item.category)}
                />
              ))}

              {userLocation && (
                <>
                  <MarkerF
                    position={userLocation}
                    icon={{
                      path: window.google.maps.SymbolPath.CIRCLE,
                      scale: 8,
                      fillColor: '#4285F4',
                      fillOpacity: 1,
                      strokeColor: '#ffffff',
                      strokeWeight: 2,
                    }}
                    zIndex={1000} 
                  />

                  <CircleF
                    center={userLocation}
                    radius={radiusFilter * 1609.34}
                    options={{
                      fillColor: '#4285F4',
                      fillOpacity: 0.1,
                      strokeColor: '#4285F4',
                      strokeOpacity: 0.5,
                      strokeWeight: 2,
                      clickable: false, 
                      zIndex: 1
                    }}
                  />
                </>
              )}

              {selectedMarkerItem && (
                <InfoWindowF
                  position={{
                    lat: Number(selectedMarkerItem.latitude),
                    lng: Number(selectedMarkerItem.longitude)
                  }}
                  onCloseClick={() => setSelectedMarkerItemId(null)}
                >
                  <div className="map-item-popup" onClick={() => navigate(`/items/${selectedMarkerItem.itemId}`)}>
                    <img
                      src={selectedMarkerItem.imageUrl}
                      alt={selectedMarkerItem.title}
                      className="map-item-popup-image"
                      draggable={false}
                    />
                    <div className="map-item-popup-content">
                      <h4>{selectedMarkerItem.title}</h4>
                      <p>{formatItemLocation(selectedMarkerItem.locationText, selectedMarkerItem.detailLocation)}</p>
                      <p>{selectedMarkerItem.status}</p>
                    </div>
                  </div>
                </InfoWindowF>
              )}
            </GoogleMap>

            {selectedMarkerItemId != null && (
              <button className="clear-marker-filter-btn" onClick={() => setSelectedMarkerItemId(null)}>
                Show all
              </button>
            )}
          </div>
        )}
      </section>

      {loading && <p>Loading items...</p>}

      {!loading && displayItems.length === 0 && (
        <p className="empty-msg">No items reported lost or found yet.</p>
      )}


      <div className="items-grid">
        {displayItems.map((item) => (
          <div key={item.itemId} className="item-card" onClick={() => navigate(`/items/${item.itemId}`)} style={{ cursor: 'pointer' }}>

            <div className="item-image-container">
              {item.imageUrl && <img src={item.imageUrl} alt={item.title} className="item-image" />}
            </div>

            <div className="item-details">
              <h3 className="item-title">{item.title}</h3>
              <p className="item-location">📍 {formatItemLocation(item.locationText, item.detailLocation)}</p>
              <p className="item-description">{item.description}</p>
              
              <div className="item-footer">
                <span className="item-category">{item.category}</span>
                <span className={`item-status ${item.status.toLowerCase()}`}>{item.status}</span>
              </div>
            </div>

          </div>
        ))}
      </div>

      <button 
        className="fab-create-btn" 
        onClick={() => navigate('/found/report')}
        title="Report Found Item"
      >
        <svg 
          xmlns="http://www.w3.org/2000/svg" 
          width="28" 
          height="28" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2.5" 
          strokeLinecap="round" 
          strokeLinejoin="round"
        >
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
      </button>

    </div>
  );
}

export default List;
