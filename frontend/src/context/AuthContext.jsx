import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { authenticateUser } from "../api/authApi";
import {
  clearStoredAuth,
  readStoredAuth,
  storeAuth,
} from "../api/authStorage";
import { AuthContext } from "./authContextCore";

export function AuthProvider({
  children,
}) {
  const [auth, setAuth] = useState(
    () => readStoredAuth(),
  );

  const login = useCallback(
    async (credentials) => {
      const response =
        await authenticateUser(credentials);

      const storedAuth =
        storeAuth(response);

      setAuth(storedAuth);

      return storedAuth;
    },
    [],
  );

  const logout = useCallback(() => {
    clearStoredAuth();
    setAuth(null);
  }, []);

  useEffect(() => {
    function handleUnauthorized() {
      setAuth(null);
    }

    window.addEventListener(
      "auth:unauthorized",
      handleUnauthorized,
    );

    return () => {
      window.removeEventListener(
        "auth:unauthorized",
        handleUnauthorized,
      );
    };
  }, []);

  const contextValue = useMemo(
    () => ({
      auth,
      login,
      logout,
      isAuthenticated:
        Boolean(auth?.token),
    }),
    [
      auth,
      login,
      logout,
    ],
  );

  return (
    <AuthContext.Provider
      value={contextValue}
    >
      {children}
    </AuthContext.Provider>
  );
}
