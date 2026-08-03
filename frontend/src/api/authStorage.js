const STORAGE_KEY = "gestion-eleves.auth";

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY);
}

export function storeAuth(authResponse) {
  const expiresInSeconds =
    Number(authResponse?.expiresIn) || 0;

  const storedAuth = {
    ...authResponse,
    storedAt: Date.now(),
    expiresAt:
      expiresInSeconds > 0
        ? Date.now() + expiresInSeconds * 1000
        : null,
  };

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(storedAuth),
  );

  return storedAuth;
}

export function readStoredAuth() {
  const serializedAuth =
    localStorage.getItem(STORAGE_KEY);

  if (!serializedAuth) {
    return null;
  }

  try {
    const storedAuth =
      JSON.parse(serializedAuth);

    if (!storedAuth?.token) {
      clearStoredAuth();
      return null;
    }

    if (
      storedAuth.expiresAt &&
      Date.now() >= storedAuth.expiresAt
    ) {
      clearStoredAuth();
      return null;
    }

    return storedAuth;
  } catch {
    clearStoredAuth();
    return null;
  }
}

export function getAccessToken() {
  return readStoredAuth()?.token ?? null;
}
