/** Label + control + hint/error wrapper. The child control must use `id` so the label points at it. */
export default function Field({ id, label, required = false, error, hint, children }) {
  return (
    <div className="flow-field">
      <label className="flow-label" htmlFor={id}>
        {label}
        {required && <span className="flow-required" aria-hidden="true">*</span>}
      </label>
      {children}
      {error ? (
        <p className="flow-error" id={`${id}-error`} role="alert">
          {error}
        </p>
      ) : (
        hint && <p className="flow-hint">{hint}</p>
      )}
    </div>
  );
}
