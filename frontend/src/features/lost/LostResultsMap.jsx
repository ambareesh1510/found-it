import { useEffect, useMemo, useState } from 'react';
import { CircleF, GoogleMap, InfoWindowF, MarkerF, PolylineF, useJsApiLoader } from '@react-google-maps/api';
import { SEARCH_RADIUS_METERS } from '../../core/lostSearch';
import ItemPhoto from '../../ui/ItemPhoto';

const MAP_LOADER_ID = 'lost-and-found-google-map';
const GOOGLE_MAP_LIBRARIES = ['places'];
const DEFAULT_CENTER = { lat: 33.7756, lng: -84.3963 };

function toPosition(point) {
  const lat = Number(point?.latitude ?? point?.lat);
  const lng = Number(point?.longitude ?? point?.lng);
  return Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null;
}

export default function LostResultsMap({ declaration, results, timelinePoints, selectedItemId, onSelectItem }) {
  const [map, setMap] = useState(null);
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';
  const { isLoaded, loadError } = useJsApiLoader({
    id: MAP_LOADER_ID,
    googleMapsApiKey: apiKey,
    libraries: GOOGLE_MAP_LIBRARIES,
  });

  const resultMarkers = useMemo(
    () => results
      .map((item) => ({ item, position: toPosition(item) }))
      .filter(({ position }) => position),
    [results],
  );
  const timelinePath = useMemo(
    () => (timelinePoints || []).map(toPosition).filter(Boolean),
    [timelinePoints],
  );
  const manualCenter = declaration.useTimeline ? null : toPosition(declaration.location);
  const initialCenter = manualCenter || timelinePath.at(-1) || resultMarkers[0]?.position || DEFAULT_CENTER;
  const selectedMarker = resultMarkers.find(({ item }) => item.itemId === selectedItemId) || null;

  useEffect(() => {
    if (!map || !globalThis.google?.maps) return;
    const points = [manualCenter, ...timelinePath, ...resultMarkers.map(({ position }) => position)].filter(Boolean);
    if (points.length === 0) return;
    if (points.length === 1) {
      map.setCenter(points[0]);
      map.setZoom(15);
      return;
    }
    const bounds = new globalThis.google.maps.LatLngBounds();
    points.forEach((point) => bounds.extend(point));
    map.fitBounds(bounds, 40);
  }, [manualCenter, map, resultMarkers, timelinePath]);

  if (!apiKey) {
    return <div className="flow-map-message">Add `VITE_GOOGLE_MAPS_API_KEY` to display the results map.</div>;
  }
  if (loadError) {
    return <div className="flow-map-message flow-map-message--error">The results map could not be loaded.</div>;
  }
  if (!isLoaded) {
    return <div className="flow-map-message">Loading results map…</div>;
  }

  return (
    <section className="flow-results-map" aria-label="Search area and matching found items">
      <GoogleMap
        mapContainerClassName="flow-results-map__canvas"
        center={initialCenter}
        zoom={15}
        onLoad={setMap}
        onUnmount={() => setMap(null)}
        options={{ streetViewControl: false, mapTypeControl: false, fullscreenControl: false }}
      >
        {manualCenter && (
          <>
            <MarkerF
              position={manualCenter}
              title="Declared lost location"
              icon={{
                path: globalThis.google.maps.SymbolPath.CIRCLE,
                scale: 7,
                fillColor: '#ea580c',
                fillOpacity: 1,
                strokeColor: '#ffffff',
                strokeWeight: 2,
              }}
            />
            <CircleF
              center={manualCenter}
              radius={SEARCH_RADIUS_METERS}
              options={{ fillColor: '#f59e0b', fillOpacity: 0.1, strokeColor: '#ea580c', strokeOpacity: 0.7 }}
            />
          </>
        )}

        {timelinePath.length > 0 && (
          <PolylineF
            path={timelinePath}
            options={{ strokeColor: '#ea580c', strokeOpacity: 0.9, strokeWeight: 4 }}
          />
        )}

        {resultMarkers.map(({ item, position }) => (
          <MarkerF
            key={item.itemId}
            position={position}
            title={item.title}
            onClick={() => onSelectItem(item.itemId)}
            zIndex={selectedItemId === item.itemId ? 100 : 10}
            icon={{
              path: globalThis.google.maps.SymbolPath.CIRCLE,
              scale: selectedItemId === item.itemId ? 10 : 8,
              fillColor: selectedItemId === item.itemId ? '#f59e0b' : '#1d4ed8',
              fillOpacity: 1,
              strokeColor: '#ffffff',
              strokeWeight: 2,
            }}
          />
        ))}

        {selectedMarker && (
          <InfoWindowF
            position={selectedMarker.position}
            onCloseClick={() => onSelectItem(null)}
            options={{ pixelOffset: new globalThis.google.maps.Size(0, -12) }}
          >
            <div className="flow-map-preview">
              <ItemPhoto src={selectedMarker.item.imageUrl} alt={selectedMarker.item.title} />
              <strong>{selectedMarker.item.title}</strong>
              <span>{selectedMarker.item.locationText || 'Approximate found location'}</span>
            </div>
          </InfoWindowF>
        )}
      </GoogleMap>
      <div className="flow-results-map__legend">
        <span><i className="flow-map-dot flow-map-dot--search" />{declaration.useTimeline ? 'Your Timeline' : 'Search area'}</span>
        <span><i className="flow-map-dot flow-map-dot--result" />Matching found item</span>
      </div>
    </section>
  );
}
