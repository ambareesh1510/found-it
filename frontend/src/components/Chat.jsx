import { authFetch } from '../core/http.js';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import sendIcon from '../assets/send.png';
import { API_BASE_URL, WS_BASE_URL } from '../core/config.js';
import { createChatSocketTicket, confirmItemResolution } from '../core/services.js';
import './Chat.css';

function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  const time = date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  // Show the date too once the message is from an earlier day, so old threads aren't ambiguous.
  const isToday = new Date().toDateString() === date.toDateString();
  return isToday ? time : `${date.toLocaleDateString([], { month: 'short', day: 'numeric' })}, ${time}`;
}

// The socket is authorised by a short-lived ticket in the query string, not by the session cookie or token.
function chatSocketUrl(ticket) {
  return `${WS_BASE_URL}/ws/chat?ticket=${encodeURIComponent(ticket)}`;
}

/** Image that falls back to an initial-letter placeholder when the source is missing or fails to load. */
function ChatImage({ src, alt, className }) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    const initial = (alt || '?').trim().charAt(0).toUpperCase() || '?';
    return <span className={`${className} chat-image-fallback`} role="img" aria-label={alt}>{initial}</span>;
  }
  return <img src={src} alt={alt} className={className} draggable={false} onError={() => setFailed(true)} />;
}

function sortThreadsByLastMessage(threads) {
  return [...threads].sort((a, b) => new Date(b.lastMessageAt) - new Date(a.lastMessageAt));
}

function Chat({ user }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const [threads, setThreads] = useState([]);
  const [selectedThreadId, setSelectedThreadId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [messageText, setMessageText] = useState('');
  const [loadingThreads, setLoadingThreads] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [confirmingResolution, setConfirmingResolution] = useState(false);
  const [resolutionError, setResolutionError] = useState('');
  const messagesEndRef = useRef(null);
  const websocketRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const selectedThreadIdRef = useRef(null);

  const visibleThreads = threads;

  const selectedThread = useMemo(
    () => visibleThreads.find((thread) => thread.threadId === selectedThreadId) || visibleThreads[0] || null,
    [visibleThreads, selectedThreadId]
  );

  useEffect(() => {
    selectedThreadIdRef.current = selectedThreadId;
  }, [selectedThreadId]);

  useEffect(() => {
    const fetchThreads = async () => {
      setLoadingThreads(true);
      setError('');

      try {
        const response = await authFetch(`${API_BASE_URL}/chat/threads`, {
          method: 'GET',
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error('Failed to load chats.');
        }

        const data = await response.json();
        setThreads(Array.isArray(data) ? data : []);
      } catch {
        setError('Could not load chat threads right now.');
      } finally {
        setLoadingThreads(false);
      }
    };

    fetchThreads();
  }, []);

  useEffect(() => {
    if (visibleThreads.length === 0) {
      setSelectedThreadId(null);
      return;
    }

    const queryThreadId = Number(searchParams.get('threadId'));
    const queryThreadExists = Number.isFinite(queryThreadId) && visibleThreads.some((thread) => thread.threadId === queryThreadId);

    setSelectedThreadId((current) => {
      if (current && visibleThreads.some((thread) => thread.threadId === current)) {
        return current;
      }

      return queryThreadExists ? queryThreadId : visibleThreads[0].threadId;
    });
  }, [visibleThreads, searchParams]);

  // Keyed on the thread id, not the thread object: incoming messages replace the thread object in
  // state, and depending on the object would refetch the whole history on every message.
  const selectedThreadId_forMessages = selectedThread?.threadId ?? null;
  useEffect(() => {
    if (selectedThreadId_forMessages == null) {
      setMessages([]);
      return;
    }

    const fetchMessages = async () => {
      setLoadingMessages(true);

      try {
        const response = await authFetch(`${API_BASE_URL}/chat/threads/${selectedThreadId_forMessages}/messages`, {
          method: 'GET',
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error('Failed to load messages.');
        }

        const data = await response.json();
        setMessages(Array.isArray(data) ? data : []);
      } catch {
        setMessages([]);
      } finally {
        setLoadingMessages(false);
      }
    };

    fetchMessages();
  }, [selectedThreadId_forMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    let disposed = false;

    const connect = async () => {
      let ticket;
      try {
        ({ ticket } = await createChatSocketTicket());
      } catch {
        // Offline, or the session has ended. Retry shortly unless the screen has closed.
        if (!disposed) reconnectTimeoutRef.current = window.setTimeout(connect, 2000);
        return;
      }
      if (disposed) return;

      const socket = new WebSocket(chatSocketUrl(ticket));
      websocketRef.current = socket;

      socket.onmessage = (event) => {
        let payload;

        try {
          payload = JSON.parse(event.data);
        } catch {
          return;
        }

        if (payload.type === 'chat_error') {
          setSending(false);
          setError(payload.error || 'Could not send the message right now.');
          return;
        }

        if (payload.type !== 'chat_message' || !payload.message || !payload.thread) {
          return;
        }

        setError('');
        setSending(false);
        setThreads((prev) => {
          const existing = prev.some((thread) => thread.threadId === payload.thread.threadId);
          const next = existing
            ? prev.map((thread) => (
                thread.threadId === payload.thread.threadId ? payload.thread : thread
              ))
            : [...prev, payload.thread];

          return sortThreadsByLastMessage(next);
        });

        if (selectedThreadIdRef.current === payload.threadId) {
          setMessages((prev) => (
            prev.some((message) => message.messageId === payload.message.messageId)
              ? prev
              : [...prev, payload.message]
          ));
        }
      };

      socket.onclose = () => {
        websocketRef.current = null;

        if (disposed) return;

        reconnectTimeoutRef.current = window.setTimeout(connect, 2000);
      };
    };

    connect();

    return () => {
      disposed = true;

      if (reconnectTimeoutRef.current) {
        window.clearTimeout(reconnectTimeoutRef.current);
      }

      websocketRef.current?.close();
      websocketRef.current = null;
    };
  }, []);

  const updateThreadPreview = (threadSummary) => {
    setThreads((prev) => {
      const existing = prev.some((thread) => thread.threadId === threadSummary.threadId);
      const next = existing
        ? prev.map((thread) => (
            thread.threadId === threadSummary.threadId ? threadSummary : thread
          ))
        : [...prev, threadSummary];

      return sortThreadsByLastMessage(next);
    });
  };

  const handleThreadSelect = (threadId) => {
    setSelectedThreadId(threadId);
    setSearchParams({ threadId: String(threadId) });
  };

  const handleSend = async (event) => {
    event.preventDefault();

    const trimmed = messageText.trim();
    if (!trimmed || !selectedThread) return;

    setError('');

    const socket = websocketRef.current;
    if (socket?.readyState === WebSocket.OPEN) {
      // Over the socket the message is echoed back by the server, which appends it; just clear the input.
      socket.send(JSON.stringify({
        type: 'send_message',
        threadId: selectedThread.threadId,
        messageText: trimmed
      }));
      setMessageText('');
      return;
    }

    // Fallback to REST when the socket is not open.
    setSending(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/chat/threads/${selectedThread.threadId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ messageText: trimmed })
      });

      if (!response.ok) {
        throw new Error('Failed to send message.');
      }

      const savedMessage = await response.json();
      setMessages((prev) => [...prev, savedMessage]);
      updateThreadPreview({
        ...selectedThread,
        lastMessage: savedMessage.messageText,
        lastMessageAt: savedMessage.sentAt
      });
      setMessageText('');
    } catch {
      setError('Could not send the message right now.');
    } finally {
      setSending(false);
    }
  };

  const handleConfirmResolution = async () => {
    if (!selectedThread) return;

    setConfirmingResolution(true);
    setResolutionError('');
    try {
      const updated = await confirmItemResolution(selectedThread.itemId);
      // Reflect the new item status on every thread that points at this item.
      setThreads((prev) =>
        prev.map((thread) =>
          thread.itemId === updated.itemId ? { ...thread, itemStatus: updated.status } : thread
        )
      );
    } catch (err) {
      setResolutionError(err?.message || 'Could not confirm the resolution.');
    } finally {
      setConfirmingResolution(false);
    }
  };

  // Resolution banner state for the open thread. The poster proposes; the chosen counterparty confirms.
  const itemStatus = selectedThread?.itemStatus;
  const iAmPoster = selectedThread && user?.id === selectedThread.itemPosterId;
  const iAmChosenCounterparty = selectedThread && user?.id === selectedThread.resolvedWithUserId;

  return (
    <div className="chat-page">
      <div className="chat-shell">
        <aside className="chat-sidebar">
          <div className="chat-sidebar-top">
            <div className="chat-route-line">
              <span className="chat-route-title">Messages</span>
            </div>
          </div>

          <div className="chat-thread-list">
            {loadingThreads && <p className="chat-inline-message">Loading chats...</p>}
            {error && <p className="chat-inline-message error">{error}</p>}

            {!loadingThreads && visibleThreads.length === 0 && !error && (
              <p className="chat-inline-message">No conversations yet.</p>
            )}

            {visibleThreads.map((thread) => (
              <button
                key={thread.threadId}
                type="button"
                className={`chat-thread-card ${selectedThread?.threadId === thread.threadId ? 'selected' : ''}`}
                onClick={() => handleThreadSelect(thread.threadId)}
              >
                <ChatImage
                  src={thread.itemImageUrl}
                  alt={thread.itemTitle}
                  className="chat-thread-thumb"
                />

                <div className="chat-thread-copy">
                  <h3>{thread.itemTitle}</h3>
                  <p className="chat-thread-other">{thread.otherUsername}</p>
                  {thread.lastMessage && <p className="chat-thread-preview">{thread.lastMessage}</p>}
                  <span>{formatTime(thread.lastMessageAt)}</span>
                </div>
              </button>
            ))}
          </div>

        </aside>

        <main className="chat-panel">
          {selectedThread ? (
            <>
              <header className="chat-header">
                <ChatImage
                  src={selectedThread.otherUserPicture}
                  alt={selectedThread.otherUsername}
                  className="chat-header-avatar"
                />
                <div className="chat-header-copy">
                  <div className="chat-header-title-row">
                    <h2>{selectedThread.otherUsername}</h2>
                  </div>
                  <p className="chat-header-item">About: {selectedThread.itemTitle}</p>
                </div>
              </header>

              {itemStatus === 'RESOLVED' && (
                <div className="chat-resolution-banner resolved">✓ This item has been resolved.</div>
              )}

              {itemStatus === 'PENDING_RESOLUTION' && iAmChosenCounterparty && (
                <div className="chat-resolution-banner pending">
                  <span>{selectedThread.otherUsername} marked this item as resolved with you. Confirm to complete it.</span>
                  <button
                    type="button"
                    className="chat-resolution-btn"
                    onClick={handleConfirmResolution}
                    disabled={confirmingResolution}
                  >
                    {confirmingResolution ? 'Confirming...' : 'Confirm resolution'}
                  </button>
                  {resolutionError && <span className="chat-resolution-error" role="alert">{resolutionError}</span>}
                </div>
              )}

              {itemStatus === 'PENDING_RESOLUTION' && iAmPoster && (
                <div className="chat-resolution-banner pending">
                  Waiting for {selectedThread.otherUsername} to confirm the resolution.
                </div>
              )}

              <section className="chat-messages">
                {loadingMessages ? (
                  <div className="chat-loading-state">Loading messages...</div>
                ) : messages.length === 0 ? (
                  <div className="chat-empty-state">
                    <h3>No messages yet</h3>
                    <p>Start the conversation from the item page or send the first message below.</p>
                  </div>
                ) : (
                  messages.map((message) => (
                    <div key={message.messageId} className={`chat-message-row ${message.mine ? 'mine' : 'theirs'}`}>
                      {!message.mine && (
                        <ChatImage
                          src={selectedThread.otherUserPicture}
                          alt={selectedThread.otherUsername}
                          className="chat-avatar-pic"
                        />
                      )}

                      <div className={`chat-bubble ${message.mine ? 'mine' : 'theirs'}`}>
                        <p>{message.messageText}</p>
                        <span className="chat-time">{formatTime(message.sentAt)}</span>
                      </div>

                      {message.mine && (
                        <ChatImage
                          src={user?.picture || `https://ui-avatars.com/api/?name=${user?.username || 'User'}&background=random&color=fff&size=128`}
                          alt={user?.username || 'You'}
                          className="chat-avatar-pic mine"
                        />
                      )}
                    </div>
                  ))
                )}
                <div ref={messagesEndRef} />
              </section>

              {error && <p className="chat-send-error">{error}</p>}

              <form className="chat-composer" onSubmit={handleSend}>
                <input
                  type="text"
                  className="chat-input"
                  placeholder="Message Here"
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                />
                <button
                  type="submit"
                  className="chat-send-btn"
                  disabled={sending || !messageText.trim()}
                  aria-label="Send message"
                  title="Send message"
                >
                  {sending ? (
                    <span className="chat-send-loading">⏳</span>
                  ) : (
                    <img src={sendIcon} alt="" className="chat-send-icon" aria-hidden="true" />
                  )}
                </button>
              </form>
            </>
          ) : (
            <div className="chat-empty-state large">
              <h3>Select a conversation</h3>
              <p>Pick a chat from the left or start one from an item page.</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default Chat;
