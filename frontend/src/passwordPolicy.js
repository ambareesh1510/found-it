// Must match PasswordPolicy.java on the backend (length, upper, lower, special).
export const MIN_PASSWORD_LENGTH = 12;

export const PASSWORD_RULES = [
  {
    id: 'length',
    label: `At least ${MIN_PASSWORD_LENGTH} characters`,
    test: (pw) => pw.length >= MIN_PASSWORD_LENGTH,
  },
  {
    id: 'upper',
    label: 'One uppercase letter (A-Z)',
    test: (pw) => /[A-Z]/.test(pw),
  },
  {
    id: 'lower',
    label: 'One lowercase letter (a-z)',
    test: (pw) => /[a-z]/.test(pw),
  },
  {
    id: 'special',
    label: 'One special character (e.g. ! @ # $ %)',
    test: (pw) => /[^A-Za-z0-9\s]/.test(pw),
  },
];

// Returns [{ id, label, met }] so the UI can render a live checklist.
export function passwordRuleStatus(password) {
  return PASSWORD_RULES.map((rule) => ({
    id: rule.id,
    label: rule.label,
    met: rule.test(password),
  }));
}

export function isPasswordValid(password) {
  return PASSWORD_RULES.every((rule) => rule.test(password));
}
