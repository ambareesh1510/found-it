import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { contactItemPoster } from '../../core/services';

/**
 * Shared "Contact" action for any screen that lists items. Opens the chat thread with the poster
 * and navigates to it. Only one contact request runs at a time.
 */
export function useContactPoster() {
  const navigate = useNavigate();
  const [contactingId, setContactingId] = useState(null);
  const [error, setError] = useState('');

  const contact = async (itemId) => {
    setContactingId(itemId);
    setError('');
    try {
      const thread = await contactItemPoster(itemId);
      navigate(`/chat?threadId=${thread.threadId}`);
    } catch (err) {
      setError(err.message);
      setContactingId(null);
    }
  };

  return { contactingId, error, contact };
}
