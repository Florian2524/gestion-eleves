import { GraduationCap, LayoutDashboard, LogOut } from "lucide-react";
import { Link, NavLink, useNavigate } from "react-router";
import { useAuth } from "../../context/authContextCore";
import Button from "../ui/Button";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

export default function ConnectedHeader() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/connexion", { replace: true });
  }

  return (
    <header className="bg-school-blue-dark text-white">
      <div className="mx-auto grid max-w-[1440px] min-w-0 gap-5 px-4 py-5 sm:px-6 lg:flex lg:items-center lg:justify-between lg:px-10">
        <Link to="/" aria-label="Scolarité — accueil public" className="flex w-fit items-center gap-3 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-yellow">
          <span className="grid size-12 shrink-0 place-items-center bg-brand-yellow text-black"><GraduationCap size={27} aria-hidden="true" /></span>
          <span>
            <span className="block font-display text-2xl font-bold uppercase">Scolarité</span>
            <span className="block text-xs uppercase tracking-[0.16em] text-slate-200">Gestion des élèves</span>
          </span>
        </Link>
        <div className="min-w-0 lg:max-w-sm lg:flex-1 lg:text-right">
          <p className="break-all text-sm font-semibold leading-6">{auth?.emailConnexion}</p>
          <p className="text-sm text-slate-200">{roleLabels[auth?.role] ?? auth?.role ?? "Utilisateur"}</p>
        </div>
        <nav aria-label="Navigation de l’espace connecté" className="flex min-w-0 flex-wrap items-center gap-3">
          <NavLink end to="/espace" className="inline-flex min-h-11 items-center gap-2 border border-white/40 px-3 py-2 text-sm font-bold text-white transition-colors hover:border-brand-yellow hover:bg-brand-yellow hover:text-black focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-yellow aria-[current=page]:border-brand-yellow aria-[current=page]:text-brand-yellow aria-[current=page]:hover:text-black">
            <LayoutDashboard size={18} aria-hidden="true" />Tableau de bord
          </NavLink>
          <Button variant="ghost" onClick={handleLogout}><LogOut size={18} aria-hidden="true" />Déconnexion</Button>
        </nav>
      </div>
    </header>
  );
}
