import { useState } from "react";
import {
  CircleUserRound,
  GraduationCap,
  Menu,
  X,
} from "lucide-react";

const navigation = [
  {
    label: "Accueil",
    href: "#accueil",
  },
  {
    label: "Élèves",
    href: "/eleves",
  },
  {
    label: "Notes",
    href: "#notes",
  },
  {
    label: "Bulletins",
    href: "#bulletins",
  },
];

function SchoolBrand() {
  return (
    <a
      href="#accueil"
      className="flex items-center gap-3"
      aria-label="Retour à l'accueil"
    >
      <span className="grid size-14 place-items-center border-2 border-school-blue text-school-blue">
        <GraduationCap
          size={31}
          strokeWidth={1.9}
        />
      </span>

      <span>
        <span className="block font-display text-2xl font-bold uppercase leading-none tracking-wide text-school-blue">
          Scolarité
        </span>

        <span className="mt-1 block text-[0.65rem] font-bold uppercase tracking-[0.24em] text-school-muted">
          Gestion des élèves
        </span>
      </span>
    </a>
  );
}

export default function AppHeader() {
  const [mobileOpen, setMobileOpen] =
    useState(false);

  return (
    <>
      <div className="bg-school-blue-dark text-white">
        <div className="mx-auto flex max-w-[1440px] items-center justify-between gap-4 px-4 py-2 text-xs sm:px-6 lg:px-10">
          <span className="font-semibold">
            Portail de gestion scolaire
          </span>

          <span className="hidden text-white/70 sm:inline">
            Année scolaire en cours
          </span>
        </div>
      </div>

      <header className="relative z-30 border-b border-slate-200 bg-white">
        <div className="mx-auto flex min-h-24 max-w-[1440px] items-center justify-between gap-8 px-4 sm:px-6 lg:px-10">
          <SchoolBrand />

          <nav
            className="hidden items-center lg:flex"
            aria-label="Navigation principale"
          >
            {navigation.map((item) => (
              <a
                key={item.label}
                href={item.href}
                className="border-r border-slate-200 px-6 font-display text-lg font-semibold uppercase tracking-wide text-school-blue transition first:border-l hover:text-brand-black"
              >
                {item.label}
              </a>
            ))}
          </nav>

          <a
            href="/connexion"
            className="hidden items-center gap-3 font-display text-lg font-semibold uppercase text-school-blue transition hover:text-brand-black lg:flex"
          >
            <span className="grid size-11 place-items-center rounded-full bg-brand-yellow text-black">
              <CircleUserRound size={26} />
            </span>

            Connexion
          </a>

          <button
            type="button"
            className="grid size-12 place-items-center border border-school-blue text-school-blue lg:hidden"
            aria-label={
              mobileOpen
                ? "Fermer le menu"
                : "Ouvrir le menu"
            }
            aria-expanded={mobileOpen}
            onClick={() =>
              setMobileOpen((current) => !current)
            }
          >
            {mobileOpen ? (
              <X size={25} />
            ) : (
              <Menu size={25} />
            )}
          </button>
        </div>

        {mobileOpen && (
          <nav
            className="border-t border-slate-200 bg-white px-4 py-4 lg:hidden"
            aria-label="Navigation mobile"
          >
            <div className="mx-auto max-w-[1440px]">
              {navigation.map((item) => (
                <a
                  key={item.label}
                  href={item.href}
                  className="block border-b border-slate-200 px-3 py-4 font-display text-xl font-semibold uppercase tracking-wide text-school-blue"
                  onClick={() => setMobileOpen(false)}
                >
                  {item.label}
                </a>
              ))}

              <a
                href="/connexion"
                className="mt-4 flex items-center gap-3 bg-brand-yellow px-4 py-3 font-display text-lg font-semibold uppercase text-black"
                onClick={() => setMobileOpen(false)}
              >
                <CircleUserRound size={24} />
                Connexion
              </a>
            </div>
          </nav>
        )}
      </header>
    </>
  );
}
