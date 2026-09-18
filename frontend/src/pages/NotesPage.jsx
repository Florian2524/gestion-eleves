import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ArrowLeft,
  CalendarDays,
  GraduationCap,
  LoaderCircle,
  LogOut,
  NotebookPen,
  Pencil,
  RefreshCw,
  Save,
  Search,
  ShieldAlert,
  Trash2,
  UserRound,
  X,
} from "lucide-react";

import {
  Link,
  useNavigate,
} from "react-router";

import {
  getScolarites,
} from "../api/bulletinsApi";

import { getEleves } from "../api/elevesApi";
import { ApiError } from "../api/http";

import {
  createNote,
  deleteNote,
  getEvaluations,
  getNotes,
  updateNote,
} from "../api/notesApi";

import { useAuth } from "../context/authContextCore";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

const initialForm = {
  idScolarite: "",
  idEvaluation: "",
  valeur: "",
  commentaire: "",
  statutNote: "SAISIE",
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

function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
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

function formatDate(dateValue) {
  if (!dateValue) {
    return "Date non renseignée";
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

function formatDateTime(dateValue) {
  if (!dateValue) {
    return "Non renseignée";
  }

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat(
    "fr-FR",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    },
  ).format(date);
}

function sortNotes(notes) {
  return [...notes].sort(
    (first, second) =>
      Number(second.idNote ?? 0) -
      Number(first.idNote ?? 0),
  );
}

function getErrorMessage(
  requestError,
  forbiddenMessage,
) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 403) {
      return forbiddenMessage;
    }

    if (requestError.status === 409) {
      return (
        requestError.message ||
        "Une note existe déjà pour cet élève et cette évaluation."
      );
    }

    return requestError.message;
  }

  return (
    "Impossible de joindre le serveur. " +
    "Vérifiez que le backend est démarré."
  );
}

export default function NotesPage() {
  const navigate = useNavigate();

  const {
    auth,
    logout,
  } = useAuth();

  const [eleves, setEleves] =
    useState([]);

  const [scolarites, setScolarites] =
    useState([]);

  const [evaluations, setEvaluations] =
    useState([]);

  const [notes, setNotes] =
    useState([]);

  const [form, setForm] =
    useState(initialForm);

  const [editingNoteId, setEditingNoteId] =
    useState(null);

  const [search, setSearch] =
    useState("");

  const [loading, setLoading] =
    useState(true);

  const [submitting, setSubmitting] =
    useState(false);

  const [deletingNoteId, setDeletingNoteId] =
    useState(null);

  const [loadError, setLoadError] =
    useState("");

  const [formError, setFormError] =
    useState("");

  const [successMessage, setSuccessMessage] =
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

    async function loadData() {
      setLoading(true);
      setLoadError("");

      try {
        const [
          elevesResponse,
          scolaritesResponse,
          evaluationsResponse,
          notesResponse,
        ] = await Promise.all([
          getEleves({
            signal: controller.signal,
          }),

          getScolarites({
            signal: controller.signal,
          }),

          getEvaluations({
            signal: controller.signal,
          }),

          getNotes({
            signal: controller.signal,
          }),
        ]);

        const loadedEleves =
          toArray(elevesResponse);

        const loadedScolarites =
          toArray(scolaritesResponse);

        const loadedEvaluations =
          toArray(evaluationsResponse)
            .sort((first, second) =>
              String(
                second.dateEvaluation ?? "",
              ).localeCompare(
                String(
                  first.dateEvaluation ?? "",
                ),
              ),
            );

        const loadedNotes =
          sortNotes(
            toArray(notesResponse),
          );

        setEleves(loadedEleves);
        setScolarites(loadedScolarites);
        setEvaluations(loadedEvaluations);
        setNotes(loadedNotes);

        setForm((current) => ({
          ...current,

          idScolarite:
            current.idScolarite ||
            String(
              loadedScolarites[0]
                ?.idScolarite ?? "",
            ),

          idEvaluation:
            current.idEvaluation ||
            String(
              loadedEvaluations[0]
                ?.idEvaluation ?? "",
            ),
        }));
      } catch (requestError) {
        if (
          requestError?.name ===
          "AbortError"
        ) {
          return;
        }

        setLoadError(
          getErrorMessage(
            requestError,
            "Votre rôle ne permet pas de consulter les notes.",
          ),
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadData();

    return () => {
      controller.abort();
    };
  }, [reloadKey]);

  const elevesById =
    useMemo(
      () =>
        new Map(
          eleves.map((eleve) => [
            String(eleve.idPersonne),
            eleve,
          ]),
        ),
      [eleves],
    );

  const scolaritesById =
    useMemo(
      () =>
        new Map(
          scolarites.map((scolarite) => [
            String(
              scolarite.idScolarite,
            ),
            scolarite,
          ]),
        ),
      [scolarites],
    );

  const evaluationsById =
    useMemo(
      () =>
        new Map(
          evaluations.map((evaluation) => [
            String(
              evaluation.idEvaluation,
            ),
            evaluation,
          ]),
        ),
      [evaluations],
    );

  const scolariteChoices =
    useMemo(
      () =>
        scolarites
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
          }),
      [
        elevesById,
        scolarites,
      ],
    );

  const selectedEvaluation =
    evaluationsById.get(
      String(form.idEvaluation),
    );

  const filteredNotes =
    useMemo(() => {
      const normalizedSearch =
        normalizeText(search);

      if (!normalizedSearch) {
        return notes;
      }

      return notes.filter((note) => {
        const scolarite =
          scolaritesById.get(
            String(note.idScolarite),
          );

        const eleve =
          elevesById.get(
            String(scolarite?.idEleve),
          );

        const evaluation =
          evaluationsById.get(
            String(note.idEvaluation),
          );

        const searchableContent =
          normalizeText(
            [
              eleve?.nom,
              eleve?.prenom,
              eleve?.matricule,
              evaluation?.libelle,
              evaluation?.typeEvaluation,
              note.valeur,
              note.commentaire,
              note.statutNote,
            ].join(" "),
          );

        return searchableContent.includes(
          normalizedSearch,
        );
      });
    }, [
      elevesById,
      evaluationsById,
      notes,
      scolaritesById,
      search,
    ]);

  function getEleveForScolarite(
    idScolarite,
  ) {
    const scolarite =
      scolaritesById.get(
        String(idScolarite),
      );

    return elevesById.get(
      String(scolarite?.idEleve),
    );
  }

  function resetForm() {
    setEditingNoteId(null);
    setForm({
      ...initialForm,

      idScolarite: String(
        scolariteChoices[0]
          ?.scolarite
          ?.idScolarite ?? "",
      ),

      idEvaluation: String(
        evaluations[0]
          ?.idEvaluation ?? "",
      ),
    });

    setFormError("");
  }

  function handleFieldChange(event) {
    const {
      name,
      value,
    } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setFormError("");
    setSuccessMessage("");
  }

  function handleEdit(note) {
    setEditingNoteId(note.idNote);

    setForm({
      idScolarite:
        String(note.idScolarite),

      idEvaluation:
        String(note.idEvaluation),

      valeur:
        String(note.valeur ?? ""),

      commentaire:
        note.commentaire ?? "",

      statutNote:
        note.statutNote ?? "SAISIE",
    });

    setFormError("");
    setSuccessMessage("");

    window.setTimeout(() => {
      document
        .getElementById("note-form")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
    }, 0);
  }

  async function handleSubmit(event) {
    event.preventDefault();

    setFormError("");
    setSuccessMessage("");

    const idScolarite =
      Number(form.idScolarite);

    const idEvaluation =
      Number(form.idEvaluation);

    const valeur =
      Number(form.valeur);

    if (
      !Number.isInteger(idScolarite) ||
      idScolarite <= 0
    ) {
      setFormError(
        "Sélectionnez une scolarité.",
      );

      return;
    }

    if (
      !Number.isInteger(idEvaluation) ||
      idEvaluation <= 0
    ) {
      setFormError(
        "Sélectionnez une évaluation.",
      );

      return;
    }

    if (
      form.valeur.trim() === "" ||
      !Number.isFinite(valeur) ||
      valeur < 0
    ) {
      setFormError(
        "La note doit être un nombre positif ou nul.",
      );

      return;
    }

    const bareme =
      Number(selectedEvaluation?.bareme);

    if (
      Number.isFinite(bareme) &&
      bareme > 0 &&
      valeur > bareme
    ) {
      setFormError(
        `La note ne peut pas dépasser le barème de ${formatDecimal(
          bareme,
        )}.`,
      );

      return;
    }

    if (!form.statutNote.trim()) {
      setFormError(
        "Le statut de la note est obligatoire.",
      );

      return;
    }

    const payload = {
      idScolarite,
      idEvaluation,
      valeur,

      commentaire:
        form.commentaire.trim() || null,

      statutNote:
        form.statutNote.trim(),
    };

    setSubmitting(true);

    try {
      if (editingNoteId) {
        const updatedNote =
          await updateNote(
            editingNoteId,
            payload,
          );

        setNotes((current) =>
          sortNotes(
            current.map((note) =>
              note.idNote ===
              updatedNote.idNote
                ? updatedNote
                : note,
            ),
          ),
        );

        setSuccessMessage(
          "La note a été modifiée.",
        );
      } else {
        const createdNote =
          await createNote(payload);

        setNotes((current) =>
          sortNotes([
            createdNote,
            ...current,
          ]),
        );

        setSuccessMessage(
          "La note a été enregistrée.",
        );
      }

      resetForm();
    } catch (requestError) {
      setFormError(
        getErrorMessage(
          requestError,
          "Votre rôle ne permet pas d'enregistrer une note.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(note) {
    const eleve =
      getEleveForScolarite(
        note.idScolarite,
      );

    const evaluation =
      evaluationsById.get(
        String(note.idEvaluation),
      );

    const confirmed =
      window.confirm(
        "Supprimer la note de " +
        `${
          eleve
            ? `${eleve.prenom} ${eleve.nom}`
            : `la scolarité #${note.idScolarite}`
        }` +
        " pour l'évaluation " +
        `${
          evaluation?.libelle ??
          `#${note.idEvaluation}`
        } ?`,
      );

    if (!confirmed) {
      return;
    }

    setDeletingNoteId(note.idNote);
    setFormError("");
    setSuccessMessage("");

    try {
      await deleteNote(note.idNote);

      setNotes((current) =>
        current.filter(
          (currentNote) =>
            currentNote.idNote !==
            note.idNote,
        ),
      );

      if (
        editingNoteId === note.idNote
      ) {
        resetForm();
      }

      setSuccessMessage(
        "La note a été supprimée.",
      );
    } catch (requestError) {
      setFormError(
        getErrorMessage(
          requestError,
          "Votre rôle ne permet pas de supprimer une note.",
        ),
      );
    } finally {
      setDeletingNoteId(null);
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
                Module notes
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
              Suivi pédagogique
            </p>

            <h1 className="mt-3 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
              Gestion des notes
            </h1>

            <p className="mt-6 max-w-3xl leading-7 text-school-muted">
              {auth?.role === "RESPONSABLE"
                ? "Consultez les résultats de vos élèves."
                : "Consultez et gérez les résultats des évaluations."}
            </p>
          </div>

          <div className="flex min-w-56 items-center gap-4 bg-school-blue p-6 text-white">
            <NotebookPen
              size={38}
              className="text-brand-yellow"
            />

            <div>
              <p className="text-sm uppercase tracking-wider text-white/60">
                Résultats
              </p>

              <p className="mt-1 font-bold">
                {notes.length}
                {notes.length > 1
                  ? " notes"
                  : " note"}
              </p>
            </div>
          </div>
        </section>

        {loadError && (
          <div
            role="alert"
            className="mt-7 flex gap-3 border border-red-200 bg-red-50 p-5 text-red-800"
          >
            <ShieldAlert
              size={23}
              className="shrink-0"
            />

            <p>{loadError}</p>
          </div>
        )}

        {auth?.role !== "RESPONSABLE" && (
        <section
          id="note-form"
          className="mt-8 border border-slate-200 bg-white p-6 lg:p-8"
        >
          <div className="flex flex-wrap items-start justify-between gap-5">
            <div>
              <p className="text-sm font-bold uppercase tracking-[0.16em] text-school-blue">
                {editingNoteId
                  ? `Modification de la note #${editingNoteId}`
                  : "Nouvelle note"}
              </p>

              <h2 className="mt-2 font-display text-3xl font-bold uppercase text-school-ink">
                {editingNoteId
                  ? "Modifier le résultat"
                  : "Saisir un résultat"}
              </h2>
            </div>

            <button
              type="button"
              onClick={() =>
                setReloadKey(
                  (current) => current + 1,
                )
              }
              disabled={loading}
              className="inline-flex items-center gap-2 border border-slate-300 px-4 py-3 font-semibold text-school-blue transition hover:border-school-blue disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw
                size={18}
                className={
                  loading
                    ? "animate-spin"
                    : ""
                }
              />
              Actualiser
            </button>
          </div>

          {formError && (
            <div
              role="alert"
              className="mt-6 flex gap-3 border border-red-200 bg-red-50 p-4 text-red-800"
            >
              <ShieldAlert
                size={21}
                className="shrink-0"
              />

              <p>{formError}</p>
            </div>
          )}

          {successMessage && (
            <div
              role="status"
              className="mt-6 border border-green-200 bg-green-50 p-4 font-semibold text-green-800"
            >
              {successMessage}
            </div>
          )}

          <form
            onSubmit={handleSubmit}
            className="mt-7 grid gap-5 lg:grid-cols-2"
          >
            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Élève et scolarité
              </span>

              <select
                name="idScolarite"
                value={form.idScolarite}
                onChange={handleFieldChange}
                disabled={
                  loading ||
                  scolariteChoices.length === 0
                }
                className="min-h-13 w-full border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-school-blue disabled:cursor-not-allowed disabled:bg-slate-100"
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
                        : `Élève #${scolarite.idEleve}`}
                      {` — scolarité #${scolarite.idScolarite}`}
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Évaluation
              </span>

              <select
                name="idEvaluation"
                value={form.idEvaluation}
                onChange={handleFieldChange}
                disabled={
                  loading ||
                  evaluations.length === 0
                }
                className="min-h-13 w-full border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-school-blue disabled:cursor-not-allowed disabled:bg-slate-100"
              >
                {evaluations.length === 0 && (
                  <option value="">
                    Aucune évaluation disponible
                  </option>
                )}

                {evaluations.map(
                  (evaluation) => (
                    <option
                      key={
                        evaluation.idEvaluation
                      }
                      value={
                        evaluation.idEvaluation
                      }
                    >
                      {evaluation.libelle}
                      {` — ${formatDate(
                        evaluation.dateEvaluation,
                      )}`}
                      {` — /${formatDecimal(
                        evaluation.bareme,
                      )}`}
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Valeur de la note
              </span>

              <input
                type="number"
                name="valeur"
                value={form.valeur}
                onChange={handleFieldChange}
                min="0"
                max={
                  selectedEvaluation
                    ?.bareme ?? undefined
                }
                step="0.01"
                placeholder="Exemple : 15.50"
                disabled={
                  loading ||
                  evaluations.length === 0
                }
                className="min-h-13 w-full border border-slate-300 px-4 py-3 outline-none transition focus:border-school-blue disabled:cursor-not-allowed disabled:bg-slate-100"
              />

              <span className="mt-2 block text-sm text-school-muted">
                Barème :
                {` ${formatDecimal(
                  selectedEvaluation?.bareme,
                )}`}
              </span>
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Statut
              </span>

              <input
                type="text"
                name="statutNote"
                value={form.statutNote}
                onChange={handleFieldChange}
                maxLength={30}
                placeholder="SAISIE"
                className="min-h-13 w-full border border-slate-300 px-4 py-3 uppercase outline-none transition focus:border-school-blue"
              />
            </label>

            <label className="block lg:col-span-2">
              <span className="mb-2 block text-sm font-bold uppercase tracking-wider text-school-ink">
                Commentaire
              </span>

              <textarea
                name="commentaire"
                value={form.commentaire}
                onChange={handleFieldChange}
                rows={4}
                placeholder="Commentaire facultatif"
                className="w-full resize-y border border-slate-300 px-4 py-3 outline-none transition focus:border-school-blue"
              />
            </label>

            <div className="flex flex-wrap gap-3 lg:col-span-2">
              <button
                type="submit"
                disabled={
                  submitting ||
                  loading ||
                  scolariteChoices.length === 0 ||
                  evaluations.length === 0
                }
                className="inline-flex min-h-13 items-center justify-center gap-2 bg-brand-yellow px-6 py-3 font-display text-lg font-bold uppercase text-black transition hover:bg-yellow-300 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? (
                  <LoaderCircle
                    size={20}
                    className="animate-spin"
                  />
                ) : (
                  <Save size={20} />
                )}

                {submitting
                  ? "Enregistrement"
                  : editingNoteId
                    ? "Enregistrer les modifications"
                    : "Ajouter la note"}
              </button>

              {editingNoteId && (
                <button
                  type="button"
                  onClick={resetForm}
                  disabled={submitting}
                  className="inline-flex min-h-13 items-center gap-2 border border-slate-300 px-5 py-3 font-semibold text-school-ink transition hover:border-school-blue"
                >
                  <X size={19} />
                  Annuler
                </button>
              )}
            </div>
          </form>

          {!loading &&
            evaluations.length === 0 && (
              <p className="mt-6 border border-dashed border-slate-300 p-5 text-school-muted">
                Une évaluation doit être créée avant
                de pouvoir saisir une note.
              </p>
            )}
        </section>
        )}

        <section className="mt-8 border border-slate-200 bg-white p-6 lg:p-8">
          <div className="flex flex-wrap items-end justify-between gap-5">
            <div>
              <h2 className="font-display text-3xl font-bold uppercase text-school-blue">
                Notes enregistrées
              </h2>

              <p className="mt-2 text-school-muted">
                {filteredNotes.length}
                {filteredNotes.length > 1
                  ? " résultats affichés"
                  : " résultat affiché"}
                .
              </p>
            </div>

            <label className="relative block w-full sm:max-w-md">
              <span className="sr-only">
                Rechercher une note
              </span>

              <Search
                size={20}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-school-muted"
              />

              <input
                type="search"
                value={search}
                onChange={(event) =>
                  setSearch(
                    event.target.value,
                  )
                }
                placeholder="Élève, évaluation, statut…"
                className="min-h-13 w-full border border-slate-300 pl-12 pr-4 outline-none transition focus:border-school-blue"
              />
            </label>
          </div>

          {loading && (
            <div className="mt-7 flex items-center justify-center gap-3 border border-slate-200 p-10 text-school-blue">
              <LoaderCircle
                size={27}
                className="animate-spin"
              />

              <p className="font-semibold">
                Chargement des notes…
              </p>
            </div>
          )}

          {!loading &&
            filteredNotes.length === 0 && (
              <div className="mt-7 border border-dashed border-slate-300 p-9 text-center text-school-muted">
                {notes.length === 0
                  ? "Aucune note n'est encore enregistrée."
                  : "Aucune note ne correspond à la recherche."}
              </div>
            )}

          {!loading &&
            filteredNotes.length > 0 && (
              <>
                <div className="mt-7 hidden overflow-x-auto lg:block">
                  <table className="w-full border-collapse text-left">
                    <thead>
                      <tr className="bg-school-blue-dark text-white">
                        <th className="px-5 py-4">
                          Élève
                        </th>

                        <th className="px-5 py-4">
                          Évaluation
                        </th>

                        <th className="px-5 py-4 text-center">
                          Note
                        </th>

                        <th className="px-5 py-4">
                          Statut
                        </th>

                        <th className="px-5 py-4">
                          Saisie
                        </th>

                        {auth?.role !== "RESPONSABLE" && (
                          <th className="px-5 py-4 text-right">Actions</th>
                        )}
                      </tr>
                    </thead>

                    <tbody>
                      {filteredNotes.map(
                        (note) => {
                          const eleve =
                            getEleveForScolarite(
                              note.idScolarite,
                            );

                          const evaluation =
                            evaluationsById.get(
                              String(
                                note.idEvaluation,
                              ),
                            );

                          return (
                            <tr
                              key={note.idNote}
                              className="border-b border-slate-200 align-top"
                            >
                              <td className="px-5 py-5">
                                <p className="font-semibold text-school-ink">
                                  {eleve
                                    ? `${eleve.prenom} ${eleve.nom}`
                                    : `Scolarité #${note.idScolarite}`}
                                </p>

                                <p className="mt-1 text-sm text-school-muted">
                                  {eleve?.matricule ??
                                    "Matricule non disponible"}
                                </p>
                              </td>

                              <td className="px-5 py-5">
                                <p className="font-semibold text-school-ink">
                                  {evaluation
                                    ?.libelle ??
                                    `Évaluation #${note.idEvaluation}`}
                                </p>

                                <p className="mt-1 text-sm text-school-muted">
                                  {evaluation
                                    ?.typeEvaluation ??
                                    "Type non renseigné"}
                                </p>
                              </td>

                              <td className="px-5 py-5 text-center font-display text-2xl font-bold text-school-blue">
                                {formatDecimal(
                                  note.valeur,
                                )}
                                <span className="text-base text-school-muted">
                                  {` / ${formatDecimal(
                                    evaluation?.bareme,
                                  )}`}
                                </span>
                              </td>

                              <td className="px-5 py-5">
                                <span className="inline-flex bg-brand-yellow px-3 py-2 text-xs font-bold uppercase text-black">
                                  {note.statutNote}
                                </span>
                              </td>

                              <td className="px-5 py-5 text-sm text-school-muted">
                                {formatDateTime(
                                  note.dateSaisie,
                                )}
                              </td>

                              {auth?.role !== "RESPONSABLE" && (
                              <td className="px-5 py-5">
                                <div className="flex justify-end gap-2">
                                  <button
                                    type="button"
                                    onClick={() =>
                                      handleEdit(
                                        note,
                                      )
                                    }
                                    className="inline-flex items-center gap-2 border border-slate-300 px-3 py-2 font-semibold text-school-blue transition hover:border-school-blue"
                                  >
                                    <Pencil size={17} />
                                    Modifier
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() =>
                                      handleDelete(
                                        note,
                                      )
                                    }
                                    disabled={
                                      deletingNoteId ===
                                      note.idNote
                                    }
                                    className="inline-flex items-center gap-2 border border-red-200 px-3 py-2 font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
                                  >
                                    {deletingNoteId ===
                                    note.idNote ? (
                                      <LoaderCircle
                                        size={17}
                                        className="animate-spin"
                                      />
                                    ) : (
                                      <Trash2
                                        size={17}
                                      />
                                    )}

                                    Supprimer
                                  </button>
                                </div>
                              </td>
                              )}
                            </tr>
                          );
                        },
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="mt-7 grid gap-4 lg:hidden">
                  {filteredNotes.map(
                    (note) => {
                      const eleve =
                        getEleveForScolarite(
                          note.idScolarite,
                        );

                      const evaluation =
                        evaluationsById.get(
                          String(
                            note.idEvaluation,
                          ),
                        );

                      return (
                        <article
                          key={note.idNote}
                          className="border border-slate-200 p-5"
                        >
                          <div className="flex items-start justify-between gap-4">
                            <div>
                              <div className="flex items-center gap-2 text-school-blue">
                                <UserRound
                                  size={20}
                                />

                                <p className="font-display text-2xl font-bold uppercase">
                                  {eleve
                                    ? `${eleve.prenom} ${eleve.nom}`
                                    : `Scolarité #${note.idScolarite}`}
                                </p>
                              </div>

                              <p className="mt-2 text-sm text-school-muted">
                                {eleve?.matricule ??
                                  "Matricule non disponible"}
                              </p>
                            </div>

                            <span className="bg-brand-yellow px-3 py-2 font-display text-xl font-bold text-black">
                              {formatDecimal(
                                note.valeur,
                              )}
                              {`/${formatDecimal(
                                evaluation?.bareme,
                              )}`}
                            </span>
                          </div>

                          <div className="mt-5 border-t border-slate-200 pt-5">
                            <p className="font-semibold text-school-ink">
                              {evaluation?.libelle ??
                                `Évaluation #${note.idEvaluation}`}
                            </p>

                            <div className="mt-2 flex items-center gap-2 text-sm text-school-muted">
                              <CalendarDays
                                size={16}
                              />

                              {formatDate(
                                evaluation
                                  ?.dateEvaluation,
                              )}
                            </div>

                            {note.commentaire && (
                              <p className="mt-4 bg-slate-50 p-3 text-sm text-school-muted">
                                {note.commentaire}
                              </p>
                            )}
                          </div>

                          <div className="mt-5 flex flex-wrap items-center justify-between gap-4 border-t border-slate-200 pt-4">
                            <span className="text-xs font-bold uppercase tracking-wider text-school-muted">
                              {note.statutNote}
                            </span>

                            {auth?.role !== "RESPONSABLE" && (
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() =>
                                  handleEdit(
                                    note,
                                  )
                                }
                                aria-label={`Modifier la note #${note.idNote}`}
                                className="grid size-10 place-items-center border border-slate-300 text-school-blue"
                              >
                                <Pencil size={18} />
                              </button>

                              <button
                                type="button"
                                onClick={() =>
                                  handleDelete(
                                    note,
                                  )
                                }
                                disabled={
                                  deletingNoteId ===
                                  note.idNote
                                }
                                aria-label={`Supprimer la note #${note.idNote}`}
                                className="grid size-10 place-items-center border border-red-200 text-red-700 disabled:opacity-50"
                              >
                                {deletingNoteId ===
                                note.idNote ? (
                                  <LoaderCircle
                                    size={18}
                                    className="animate-spin"
                                  />
                                ) : (
                                  <Trash2
                                    size={18}
                                  />
                                )}
                              </button>
                            </div>
                            )}
                          </div>
                        </article>
                      );
                    },
                  )}
                </div>
              </>
            )}
        </section>
      </main>
    </div>
  );
}
