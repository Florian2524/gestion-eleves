import {
  Navigate,
  Route,
  Routes,
} from "react-router";

import ProtectedRoute from "./components/auth/ProtectedRoute";
import BulletinsPage from "./pages/BulletinsPage";
import DashboardPage from "./pages/DashboardPage";
import ElevesPage from "./pages/ElevesPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import NotesPage from "./pages/NotesPage";

const rolesAutorises = ["ADMIN", "ENSEIGNANT", "RESPONSABLE"];

export default function App() {
  return (
    <Routes>
      <Route
        path="/"
        element={<HomePage />}
      />

      <Route
        path="/connexion"
        element={<LoginPage />}
      />

      <Route
        path="/espace"
        element={
          <ProtectedRoute allowedRoles={rolesAutorises}>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/eleves"
        element={
          <ProtectedRoute allowedRoles={rolesAutorises}>
            <ElevesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/notes"
        element={
          <ProtectedRoute allowedRoles={rolesAutorises}>
            <NotesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/bulletins"
        element={
          <ProtectedRoute allowedRoles={rolesAutorises}>
            <BulletinsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="*"
        element={
          <Navigate
            to="/"
            replace
          />
        }
      />
    </Routes>
  );
}
