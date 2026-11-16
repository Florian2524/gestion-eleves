import { useRef, useState } from "react";
import { useAuth } from "../../context/authContextCore";
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
    href: "/notes",
  },
  {
    label: "Bulletins",
    href: "/bulletins",
  },
];

function SchoolBrand() {
  return (
    <a
      href="#accueil"
      className="flex min-w-0 items-center gap-3"
      aria-label="Retour à l'accueil"
    >
      <span className="grid size-12 shrink-0 place-items-center border-2 border-school-blue text-school-blue sm:size-14">
        <GraduationCap
          aria-hidden="true"
          size={31}
          strokeWidth={1.9}
        />
      </span>

      <span className="min-w-0">
        <span className="block font-display text-2xl font-bold uppercase leading-none tracking-wide text-school-blue">
          Scolarité
        </span>

        <span className="mt-1 block text-[0.65rem] font-bold uppercase tracking-[0.16em] text-school-muted sm:tracking-[0.24em]">
          Gestion des élèves
        </span>
      </span>
    </a>
  );
}

export default function AppHeader() {
  const { isAuthenticated } = useAuth();
  const accountHref = isAuthenticated ? "/espace" : "/connexion";
  const accountLabel = isAuthenticated ? "Mon espace" : "Connexion";
  const menuButtonRef = useRef(null);
  const [mobileOpen, setMobileOpen] =
    useState(false);

  function handleHeaderKeyDown(event) {
    if (event.key === "Escape" && mobileOpen) {
      event.preventDefault();
      setMobileOpen(false);
      menuButtonRef.current?.focus();
    }
  }

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

      <header onKeyDown={handleHeaderKeyDown} className="relative z-30 border-b border-slate-200 bg-white">
        <div className="mx-auto flex min-h-24 max-w-[1440px] items-center justify-between gap-3 px-4 sm:gap-8 sm:px-6 lg:px-10">
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
            href={accountHref}
            className="hidden items-center gap-3 font-display text-lg font-semibold uppercase text-school-blue transition hover:text-brand-black lg:flex"
          >
            <span className="grid size-11 place-items-center rounded-full bg-brand-yellow text-black">
              <CircleUserRound size={26} aria-hidden="true" />
            </span>

            {accountLabel}
          </a>

          <button
            ref={menuButtonRef}
            type="button"
            className="grid size-12 shrink-0 place-items-center border border-school-blue text-school-blue transition-colors hover:bg-school-blue-light lg:hidden"
            aria-label={
              mobileOpen
                ? "Fermer le menu"
                : "Ouvrir le menu"
            }
            aria-controls="public-mobile-navigation"
            aria-expanded={mobileOpen}
            onClick={() =>
              setMobileOpen((current) => !current)
            }
          >
            {mobileOpen ? (
              <X size={25} aria-hidden="true" />
            ) : (
              <Menu size={25} aria-hidden="true" />
            )}
          </button>
        </div>

        {mobileOpen && (
          <nav
            className="border-t border-slate-200 bg-white px-4 py-4 lg:hidden"
            id="public-mobile-navigation"
            aria-label="Navigation mobile"
          >
            <div className="mx-auto max-w-[1440px]">
              {navigation.map((item) => (
                <a
                  key={item.label}
                  href={item.href}
                  className="block border-b border-slate-200 px-3 py-4 font-display text-xl font-semibold uppercase tracking-wide text-school-blue"
                  onClick={(event) => {
                    setMobileOpen(false);
                    if (event.currentTarget.getAttribute("href") === "#accueil") {
                      document.getElementById("main-content")?.focus();
                    }
                  }}
                >
                  {item.label}
                </a>
              ))}

              <a
                href={accountHref}
                className="mt-4 flex items-center gap-3 bg-brand-yellow px-4 py-3 font-display text-lg font-semibold uppercase text-black"
                onClick={() => setMobileOpen(false)}
              >
                <CircleUserRound size={24} aria-hidden="true" />
                {accountLabel}
              </a>
            </div>
          </nav>
        )}
      </header>
    </>
  );
}
