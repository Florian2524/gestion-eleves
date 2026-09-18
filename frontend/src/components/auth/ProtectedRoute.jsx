import {
  Navigate,
  useLocation,
} from "react-router";

import { useAuth } from "../../context/authContextCore";

export default function ProtectedRoute({
  children,
  allowedRoles,
}) {
  const location = useLocation();

  const {
    isAuthenticated,
    auth,
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

  if (allowedRoles && !allowedRoles.includes(auth?.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
