/** Mirrors the backend password policy for immediate role-aware feedback. */
export function isStrongPassword(password: string, minLength = 10) {
  return (
    password.length >= minLength &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /[0-9]/.test(password) &&
    /[^A-Za-z0-9\s]/.test(password)
  );
}
