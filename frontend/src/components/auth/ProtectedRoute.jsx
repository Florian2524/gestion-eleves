import {
  Navigate,
  useLocation,
} from "react-router";

import { useAuth } from "../../context/authContextCore";

export default function ProtectedRoute({
  children,
}) {
  const location = useLocation();

  const {
    isAuthenticated,
  } = useAuth();

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/connexion"
        replace
        state={{
          from: location.pathname,
        }}
      />
    );
  }

  return children;
}
