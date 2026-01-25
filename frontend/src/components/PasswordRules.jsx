import { passwordRuleStatus } from '../passwordPolicy';

// Live checklist shown under a new-password field. Advisory text; the backend enforces the rules.
function PasswordRules({ password, id }) {
  const rules = passwordRuleStatus(password);

  return (
    <ul id={id} className="auth-rules" aria-label="Password requirements">
      {rules.map((rule) => (
        <li
          key={rule.id}
          className={`auth-rules__item ${password === '' ? '' : rule.met ? 'is-met' : 'is-unmet'}`}
        >
          <span className="auth-rules__mark" aria-hidden="true">
            {password !== '' && rule.met ? '✓' : '•'}
          </span>
          {rule.label}
        </li>
      ))}
    </ul>
  );
}

export default PasswordRules;
