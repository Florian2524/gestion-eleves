import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ArrowLeft,
  CalendarDays,
  GraduationCap,
  LogOut,
  Mail,
  RefreshCw,
  Search,
  ShieldAlert,
  UserRound,
  Users,
} from "lucide-react";

import {
  Link,
  useNavigate,
} from "react-router";

import { getEleves } from "../api/elevesApi";
import { ApiError } from "../api/http";
import { useAuth } from "../context/authContextCore";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "Non renseignée";
  }

  const date = new Date(
    `${dateValue}T00:00:00`,
  );

  if (Number.isNaN(date.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat(
    "fr-FR",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    },
  ).format(date);
}

function getInitials(eleve) {
  const prenomInitial =
    eleve?.prenom?.trim()?.charAt(0) ?? "";

  const nomInitial =
    eleve?.nom?.trim()?.charAt(0) ?? "";

  return (
    `${prenomInitial}${nomInitial}`
      .toUpperCase() || "É"
  );
}

export default function ElevesPage() {
  const navigate = useNavigate();

  const {
    auth,
    logout,
  } = useAuth();

  const [eleves, setEleves] =
    useState([]);

  const [search, setSearch] =
    useState("");

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [reloadKey, setReloadKey] =
    useState(0);

  const roleLabel =
    roleLabels[auth?.role] ??
    auth?.role ??
    "Utilisateur";

  useEffect(() => {
    const controller =
      new AbortController();

    async function loadEleves() {
      setLoading(true);
      setError("");

      try {
        const response =
          await getEleves({
            signal: controller.signal,
          });

        const responseEleves =
          Array.isArray(response)
            ? response
            : Array.isArray(response?.content)
              ? response.content
              : [];

        const sortedEleves = [
          ...responseEleves,
        ].sort((firstEleve, secondEleve) => {
          const firstName =
            `${firstEleve.nom ?? ""} ${firstEleve.prenom ?? ""}`;

          const secondName =
            `${secondEleve.nom ?? ""} ${secondEleve.prenom ?? ""}`;

          return firstName.localeCompare(
            secondName,
            "fr",
            {
              sensitivity: "base",
            },
          );
        });

        setEleves(sortedEleves);
      } catch (requestError) {
        if (
          requestError?.name === "AbortError"
        ) {
          return;
        }

        if (requestError instanceof ApiError) {
          if (requestError.status === 403) {
            setError(
              "Votre rôle ne permet pas de consulter la liste des élèves.",
            );
          } else {
            setError(requestError.message);
          }
        } else {
          setError(
            "Impossible de joindre le serveur. Vérifiez que le backend est démarré.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadEleves();

    return () => {
      controller.abort();
    };
  }, [reloadKey]);

  const filteredEleves = useMemo(() => {
    const normalizedSearch =
      normalizeText(search);

    if (!normalizedSearch) {
      return eleves;
    }

    return eleves.filter((eleve) => {
      const searchableContent =
        normalizeText(
          [
            eleve.nom,
            eleve.prenom,
            eleve.matricule,
            eleve.emailContact,
            eleve.telephone,
          ].join(" "),
        );

      return searchableContent.includes(
        normalizedSearch,
      );
    });
  }, [eleves, search]);

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
            to="/espace"
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
                Gestion des élèves
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

            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex min-h-11 items-center gap-2 border border-white/25 px-4 font-semibold transition hover:border-brand-yellow hover:text-brand-yellow"
            >
              <LogOut size={18} />
              Déconnexion
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1440px] px-4 py-8 sm:px-6 lg:px-10 lg:py-12">
        <Link
          to="/espace"
          className="inline-flex items-center gap-2 font-semibold text-school-blue transition hover:text-school-blue-dark"
        >
          <ArrowLeft size={18} />
          Retour à l’espace sécurisé
        </Link>

        <section className="mt-7 bg-white p-6 shadow-[0_18px_50px_rgba(23,23,23,0.06)] sm:p-8 lg:p-10">
          <div className="flex flex-col justify-between gap-7 lg:flex-row lg:items-end">
            <div>
              <p className="font-display text-base font-semibold uppercase tracking-[0.18em] text-school-blue">
                Dossiers scolaires
              </p>

              <h1 className="mt-2 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
                Liste des élèves
              </h1>

              <p className="mt-5 max-w-2xl leading-7 text-school-muted">
                Consultez les élèves enregistrés dans
                l’établissement et recherchez un dossier
                par identité, matricule ou coordonnées.
              </p>
            </div>

            <div className="flex items-center gap-4 bg-school-blue-light/50 px-5 py-4">
              <Users
                size={30}
                className="text-school-blue"
              />

              <div>
                <p className="font-display text-3xl font-bold text-school-blue">
                  {loading
                    ? "—"
                    : eleves.length}
                </p>

                <p className="text-sm text-school-muted">
                  élèves enregistrés
                </p>
              </div>
            </div>
          </div>

          <div className="mt-9 flex flex-col gap-4 border-t border-slate-200 pt-7 sm:flex-row sm:items-center sm:justify-between">
            <label className="relative block w-full max-w-xl">
              <span className="sr-only">
                Rechercher un élève
              </span>

              <Search
                size={20}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
              />

              <input
                type="search"
                value={search}
                onChange={(event) =>
                  setSearch(event.target.value)
                }
                placeholder="Nom, prénom, matricule ou contact..."
                className="min-h-12 w-full border border-slate-300 bg-white pl-12 pr-4 text-school-ink outline-none transition placeholder:text-slate-400 focus:border-school-blue focus:ring-2 focus:ring-school-blue/15"
              />
            </label>

            {!loading && !error && (
              <p className="shrink-0 text-sm font-semibold text-school-muted">
                {filteredEleves.length}
                {" "}
                résultat
                {filteredEleves.length > 1
                  ? "s"
                  : ""}
              </p>
            )}
          </div>
        </section>

        {loading && (
          <section
            aria-live="polite"
            className="mt-7 grid min-h-64 place-items-center bg-white p-8"
          >
            <div className="text-center">
              <RefreshCw
                size={38}
                className="mx-auto animate-spin text-school-blue"
              />

              <p className="mt-5 font-semibold text-school-muted">
                Chargement des élèves...
              </p>
            </div>
          </section>
        )}

        {!loading && error && (
          <section
            role="alert"
            className="mt-7 flex flex-col items-start gap-5 border border-red-200 bg-red-50 p-7 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start gap-4">
              <ShieldAlert
                size={28}
                className="shrink-0 text-red-600"
              />

              <div>
                <h2 className="font-display text-2xl font-bold uppercase text-red-800">
                  Chargement impossible
                </h2>

                <p className="mt-2 text-red-700">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() =>
                setReloadKey(
                  (currentKey) =>
                    currentKey + 1,
                )
              }
              className="inline-flex min-h-11 items-center gap-2 bg-red-700 px-5 font-semibold text-white transition hover:bg-red-800"
            >
              <RefreshCw size={18} />
              Réessayer
            </button>
          </section>
        )}

        {!loading &&
          !error &&
          filteredEleves.length === 0 && (
            <section className="mt-7 grid min-h-64 place-items-center bg-white p-8 text-center">
              <div>
                <UserRound
                  size={45}
                  className="mx-auto text-slate-300"
                />

                <h2 className="mt-5 font-display text-3xl font-bold uppercase text-school-blue">
                  Aucun élève trouvé
                </h2>

                <p className="mt-3 text-school-muted">
                  {search
                    ? "Aucun dossier ne correspond à votre recherche."
                    : "Aucun élève n’est encore enregistré."}
                </p>
              </div>
            </section>
          )}

        {!loading &&
          !error &&
          filteredEleves.length > 0 && (
            <>
              <section className="mt-7 hidden overflow-hidden bg-white shadow-[0_18px_50px_rgba(23,23,23,0.05)] md:block">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[850px] border-collapse text-left">
                    <thead className="bg-school-blue text-white">
                      <tr>
                        <th className="px-6 py-4 text-sm uppercase tracking-wider">
                          Élève
                        </th>

                        <th className="px-6 py-4 text-sm uppercase tracking-wider">
                          Matricule
                        </th>

                        <th className="px-6 py-4 text-sm uppercase tracking-wider">
                          Contact
                        </th>

                        <th className="px-6 py-4 text-sm uppercase tracking-wider">
                          Naissance
                        </th>
                      </tr>
                    </thead>

                    <tbody>
                      {filteredEleves.map(
                        (eleve) => (
                          <tr
                            key={eleve.idPersonne}
                            className="border-b border-slate-200 transition last:border-0 hover:bg-school-blue-light/25"
                          >
                            <td className="px-6 py-5">
                              <div className="flex items-center gap-4">
                                <span className="grid size-11 shrink-0 place-items-center bg-brand-yellow font-bold text-black">
                                  {getInitials(eleve)}
                                </span>

                                <div>
                                  <p className="font-semibold text-school-ink">
                                    {eleve.prenom}
                                    {" "}
                                    {eleve.nom}
                                  </p>

                                  <p className="mt-1 text-sm text-school-muted">
                                    {eleve.emailContact ??
                                      "Coordonnées non renseignées"}
                                  </p>
                                </div>
                              </div>
                            </td>

                            <td className="px-6 py-5 font-semibold text-school-blue">
                              {eleve.matricule ??
                                "Non renseigné"}
                            </td>

                            <td className="px-6 py-5">
                              <p className="text-school-ink">
                                {eleve.emailContact ??
                                  "Non renseigné"}
                              </p>

                              <p className="mt-1 text-sm text-school-muted">
                                {eleve.telephone ??
                                  "Téléphone non renseigné"}
                              </p>
                            </td>

                            <td className="px-6 py-5 text-school-muted">
                              {formatDate(
                                eleve.dateNaissance,
                              )}
                            </td>
                          </tr>
                        ),
                      )}
                    </tbody>
                  </table>
                </div>
              </section>

              <section className="mt-7 grid gap-4 md:hidden">
                {filteredEleves.map((eleve) => (
                  <article
                    key={eleve.idPersonne}
                    className="bg-white p-6 shadow-[0_12px_35px_rgba(23,23,23,0.05)]"
                  >
                    <div className="flex items-start gap-4">
                      <span className="grid size-12 shrink-0 place-items-center bg-brand-yellow font-bold text-black">
                        {getInitials(eleve)}
                      </span>

                      <div className="min-w-0">
                        <h2 className="font-display text-2xl font-bold uppercase text-school-blue">
                          {eleve.prenom}
                          {" "}
                          {eleve.nom}
                        </h2>

                        <p className="mt-1 text-sm font-semibold text-school-muted">
                          {eleve.matricule ??
                            "Matricule non renseigné"}
                        </p>
                      </div>
                    </div>

                    <dl className="mt-6 space-y-4 border-t border-slate-200 pt-5">
                      <div className="flex items-start gap-3">
                        <Mail
                          size={19}
                          className="mt-0.5 shrink-0 text-school-blue"
                        />

                        <div>
                          <dt className="text-xs font-bold uppercase tracking-wider text-school-muted">
                            Contact
                          </dt>

                          <dd className="mt-1 break-words text-school-ink">
                            {eleve.emailContact ??
                              "Non renseigné"}
                          </dd>
                        </div>
                      </div>

                      <div className="flex items-start gap-3">
                        <CalendarDays
                          size={19}
                          className="mt-0.5 shrink-0 text-school-blue"
                        />

                        <div>
                          <dt className="text-xs font-bold uppercase tracking-wider text-school-muted">
                            Date de naissance
                          </dt>

                          <dd className="mt-1 text-school-ink">
                            {formatDate(
                              eleve.dateNaissance,
                            )}
                          </dd>
                        </div>
                      </div>
                    </dl>
                  </article>
                ))}
              </section>
            </>
          )}
      </main>
    </div>
  );
}
