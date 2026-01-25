import { useState } from 'react';

/** Item photo with a placeholder when there is no photo or it fails to load. */
export default function ItemPhoto({ src, alt }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div className="flow-result__image flow-result__image--empty" role="img" aria-label="No photo">
        No photo
      </div>
    );
  }

  return <img className="flow-result__image" src={src} alt={alt} onError={() => setFailed(true)} />;
}
