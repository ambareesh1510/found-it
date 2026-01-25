// Date helpers for "date + time" inputs. Uses only the Date API so it runs unchanged on iOS.
const pad = (n) => String(n).padStart(2, '0');

/** Combines a local `YYYY-MM-DD` date and `HH:mm` time into an ISO-8601 UTC string, or null. */
export function toIsoFromLocal(date, time) {
  if (!date || !time) return null;
  const parsed = new Date(`${date}T${time}`);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString();
}

/** Today's local date as `YYYY-MM-DD`, for a date input's `max` attribute. */
export function toLocalDateInputValue(now = new Date()) {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Shifts an ISO string by a number of hours and returns ISO again. */
export function shiftIsoHours(iso, hours) {
  return new Date(new Date(iso).getTime() + hours * 3600 * 1000).toISOString();
}

/** Formats an ISO timestamp as `2026-09-15 · 14:30` in local time (ui-spec finder card). */
export function formatEventTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} · ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
