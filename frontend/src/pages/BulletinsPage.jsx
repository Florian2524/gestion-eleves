import ConnectedHeader from "../components/layout/ConnectedHeader";
import {
  useEffect,
  useMemo,
  useState,
} from "react";

import { Download, FileText, RefreshCw } from "lucide-react";
import Button from "../components/ui/Button";
import Panel from "../components/ui/Panel";
import StatusMessage from "../components/ui/StatusMessage";
import { lightControlClass } from "../components/ui/controlStyles";

import {
  calculateBulletin,
  downloadBulletinPdf,
  getPeriodes,
  getScolarites,
} from "../api/bulletinsApi";

import { getEleves } from "../api/elevesApi";
import { listResource } from "../api/schoolApi";
import { ApiError } from "../api/http";
import { schoolYearLabel } from "../components/school/schoolYear";

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

  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-[1440px] px-4 py-8 sm:px-6 lg:px-10">
        <div className="mb-6 border-l-4 border-brand-yellow pl-4">
          <p className="text-sm font-bold uppercase tracking-widest text-school-blue">Résultats scolaires</p>
          <h1 className="mt-1 font-display text-3xl font-bold uppercase text-school-blue-dark sm:text-4xl">Consulter un bulletin</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-school-muted">Sélectionnez la scolarité d’un élève et une période pour consulter ses moyennes sur 20, puis télécharger son bulletin PDF.</p>
        </div>
        <Panel aria-labelledby="bulletin-selection-title" className="p-4 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0">
              <h2 id="bulletin-selection-title" className="font-display text-2xl font-semibold text-school-blue-dark">Sélection du bulletin</h2>
              <p className="mt-2 text-sm leading-6 text-school-muted">Vérifiez la classe, l’année scolaire et la période avant de lancer le calcul.</p>
            </div>
            <Button variant="secondary" onClick={() => setReloadKey((current) => current + 1)} disabled={loadingOptions} loading={loadingOptions}><RefreshCw size={18} aria-hidden="true" />Actualiser</Button>
          </div>
          {loadingOptions && <StatusMessage className="mt-4">Chargement des élèves et des périodes…</StatusMessage>}
          {optionsError && <StatusMessage variant="error" className="mt-4">{optionsError}</StatusMessage>}
          {!loadingOptions && !optionsError && (scolariteChoices.length === 0 || periodes.length === 0) && <StatusMessage className="mt-4">{scolariteChoices.length === 0 ? "Aucune scolarité disponible pour votre profil. " : ""}{periodes.length === 0 ? "Aucune période disponible. " : ""}Le calcul nécessite une scolarité et une période.</StatusMessage>}
          <form onSubmit={handleCalculate} aria-label="Sélection du bulletin" className="mt-5 grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto]">
            <div className="min-w-0">
              <label htmlFor="bulletin-scolarite" className="block text-sm font-semibold">Élève et scolarité</label>
              <select id="bulletin-scolarite" value={selectedScolariteId} onChange={handleScolariteChange} disabled={loadingOptions || scolariteChoices.length === 0} aria-describedby="bulletin-scolarite-hint" className={lightControlClass}>
                {scolariteChoices.length === 0 && <option value="">Aucune scolarité disponible</option>}
                {scolariteChoices.map(({ scolarite, eleve }) => (
                  <option key={scolarite.idScolarite} value={scolarite.idScolarite}>
                    {eleve ? `${eleve.prenom} ${eleve.nom} — ${eleve.matricule}` : "Élève indisponible"}
                    {` — ${classes.find((item) => item.idClasse === scolarite.idClasse)?.nom ?? "Classe indisponible"} (${classes.find((item) => item.idClasse === scolarite.idClasse)?.anneeScolaire ?? scolarite.dateDebut})`}
                  </option>
                ))}
              </select>
              <p id="bulletin-scolarite-hint" className="mt-2 text-sm leading-6 text-school-muted">Un élève peut avoir plusieurs scolarités : choisissez la classe et l’année concernées.</p>
            </div>
            <div className="min-w-0">
              <label htmlFor="bulletin-periode" className="block text-sm font-semibold">Période</label>
              <select id="bulletin-periode" value={selectedPeriodeId} onChange={handlePeriodeChange} disabled={loadingOptions || periodes.length === 0} aria-describedby="bulletin-periode-hint" className={lightControlClass}>
                {periodes.length === 0 && <option value="">Aucune période disponible</option>}
                {periodes.map((periode) => <option key={periode.idPeriode} value={periode.idPeriode}>{periode.libelle}{` · ${schoolYearLabel(periode.anneeScolaire)}`}{` — ${buildDateRange(periode.dateDebut, periode.dateFin)}`}</option>)}
              </select>
              <p id="bulletin-periode-hint" className="mt-2 text-sm leading-6 text-school-muted">La période détermine les résultats inclus dans le bulletin.</p>
            </div>
            <Button type="submit" loading={calculating} disabled={loadingOptions || calculating || !selectedScolariteId || !selectedPeriodeId} className="lg:mt-7 lg:self-start"><FileText size={20} aria-hidden="true" />{calculating ? "Calcul en cours" : "Calculer"}</Button>
          </form>
        </Panel>
        {bulletinError && <StatusMessage variant="error" className="mt-5">{bulletinError}</StatusMessage>}
        {calculating && <StatusMessage className="mt-5">Calcul du bulletin…</StatusMessage>}
        {bulletin && !calculating && (
          <Panel aria-labelledby="bulletin-result-title" className="mt-6 min-w-0 overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-5 bg-school-blue-dark p-4 text-white sm:p-6">
              <div className="min-w-0">
                <p className="text-sm font-bold uppercase tracking-widest text-brand-yellow">Bulletin calculé</p>
                <h2 id="bulletin-result-title" className="mt-2 break-words font-display text-2xl font-semibold sm:text-3xl">{bulletin.prenomEleve}{` ${bulletin.nomEleve}`}</h2>
                <p className="mt-2 break-words text-sm text-slate-200">Matricule :{` ${bulletin.matriculeEleve ?? "—"}`}</p>
              </div>
              <Button onClick={handleDownload} loading={downloading} disabled={downloading}><Download size={19} aria-hidden="true" />{downloading ? "Téléchargement" : "Télécharger le PDF"}</Button>
            </div>
            <div className="grid min-w-0 gap-4 p-4 sm:grid-cols-2 sm:p-6 lg:grid-cols-3">
              <div className="min-w-0 border border-slate-200 p-4">
                <h3 className="text-sm font-semibold text-school-muted">Classe et année scolaire</h3>
                <p className="mt-2 break-words font-semibold text-school-blue-dark">{bulletin.nomClasse ?? "—"}</p>
                <p className="mt-1 break-words text-sm text-school-muted">{bulletin.niveauClasse ?? "—"}{bulletin.anneeScolaire ? ` — ${bulletin.anneeScolaire}` : ""}</p>
              </div>
              <div className="min-w-0 border border-slate-200 p-4">
                <h3 className="text-sm font-semibold text-school-muted">Période</h3>
                <p className="mt-2 break-words font-semibold text-school-blue-dark">{bulletin.libellePeriode ?? "—"}</p>
                <p className="mt-1 text-sm leading-6 text-school-muted">{buildDateRange(bulletin.dateDebutPeriode, bulletin.dateFinPeriode)}</p>
              </div>
              <div className="min-w-0 bg-brand-yellow p-4 text-black sm:col-span-2 lg:col-span-1">
                <h3 className="text-sm font-bold">Moyenne générale</h3>
                <p className="mt-2 break-words font-display text-3xl font-semibold">{bulletin.moyenneGenerale === null ? "Non calculable" : `${formatDecimal(bulletin.moyenneGenerale)} / 20`}</p>
              </div>
            </div>
            <div className="min-w-0 border-t border-slate-200 p-4 sm:p-6">
              <div className="flex flex-wrap items-baseline justify-between gap-3">
                <h3 className="font-display text-2xl font-semibold text-school-blue-dark">Résultats par matière</h3>
                <p className="text-sm text-school-muted">{bulletinLines.length}{bulletinLines.length > 1 ? " matières affichées." : " matière affichée."}</p>
              </div>
              {bulletinLines.length === 0 ? <p className="mt-4 border border-dashed border-slate-400 p-5 text-center text-sm text-school-muted">Aucun résultat disponible pour cette période.</p> : (
                <>
                  <div className="mt-4 hidden overflow-x-auto md:block" role="region" aria-label="Tableau des résultats par matière" tabIndex={0}>
                    <table className="w-full min-w-[640px] border-collapse text-left text-sm">
                      <caption className="sr-only">Moyennes sur 20, coefficients et nombre de notes par matière</caption>
                      <thead className="bg-school-blue text-white"><tr>
                        <th scope="col" className="px-4 py-3">Matière</th><th scope="col" className="px-4 py-3">Code</th><th scope="col" className="px-4 py-3 text-center">Coefficient</th><th scope="col" className="px-4 py-3 text-center">Nombre de notes</th><th scope="col" className="px-4 py-3 text-right">Moyenne sur 20</th>
                      </tr></thead>
                      <tbody>{bulletinLines.map((line) => (
                        <tr key={line.idMatiere} className="border-b border-slate-200 transition-colors last:border-0 hover:bg-school-blue-light/25">
                          <th scope="row" className="max-w-64 break-words px-4 py-4 font-semibold text-school-blue-dark">{line.nomMatiere ?? "—"}</th>
                          <td className="max-w-40 break-words px-4 py-4 text-school-muted">{line.codeMatiere ?? "—"}</td>
                          <td className="px-4 py-4 text-center">{formatDecimal(line.coefficientMatiere)}</td><td className="px-4 py-4 text-center">{line.nombreNotes}</td>
                          <td className="px-4 py-4 text-right font-bold text-school-blue">{line.moyenneSur20 === null ? "Non calculable" : `${formatDecimal(line.moyenneSur20)} / 20`}</td>
                        </tr>
                      ))}</tbody>
                    </table>
                  </div>
                  <div className="mt-4 grid min-w-0 gap-4 md:hidden" role="region" aria-label="Cartes des résultats par matière">
                    {bulletinLines.map((line) => (
                      <article key={line.idMatiere} className="min-w-0 border border-slate-200 p-4">
                        <h4 className="break-words font-display text-xl font-semibold text-school-blue-dark">{line.nomMatiere ?? "—"}</h4>
                        <p className="mt-1 break-words text-sm text-school-muted">Code :{` ${line.codeMatiere ?? "—"}`}</p>
                        <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-200 pt-3 text-sm">
                          <div className="col-span-2 bg-brand-yellow p-3 text-black"><dt className="font-semibold">Moyenne sur 20</dt><dd className="mt-1 font-bold">{line.moyenneSur20 === null ? "Non calculable" : `${formatDecimal(line.moyenneSur20)} / 20`}</dd></div>
                          <div><dt className="font-semibold text-school-muted">Coefficient</dt><dd className="mt-1">{formatDecimal(line.coefficientMatiere)}</dd></div>
                          <div><dt className="font-semibold text-school-muted">Nombre de notes</dt><dd className="mt-1">{line.nombreNotes}</dd></div>
                        </dl>
                      </article>
                    ))}
                  </div>
                </>
              )}
            </div>
          </Panel>
        )}
      </main>
    </div>
  );
}
