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
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/eleves"
        element={
          <ProtectedRoute>
            <ElevesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/notes"
        element={
          <ProtectedRoute>
            <NotesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/bulletins"
        element={
          <ProtectedRoute>
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
