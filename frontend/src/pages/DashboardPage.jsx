import {
  ArrowRight,
  FileText,
  GraduationCap,
  LogOut,
  NotebookPen,
  ShieldCheck,
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
  },
  {
    title: "Notes",
    description:
      "Saisir et consulter les résultats des évaluations.",
    icon: NotebookPen,
    to: null,
  },
  {
    title: "Bulletins",
    description:
      "Calculer les moyennes et télécharger les PDF.",
    icon: FileText,
    to: "/bulletins",
  },
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
                Espace sécurisé
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
              Session active
            </p>

            <h1 className="mt-3 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
              Bienvenue dans votre espace
            </h1>

            <p className="mt-6 max-w-2xl leading-7 text-school-muted">
              Votre authentification JWT est valide.
              Vous pouvez accéder aux fonctionnalités
              autorisées pour le rôle
              {` ${roleLabel}`}.
            </p>
          </div>

          <div className="flex min-w-56 items-center gap-4 bg-school-blue p-6 text-white">
            <ShieldCheck
              size={36}
              className="text-brand-yellow"
            />

            <div>
              <p className="text-sm uppercase tracking-wider text-white/60">
                Sécurité
              </p>

              <p className="mt-1 font-bold">
                JWT authentifié
              </p>
            </div>
          </div>
        </section>

        <section className="mt-8 grid gap-5 md:grid-cols-3">
          {modules.map((module) => {
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
                  {module.description}
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

        <section className="mt-8 bg-brand-black p-7 text-white lg:p-9">
          <p className="font-display text-base font-semibold uppercase tracking-[0.18em] text-brand-yellow">
            Informations de session
          </p>

          <dl className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt className="text-xs uppercase tracking-wider text-zinc-500">
                Utilisateur
              </dt>

              <dd className="mt-2 font-semibold">
                #{auth?.idUtilisateur ?? "—"}
              </dd>
            </div>

            <div>
              <dt className="text-xs uppercase tracking-wider text-zinc-500">
                Personne
              </dt>

              <dd className="mt-2 font-semibold">
                #{auth?.idPersonne ?? "—"}
              </dd>
            </div>

            <div>
              <dt className="text-xs uppercase tracking-wider text-zinc-500">
                Rôle
              </dt>

              <dd className="mt-2 font-semibold">
                {roleLabel}
              </dd>
            </div>

            <div>
              <dt className="text-xs uppercase tracking-wider text-zinc-500">
                Type de jeton
              </dt>

              <dd className="mt-2 font-semibold">
                {auth?.tokenType ?? "Bearer"}
              </dd>
            </div>
          </dl>
        </section>
      </main>
    </div>
  );
}
