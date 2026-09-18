import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ArrowLeft,
  CalendarDays,
  Download,
  FileText,
  GraduationCap,
  LoaderCircle,
  LogOut,
  RefreshCw,
  School,
  ShieldAlert,
  UserRound,
} from "lucide-react";

import {
  Link,
  useNavigate,
} from "react-router";

import {
  calculateBulletin,
  downloadBulletinPdf,
  getPeriodes,
  getScolarites,
} from "../api/bulletinsApi";

import { getEleves } from "../api/elevesApi";
import { listResource } from "../api/schoolApi";
import { ApiError } from "../api/http";
import { useAuth } from "../context/authContextCore";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

function toArray(response) {
  if (Array.isArray(response)) {
    return response;
  }

  if (Array.isArray(response?.content)) {
    return response.content;
  }

  return [];
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "Non renseignée";
  }

  const date =
    new Date(`${dateValue}T00:00:00`);

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

function formatDecimal(value) {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return "—";
  }

  const numericValue = Number(value);

  if (!Number.isFinite(numericValue)) {
    return String(value);
  }

  return new Intl.NumberFormat(
    "fr-FR",
    {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    },
  ).format(numericValue);
}

function buildDateRange(
  dateDebut,
  dateFin,
) {
  if (!dateDebut && !dateFin) {
    return "Dates non renseignées";
  }

  if (!dateFin) {
    return `Depuis le ${formatDate(dateDebut)}`;
  }

  return (
    `Du ${formatDate(dateDebut)}` +
    ` au ${formatDate(dateFin)}`
  );
}

function slugify(value) {
  const slug = String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();

  return slug || "bulletin";
}

function getRequestErrorMessage(
  requestError,
  forbiddenMessage,
) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 403) {
      return forbiddenMessage;
    }

    return requestError.message;
  }

  return (
    "Impossible de joindre le serveur. " +
    "Vérifiez que le backend est démarré."
  );
}

export default function BulletinsPage() {
  const navigate = useNavigate();

  const {
    auth,
    logout,
  } = useAuth();

  const [eleves, setEleves] =
    useState([]);

  const [scolarites, setScolarites] =
    useState([]);

  const [periodes, setPeriodes] =
    useState([]);

  const [
    selectedScolariteId,
    setSelectedScolariteId,
  ] = useState("");

  const [
    selectedPeriodeId,
    setSelectedPeriodeId,
  ] = useState("");

  const [bulletin, setBulletin] =
    useState(null);

  const [loadingOptions, setLoadingOptions] =
    useState(true);

  const [calculating, setCalculating] =
    useState(false);

  const [downloading, setDownloading] =
    useState(false);

  const [optionsError, setOptionsError] =
    useState("");

  const [bulletinError, setBulletinError] =
    useState("");

  const [reloadKey, setReloadKey] =
    useState(0);
  const [classes, setClasses] = useState([]);

  const roleLabel =
    roleLabels[auth?.role] ??
    auth?.role ??
    "Utilisateur";

  useEffect(() => {
    const controller =
      new AbortController();

    async function loadOptions() {
      setLoadingOptions(true);
      setOptionsError("");
      setBulletin(null);
      setBulletinError("");

      try {
        const [
          elevesResponse,
          scolaritesResponse,
          periodesResponse,
          classesResponse,
        ] = await Promise.all([
          getEleves({
            signal: controller.signal,
          }),

          getScolarites({
            signal: controller.signal,
          }),

          getPeriodes({
            signal: controller.signal,
          }),
          listResource("classes", { signal: controller.signal }),
        ]);

        const loadedEleves =
          toArray(elevesResponse);

        const loadedScolarites =
          toArray(scolaritesResponse);

        const loadedPeriodes =
          toArray(periodesResponse)
            .sort((first, second) => {
              return String(
                first.dateDebut ?? "",
              ).localeCompare(
                String(second.dateDebut ?? ""),
              );
            });

        setEleves(loadedEleves);
        setScolarites(loadedScolarites);
        setPeriodes(loadedPeriodes);
        setClasses(toArray(classesResponse));

        setSelectedScolariteId(
          loadedScolarites[0]?.idScolarite
            ? String(
                loadedScolarites[0]
                  .idScolarite,
              )
            : "",
        );

        setSelectedPeriodeId(
          loadedPeriodes[0]?.idPeriode
            ? String(
                loadedPeriodes[0]
                  .idPeriode,
              )
            : "",
        );
      } catch (requestError) {
        if (
          requestError?.name ===
          "AbortError"
        ) {
          return;
        }

        setOptionsError(
          getRequestErrorMessage(
            requestError,
            "Votre rôle ne permet pas de charger les données nécessaires aux bulletins.",
          ),
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoadingOptions(false);
        }
      }
    }

    loadOptions();

    return () => {
      controller.abort();
    };
  }, [reloadKey]);

  const scolariteChoices =
    useMemo(() => {
      const elevesById =
        new Map(
          eleves.map((eleve) => [
            String(eleve.idPersonne),
            eleve,
          ]),
        );

      return scolarites
        .map((scolarite) => ({
          scolarite,
          eleve: elevesById.get(
            String(scolarite.idEleve),
          ),
        }))
        .sort((first, second) => {
          const firstName =
            `${first.eleve?.nom ?? ""} ` +
            `${first.eleve?.prenom ?? ""}`;

          const secondName =
            `${second.eleve?.nom ?? ""} ` +
            `${second.eleve?.prenom ?? ""}`;

          return firstName.localeCompare(
            secondName,
            "fr",
            {
              sensitivity: "base",
            },
          );
        });
    }, [eleves, scolarites]);

  const bulletinLines =
    Array.isArray(bulletin?.lignes)
      ? bulletin.lignes
      : [];

  function clearCurrentBulletin() {
    setBulletin(null);
    setBulletinError("");
  }

  function handleScolariteChange(event) {
    setSelectedScolariteId(
      event.target.value,
    );

    clearCurrentBulletin();
  }

  function handlePeriodeChange(event) {
    setSelectedPeriodeId(
      event.target.value,
    );

    clearCurrentBulletin();
  }

  async function handleCalculate(event) {
    event.preventDefault();

    if (
      !selectedScolariteId ||
      !selectedPeriodeId
    ) {
      setBulletinError(
        "Sélectionnez un élève et une période.",
      );

      return;
    }

    setCalculating(true);
    setBulletin(null);
    setBulletinError("");

    try {
      const calculatedBulletin =
        await calculateBulletin(
          selectedScolariteId,
          selectedPeriodeId,
        );

      setBulletin(calculatedBulletin);
    } catch (requestError) {
      setBulletinError(
        getRequestErrorMessage(
          requestError,
          "Votre rôle ne permet pas de consulter ce bulletin.",
        ),
      );
    } finally {
      setCalculating(false);
    }
  }

  async function handleDownload() {
    if (!bulletin) {
      return;
    }

    setDownloading(true);
    setBulletinError("");

    try {
      const pdfBlob =
        await downloadBulletinPdf(
          bulletin.idScolarite,
          bulletin.idPeriode,
        );

      const fileUrl =
        window.URL.createObjectURL(
          pdfBlob,
        );

      const downloadLink =
        document.createElement("a");

      const studentPart =
        slugify(
          bulletin.matriculeEleve ||
          `${bulletin.prenomEleve}-${bulletin.nomEleve}`,
        );

      const periodPart =
        slugify(
          bulletin.libellePeriode,
        );

      downloadLink.href = fileUrl;
      downloadLink.download =
        `bulletin-${studentPart}-${periodPart}.pdf`;

      document.body.appendChild(
        downloadLink,
      );

      downloadLink.click();
      downloadLink.remove();

      window.setTimeout(() => {
        window.URL.revokeObjectURL(
          fileUrl,
        );
      }, 1000);
    } catch (requestError) {
      setBulletinError(
        getRequestErrorMessage(
          requestError,
          "Votre rôle ne permet pas de télécharger ce bulletin.",
        ),
      );
    } finally {
      setDownloading(false);
    }
  }

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
                Module bulletins
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
              className="inline-flex items-center gap-2 border border-white/25 px-4 py-3 font-semibold transition hover:bg-white hover:text-school-blue-dark"
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
          Retour à l'espace sécurisé
        </Link>

        <section className="mt-7 grid gap-7 bg-white p-7 shadow-[0_18px_50px_rgba(23,23,23,0.06)] lg:grid-cols-[1fr_auto] lg:p-10">
          <div>
            <p className="font-display text-lg font-semibold uppercase tracking-[0.18em] text-school-blue">
              Résultats scolaires
            </p>

            <h1 className="mt-3 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
              Consulter un bulletin
            </h1>

            <p className="mt-6 max-w-3xl leading-7 text-school-muted">
              Sélectionnez la scolarité d'un élève
              et une période pour calculer ses
              moyennes et générer son bulletin PDF.
            </p>
          </div>

          <div className="flex min-w-56 items-center gap-4 bg-school-blue p-6 text-white">
            <FileText
              size={38}
              className="text-brand-yellow"
            />

            <div>
              <p className="text-sm uppercase tracking-wider text-white/60">
                Calcul
              </p>

              <p className="mt-1 font-bold">
                Résultats sur 20
              </p>
            </div>
          </div>
        </section>

        <section className="mt-8 border border-slate-200 bg-white p-6 lg:p-8">
          <div className="flex flex-wrap items-start justify-between gap-5">
            <div>
              <h2 className="font-display text-3xl font-bold uppercase text-school-blue">
                Sélection
              </h2>

              <p className="mt-2 text-school-muted">
                Choisissez un élève et une période
                pour consulter son bulletin.
              </p>
            </div>

            <button
              type="button"
              onClick={() =>
                setReloadKey(
                  (current) => current + 1,
                )
              }
              disabled={loadingOptions}
              className="inline-flex items-center gap-2 border border-slate-300 px-4 py-3 font-semibold text-school-blue transition hover:border-school-blue disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw
                size={18}
                className={
                  loadingOptions
                    ? "animate-spin"
                    : ""
                }
              />
              Actualiser
            </button>
          </div>

          {optionsError && (
            <div
              role="alert"
              className="mt-6 flex gap-3 border border-red-200 bg-red-50 p-4 text-red-800"
            >
              <ShieldAlert
                size={22}
                className="shrink-0"
              />

              <p>{optionsError}</p>
            </div>
          )}

          <form
            onSubmit={handleCalculate}
            className="mt-7 grid gap-5 lg:grid-cols-[1fr_1fr_auto]"
          >
            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Élève et scolarité
              </span>

              <select
                value={selectedScolariteId}
                onChange={
                  handleScolariteChange
                }
                disabled={
                  loadingOptions ||
                  scolariteChoices.length === 0
                }
                className="min-h-13 w-full border border-slate-300 bg-white px-4 py-3 text-school-ink outline-none transition focus:border-school-blue disabled:cursor-not-allowed disabled:bg-slate-100"
              >
                {scolariteChoices.length === 0 && (
                  <option value="">
                    Aucune scolarité disponible
                  </option>
                )}

                {scolariteChoices.map(
                  ({
                    scolarite,
                    eleve,
                  }) => (
                    <option
                      key={
                        scolarite.idScolarite
                      }
                      value={
                        scolarite.idScolarite
                      }
                    >
                      {eleve
                        ? `${eleve.prenom} ${eleve.nom} — ${eleve.matricule}`
                        : "Élève indisponible"}
                      {` — ${classes.find((item) => item.idClasse === scolarite.idClasse)?.nom ?? "Classe indisponible"} (${classes.find((item) => item.idClasse === scolarite.idClasse)?.anneeScolaire ?? scolarite.dateDebut})`}
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Période
              </span>

              <select
                value={selectedPeriodeId}
                onChange={
                  handlePeriodeChange
                }
                disabled={
                  loadingOptions ||
                  periodes.length === 0
                }
                className="min-h-13 w-full border border-slate-300 bg-white px-4 py-3 text-school-ink outline-none transition focus:border-school-blue disabled:cursor-not-allowed disabled:bg-slate-100"
              >
                {periodes.length === 0 && (
                  <option value="">
                    Aucune période disponible
                  </option>
                )}

                {periodes.map((periode) => (
                  <option
                    key={periode.idPeriode}
                    value={periode.idPeriode}
                  >
                    {periode.libelle}
                    {` — ${buildDateRange(
                      periode.dateDebut,
                      periode.dateFin,
                    )}`}
                  </option>
                ))}
              </select>
            </label>

            <button
              type="submit"
              disabled={
                loadingOptions ||
                calculating ||
                !selectedScolariteId ||
                !selectedPeriodeId
              }
              className="inline-flex min-h-13 items-center justify-center gap-2 self-end bg-brand-yellow px-6 py-3 font-display text-lg font-bold uppercase text-black transition hover:bg-yellow-300 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {calculating ? (
                <LoaderCircle
                  size={20}
                  className="animate-spin"
                />
              ) : (
                <FileText size={20} />
              )}

              {calculating
                ? "Calcul en cours"
                : "Calculer"}
            </button>
          </form>
        </section>

        {bulletinError && (
          <div
            role="alert"
            aria-live="polite"
            className="mt-7 flex gap-3 border border-red-200 bg-red-50 p-5 text-red-800"
          >
            <ShieldAlert
              size={23}
              className="shrink-0"
            />

            <p>{bulletinError}</p>
          </div>
        )}

        {calculating && (
          <div
            aria-live="polite"
            className="mt-7 flex items-center justify-center gap-3 border border-slate-200 bg-white p-10 text-school-blue"
          >
            <LoaderCircle
              size={27}
              className="animate-spin"
            />

            <p className="font-semibold">
              Calcul du bulletin…
            </p>
          </div>
        )}

        {bulletin && !calculating && (
          <section className="mt-8">
            <div className="flex flex-wrap items-center justify-between gap-5 bg-school-blue-dark p-6 text-white lg:p-8">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.18em] text-brand-yellow">
                  Bulletin calculé
                </p>

                <h2 className="mt-2 font-display text-4xl font-bold uppercase">
                  {bulletin.prenomEleve}
                  {` ${bulletin.nomEleve}`}
                </h2>

                <p className="mt-2 text-white/65">
                  Matricule :
                  {` ${bulletin.matriculeEleve ?? "—"}`}
                </p>
              </div>

              <button
                type="button"
                onClick={handleDownload}
                disabled={downloading}
                className="inline-flex items-center gap-2 bg-brand-yellow px-5 py-4 font-display text-lg font-bold uppercase text-black transition hover:bg-yellow-300 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {downloading ? (
                  <LoaderCircle
                    size={21}
                    className="animate-spin"
                  />
                ) : (
                  <Download size={21} />
                )}

                {downloading
                  ? "Téléchargement"
                  : "Télécharger le PDF"}
              </button>
            </div>

            <div className="grid gap-4 bg-white p-6 sm:grid-cols-2 lg:grid-cols-4 lg:p-8">
              <article className="border border-slate-200 p-5">
                <UserRound
                  size={25}
                  className="text-school-blue"
                />

                <p className="mt-4 text-xs font-bold uppercase tracking-wider text-school-muted">
                  Élève
                </p>

                <p className="mt-2 font-bold text-school-ink">
                  {bulletin.prenomEleve}
                  {` ${bulletin.nomEleve}`}
                </p>
              </article>

              <article className="border border-slate-200 p-5">
                <School
                  size={25}
                  className="text-school-blue"
                />

                <p className="mt-4 text-xs font-bold uppercase tracking-wider text-school-muted">
                  Classe
                </p>

                <p className="mt-2 font-bold text-school-ink">
                  {bulletin.nomClasse ?? "—"}
                </p>

                <p className="mt-1 text-sm text-school-muted">
                  {bulletin.niveauClasse ?? "—"}
                  {bulletin.anneeScolaire
                    ? ` — ${bulletin.anneeScolaire}`
                    : ""}
                </p>
              </article>

              <article className="border border-slate-200 p-5">
                <CalendarDays
                  size={25}
                  className="text-school-blue"
                />

                <p className="mt-4 text-xs font-bold uppercase tracking-wider text-school-muted">
                  Période
                </p>

                <p className="mt-2 font-bold text-school-ink">
                  {bulletin.libellePeriode ?? "—"}
                </p>

                <p className="mt-1 text-sm text-school-muted">
                  {buildDateRange(
                    bulletin.dateDebutPeriode,
                    bulletin.dateFinPeriode,
                  )}
                </p>
              </article>

              <article className="bg-brand-yellow p-5 text-black">
                <GraduationCap size={27} />

                <p className="mt-4 text-xs font-bold uppercase tracking-wider">
                  Moyenne générale
                </p>

                <p className="mt-2 font-display text-4xl font-bold">
                  {bulletin.moyenneGenerale === null
                    ? "Non calculable"
                    : `${formatDecimal(
                        bulletin.moyenneGenerale,
                      )} / 20`}
                </p>
              </article>
            </div>

            <div className="border-t border-slate-200 bg-white p-6 lg:p-8">
              <div className="flex flex-wrap items-end justify-between gap-4">
                <div>
                  <h3 className="font-display text-3xl font-bold uppercase text-school-blue">
                    Résultats par matière
                  </h3>

                  <p className="mt-2 text-school-muted">
                    {bulletinLines.length}
                    {bulletinLines.length > 1
                      ? " matières"
                      : " matière"}
                    {" affichée"}
                    {bulletinLines.length > 1
                      ? "s"
                      : ""}
                    .
                  </p>
                </div>
              </div>

              {bulletinLines.length === 0 ? (
                <div className="mt-6 border border-dashed border-slate-300 p-8 text-center text-school-muted">
                  Aucun résultat disponible pour
                  cette période.
                </div>
              ) : (
                <>
                  <div className="mt-6 hidden overflow-x-auto md:block">
                    <table className="w-full border-collapse text-left">
                      <thead>
                        <tr className="bg-school-blue-dark text-white">
                          <th className="px-5 py-4">
                            Matière
                          </th>

                          <th className="px-5 py-4">
                            Code
                          </th>

                          <th className="px-5 py-4 text-center">
                            Coefficient
                          </th>

                          <th className="px-5 py-4 text-center">
                            Notes
                          </th>

                          <th className="px-5 py-4 text-right">
                            Moyenne
                          </th>
                        </tr>
                      </thead>

                      <tbody>
                        {bulletinLines.map(
                          (line) => (
                            <tr
                              key={
                                line.idEnseignement
                              }
                              className="border-b border-slate-200"
                            >
                              <td className="px-5 py-5 font-semibold text-school-ink">
                                {line.nomMatiere ??
                                  "—"}
                              </td>

                              <td className="px-5 py-5 text-school-muted">
                                {line.codeMatiere ??
                                  "—"}
                              </td>

                              <td className="px-5 py-5 text-center">
                                {formatDecimal(
                                  line.coefficientMatiere,
                                )}
                              </td>

                              <td className="px-5 py-5 text-center">
                                {line.nombreNotes}
                              </td>

                              <td className="px-5 py-5 text-right font-bold text-school-blue">
                                {line.moyenneSur20 ===
                                null
                                  ? "Non calculable"
                                  : `${formatDecimal(
                                      line.moyenneSur20,
                                    )} / 20`}
                              </td>
                            </tr>
                          ),
                        )}
                      </tbody>
                    </table>
                  </div>

                  <div className="mt-6 grid gap-4 md:hidden">
                    {bulletinLines.map(
                      (line) => (
                        <article
                          key={
                            line.idEnseignement
                          }
                          className="border border-slate-200 p-5"
                        >
                          <div className="flex items-start justify-between gap-4">
                            <div>
                              <p className="font-display text-2xl font-bold uppercase text-school-blue">
                                {line.nomMatiere ??
                                  "—"}
                              </p>

                              <p className="mt-1 text-sm text-school-muted">
                                Code :
                                {` ${line.codeMatiere ?? "—"}`}
                              </p>
                            </div>

                            <span className="bg-brand-yellow px-3 py-2 font-bold text-black">
                              {line.moyenneSur20 ===
                              null
                                ? "—"
                                : `${formatDecimal(
                                    line.moyenneSur20,
                                  )}/20`}
                            </span>
                          </div>

                          <dl className="mt-5 grid grid-cols-2 gap-4 border-t border-slate-200 pt-4">
                            <div>
                              <dt className="text-xs font-bold uppercase tracking-wider text-school-muted">
                                Coefficient
                              </dt>

                              <dd className="mt-1 font-semibold">
                                {formatDecimal(
                                  line.coefficientMatiere,
                                )}
                              </dd>
                            </div>

                            <div>
                              <dt className="text-xs font-bold uppercase tracking-wider text-school-muted">
                                Nombre de notes
                              </dt>

                              <dd className="mt-1 font-semibold">
                                {line.nombreNotes}
                              </dd>
                            </div>
                          </dl>
                        </article>
                      ),
                    )}
                  </div>
                </>
              )}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
