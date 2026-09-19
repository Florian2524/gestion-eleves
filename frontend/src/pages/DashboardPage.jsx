import {
  ArrowRight,
  FileText,
  GraduationCap,
  LogOut,
  NotebookPen,
  Users,
} from "lucide-react";

import {
  Link,
  useNavigate,
} from "react-router";

import Button from "../components/ui/Button";
import { useAuth } from "../context/authContextCore";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

const modules = [
  {
    title: "Élèves",
    description:
      "Consulter les dossiers et les informations scolaires.",
    icon: Users,
    to: "/eleves",
    roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"],
  },
  {
    title: "Notes",
    description:
      "Saisir et consulter les résultats des évaluations.",
    icon: NotebookPen,
    to: "/notes",
    roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"],
  },
  {
    title: "Bulletins",
    description:
      "Calculer les moyennes et télécharger les PDF.",
    icon: FileText,
    to: "/bulletins",
    roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"],
  },
  ...[
    ["Classes", "/classes", ["ADMIN"]],
    ["Enseignants", "/enseignants", ["ADMIN"]],
    ["Responsables", "/responsables", ["ADMIN"]],
    ["Responsabilités légales", "/responsabilites-legales", ["ADMIN"]],
    ["Comptes utilisateurs", "/comptes-utilisateurs", ["ADMIN"]],
    ["Matières", "/matieres", ["ADMIN"]],
    ["Périodes", "/periodes", ["ADMIN"]],
    ["Inscriptions", "/inscriptions", ["ADMIN"]],
    ["Scolarités", "/scolarites", ["ADMIN"]],
    ["Enseignements", "/enseignements", ["ADMIN", "ENSEIGNANT", "RESPONSABLE"]],
    ["Évaluations", "/evaluations", ["ADMIN", "ENSEIGNANT", "RESPONSABLE"]],
  ].map(([title, to, roles]) => ({ title, to, roles, icon: FileText, description: `Consulter ${title.toLowerCase()}.` })),
];

export default function DashboardPage() {
  const navigate = useNavigate();

  const {
    auth,
    logout,
  } = useAuth();

  const roleLabel =
    roleLabels[auth?.role] ??
    auth?.role ??
    "Utilisateur";

  function handleLogout() {
    logout();

    navigate(
      "/connexion",
      {
        replace: true,
      },
    );
  }

  return (
    <div className="min-h-screen bg-school-paper">
      <header className="bg-school-blue-dark text-white">
        <div className="mx-auto flex max-w-[1440px] flex-wrap items-center justify-between gap-5 px-4 py-5 sm:px-6 lg:px-10">
          <Link
            to="/"
            className="flex items-center gap-3"
          >
            <span className="grid size-12 place-items-center bg-brand-yellow text-black">
              <GraduationCap size={27} />
            </span>

            <span>
              <span className="block font-display text-2xl font-bold uppercase">
                Scolarité
              </span>

              <span className="block text-xs uppercase tracking-[0.18em] text-white/55">
                Gestion scolaire
              </span>
            </span>
          </Link>

          <div className="flex items-center gap-5">
            <div className="hidden text-right sm:block">
              <p className="font-semibold">
                {auth?.emailConnexion}
              </p>

              <p className="text-xs uppercase tracking-wider text-white/55">
                {roleLabel}
              </p>
            </div>

            <Button
              variant="ghost"
              onClick={handleLogout}
            >
              <LogOut size={18} />
              Déconnexion
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1440px] px-4 py-10 sm:px-6 lg:px-10 lg:py-16">
        <section className="grid gap-8 bg-white p-7 shadow-[0_18px_50px_rgba(23,23,23,0.06)] lg:grid-cols-[1fr_auto] lg:p-10">
          <div>
            <p className="font-display text-lg font-semibold uppercase tracking-[0.18em] text-school-blue">
              Tableau de bord
            </p>

            <h1 className="mt-3 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
              Bienvenue dans votre espace
            </h1>

            <p className="mt-6 max-w-2xl leading-7 text-school-muted">
              {auth?.role === "RESPONSABLE"
                ? "Consultez les informations, les notes et les bulletins de vos élèves."
                : "Retrouvez les outils essentiels pour gérer les dossiers des élèves, les notes et les bulletins de l’établissement."}
            </p>
          </div>

          <div className="flex min-w-56 items-center gap-4 bg-school-blue p-6 text-white">
            <GraduationCap
              size={36}
              className="text-brand-yellow"
            />

            <div>
              <p className="text-sm uppercase tracking-wider text-white/60">
                Profil connecté
              </p>

              <p className="mt-1 font-bold">
                {roleLabel}
              </p>
            </div>
          </div>
        </section>

        <section className="mt-8 grid gap-5 md:grid-cols-3">
          {modules.filter((module) => module.roles.includes(auth?.role)).map((module) => {
            const Icon = module.icon;

            return (
              <article
                key={module.title}
                className="border border-slate-200 bg-white p-7"
              >
                <span className="grid size-13 place-items-center bg-brand-yellow text-black">
                  <Icon size={25} />
                </span>

                <h2 className="mt-8 font-display text-4xl font-bold uppercase text-school-blue">
                  {module.title}
                </h2>

                <p className="mt-4 leading-7 text-school-muted">
                  {auth?.role === "RESPONSABLE" && module.to === "/notes"
                    ? "Consulter les résultats de vos élèves."
                    : module.description}
                </p>

                {module.to ? (
                  <Link
                    to={module.to}
                    className="mt-8 inline-flex items-center gap-2 border-t border-slate-200 pt-5 font-semibold text-school-blue transition hover:text-school-blue-dark"
                  >
                    Ouvrir le module
                    <ArrowRight size={18} />
                  </Link>
                ) : (
                  <p className="mt-8 border-t border-slate-200 pt-5 text-sm font-semibold text-school-muted">
                    Module à intégrer prochainement.
                  </p>
                )}
              </article>
            );
          })}
        </section>

      </main>
    </div>
  );
}
