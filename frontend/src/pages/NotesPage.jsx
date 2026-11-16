import Button from "../components/ui/Button";
import Panel from "../components/ui/Panel";
import StatusMessage from "../components/ui/StatusMessage";
import { lightControlClass } from "../components/ui/controlStyles";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import { useEffect, useMemo, useRef, useState } from "react";
import { getEleves } from "../api/elevesApi";
import { getScolarites } from "../api/bulletinsApi";
import { createNote, deleteNote, getEvaluations, getNotes, updateNote } from "../api/notesApi";
import { listResource } from "../api/schoolApi";
import { useAuth } from "../context/authContextCore";
import { schoolLabels } from "../components/school/schoolData";

const emptyForm = { idEvaluation: "", idScolarite: "", valeur: "", commentaire: "", statutNote: "SAISIE" };
const fieldClass = lightControlClass;

export default function NotesPage() {
  const { auth } = useAuth();
  const formTitleRef = useRef(null);
  const writable = auth?.role !== "RESPONSABLE";
  const [data, setData] = useState({ notes: [], evaluations: [], scolarites: [], eleves: [], enseignements: [], classes: [], matieres: [], periodes: [] });
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [search, setSearch] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true); setError("");
      try {
        const [notes, evaluations, scolarites, eleves, enseignements, classes, matieres, periodes] = await Promise.all([
          getNotes({ signal: controller.signal }), getEvaluations({ signal: controller.signal }),
          getScolarites({ signal: controller.signal }), getEleves({ signal: controller.signal }),
          listResource("enseignements", { signal: controller.signal }),
          listResource("classes", { signal: controller.signal }), listResource("matieres", { signal: controller.signal }),
          listResource("periodes", { signal: controller.signal }),
        ]);
        if (!controller.signal.aborted) setData({ notes, evaluations, scolarites, eleves, enseignements, classes, matieres, periodes });
      } catch (failure) { if (!controller.signal.aborted) setError(failure.message || "Chargement impossible."); }
      finally { if (!controller.signal.aborted) setLoading(false); }
    }
    load();
    return () => controller.abort();
  }, [reloadKey]);

  const labels = schoolLabels(data);
  const selectedEvaluation = data.evaluations.find((item) => String(item.idEvaluation) === String(form.idEvaluation));
  const selectedTeaching = data.enseignements.find((item) => item.idEnseignement === selectedEvaluation?.idEnseignement);
  const compatible = useMemo(() => data.scolarites.filter((item) => item.idClasse === selectedTeaching?.idClasse), [data.scolarites, selectedTeaching?.idClasse]);
  const selectedNote = data.notes.find((item) => item.idNote === editing);
  const filteredNotes = useMemo(() => {
    const query = search.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
    const orderedNotes = [...data.notes].sort((first, second) => Number(second.idNote) - Number(first.idNote));
    if (!query) return orderedNotes;
    return orderedNotes.filter((note) => {
      const evaluation = data.evaluations.find((item) => item.idEvaluation === note.idEvaluation);
      const scolarite = data.scolarites.find((item) => item.idScolarite === note.idScolarite);
      const eleve = data.eleves.find((item) => item.idPersonne === scolarite?.idEleve);
      return [eleve?.nom, eleve?.prenom, eleve?.matricule, evaluation?.libelle, evaluation?.typeEvaluation, note.valeur, note.commentaire, note.statutNote]
        .join(" ").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().includes(query);
    });
  }, [data, search]);

  useEffect(() => {
    if (editing !== null) formTitleRef.current?.focus({ preventScroll: true });
  }, [editing]);

  function selectEvaluation(value) {
    const evaluation = data.evaluations.find((item) => String(item.idEvaluation) === value);
    const teaching = data.enseignements.find((item) => item.idEnseignement === evaluation?.idEnseignement);
    setForm({ ...form, idEvaluation: value, idScolarite: data.scolarites.some((item) => String(item.idScolarite) === form.idScolarite && item.idClasse === teaching?.idClasse) ? form.idScolarite : "" });
  }

  function edit(note) {
    setEditing(note.idNote);
    setForm({ idEvaluation: String(note.idEvaluation), idScolarite: String(note.idScolarite), valeur: String(note.valeur), commentaire: note.commentaire ?? "", statutNote: note.statutNote });
    setError(""); setSuccess("");
    window.scrollTo({ top: 0, behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches ? "instant" : "smooth" });
  }

  async function save(event) {
    event.preventDefault();
    if (!selectedEvaluation || !compatible.some((item) => String(item.idScolarite) === form.idScolarite)) { setError("Choisissez une évaluation et une scolarité de la même classe."); return; }
    if (form.valeur.trim() === "" || !Number.isFinite(Number(form.valeur)) || Number(form.valeur) < 0 || Number(form.valeur) > Number(selectedEvaluation.bareme)) { setError(`La note doit être comprise entre 0 et ${selectedEvaluation.bareme}.`); return; }
    if (!form.statutNote.trim()) { setError("Le statut est obligatoire."); return; }
    setBusy(true); setError(""); setSuccess("");
    try {
      const body = { idEvaluation: Number(form.idEvaluation), idScolarite: Number(form.idScolarite), valeur: Number(form.valeur), commentaire: form.commentaire.trim() || null, statutNote: form.statutNote.trim() };
      if (editing) await updateNote(editing, body); else await createNote(body);
      setSuccess(editing ? "Note modifiée." : "Note créée."); setEditing(null); setForm(emptyForm); setReloadKey((value) => value + 1);
    } catch (failure) { setError(failure.message || "Enregistrement impossible."); }
    finally { setBusy(false); }
  }

  async function remove(note) {
    if (!window.confirm("Supprimer cette note ?")) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteNote(note.idNote); setSuccess("Note supprimée."); setReloadKey((value) => value + 1); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <div className="mb-6 border-l-4 border-brand-yellow pl-4">
          <p className="text-sm font-bold uppercase tracking-widest text-school-blue">Suivi des résultats</p>
          <h1 className="mt-1 font-display text-3xl font-bold uppercase text-school-blue-dark sm:text-4xl">Notes</h1>
          <p className="mt-2 text-sm leading-6 text-school-muted">Résultats des élèves accessibles à votre profil.</p>
        </div>
        {loading && <StatusMessage className="mb-6">Chargement des notes…</StatusMessage>}
        {error && <StatusMessage variant="error" className="mb-6">{error}</StatusMessage>}
        {success && <StatusMessage variant="success" className="mb-6">{success}</StatusMessage>}
        {writable && !loading && (
          <Panel as="form" onSubmit={save} aria-labelledby="note-form-title" className="border-t-4 border-t-school-blue p-4 sm:p-6">
            <h2 ref={formTitleRef} tabIndex={-1} id="note-form-title" className="font-display text-2xl font-semibold text-school-blue-dark">{editing ? "Modifier une note" : "Saisir une note"}</h2>
            <p className="mt-2 text-sm leading-6 text-school-muted">Les champs marqués d’un astérisque (*) sont obligatoires. Choisissez d’abord une évaluation, puis un élève de la même classe.</p>
            <div className="mt-5 grid min-w-0 gap-5 sm:grid-cols-2">
              <div className="min-w-0 sm:col-span-2">
                <div className="flex gap-1"><label className="block text-sm font-semibold" htmlFor="note-evaluation">Évaluation</label><span aria-hidden="true">*</span></div>
                <select id="note-evaluation" className={fieldClass} aria-describedby="note-evaluation-hint" required disabled={busy || Boolean(editing)} value={form.idEvaluation} onChange={(event) => selectEvaluation(event.target.value)}>
                  <option value="">Choisir une évaluation…</option>
                  {data.evaluations.map((item) => <option key={item.idEvaluation} value={item.idEvaluation}>{item.libelle} · {labels.enseignement(item.idEnseignement)} · /{item.bareme}</option>)}
                </select>
                <p id="note-evaluation-hint" className="mt-2 text-sm leading-6 text-school-muted">
                  {selectedEvaluation ? `${labels.enseignement(selectedEvaluation.idEnseignement)} · Barème : ${selectedEvaluation.bareme} points.` : "La sélection de l’évaluation détermine les scolarités proposées et le barème de la note."}
                  {editing ? " L’évaluation reste inchangée pendant la modification." : ""}
                </p>
              </div>
              <div className="min-w-0">
                <div className="flex gap-1"><label className="block text-sm font-semibold" htmlFor="note-scolarite">Élève et scolarité</label><span aria-hidden="true">*</span></div>
                <select id="note-scolarite" className={fieldClass} aria-describedby="note-scolarite-hint" required disabled={busy || !selectedEvaluation} value={form.idScolarite} onChange={(event) => setForm({ ...form, idScolarite: event.target.value })}>
                  <option value="">Choisir un élève…</option>
                  {compatible.map((item) => <option key={item.idScolarite} value={item.idScolarite}>{labels.eleve(item.idEleve)} · {labels.classe(item.idClasse)} · {item.dateDebut}</option>)}
                </select>
                <p id="note-scolarite-hint" className="mt-2 text-sm leading-6 text-school-muted">{!selectedEvaluation ? "Sélectionnez d’abord une évaluation." : compatible.length ? "Seules les scolarités de la classe de l’évaluation sont proposées." : "Aucune scolarité compatible avec cette évaluation."}</p>
              </div>
              <div className="min-w-0">
                <div className="flex gap-1"><label className="block text-sm font-semibold" htmlFor="note-valeur">Note {selectedEvaluation ? `(sur ${selectedEvaluation.bareme})` : ""}</label><span aria-hidden="true">*</span></div>
                <input id="note-valeur" className={fieldClass} aria-describedby="note-valeur-hint" type="number" required min="0" max={selectedEvaluation?.bareme} step="0.01" disabled={busy || !selectedEvaluation} value={form.valeur} onChange={(event) => setForm({ ...form, valeur: event.target.value })} />
                <p id="note-valeur-hint" className="mt-2 text-sm leading-6 text-school-muted">{selectedEvaluation ? `Valeur attendue entre 0 et ${selectedEvaluation.bareme}.` : "Le barème sera affiché après sélection de l’évaluation."}</p>
              </div>
              <div className="min-w-0">
                <div className="flex gap-1"><label className="block text-sm font-semibold" htmlFor="note-statut">Statut</label><span aria-hidden="true">*</span></div>
                <input id="note-statut" className={fieldClass} aria-describedby="note-statut-hint" required maxLength={30} disabled={busy} value={form.statutNote} onChange={(event) => setForm({ ...form, statutNote: event.target.value })} />
                <p id="note-statut-hint" className="mt-2 text-sm leading-6 text-school-muted">Statut de la note, prérempli avec « SAISIE » pour une nouvelle note.</p>
              </div>
              <div className="min-w-0 sm:col-span-2">
                <label className="block text-sm font-semibold" htmlFor="note-commentaire">Commentaire</label>
                <textarea id="note-commentaire" className={`${fieldClass} min-h-24 resize-y`} aria-describedby="note-commentaire-hint" disabled={busy} value={form.commentaire} onChange={(event) => setForm({ ...form, commentaire: event.target.value })} />
                <p id="note-commentaire-hint" className="mt-2 text-sm text-school-muted">Facultatif : précisez le contexte ou une observation sur ce résultat.</p>
              </div>
            </div>
            <div className="mt-6 flex flex-wrap gap-3 border-t border-slate-200 pt-5">
              <Button type="submit" variant="school" loading={busy} disabled={busy || !selectedEvaluation || !compatible.length}>{busy ? "Enregistrement…" : "Enregistrer"}</Button>
              {editing && <Button variant="secondary" disabled={busy} onClick={() => { setEditing(null); setForm(emptyForm); }}>Annuler</Button>}
            </div>
            {selectedNote && <p className="mt-3 text-sm text-school-muted">Modification de la note de {labels.eleve(data.scolarites.find((item) => item.idScolarite === selectedNote.idScolarite)?.idEleve)}.</p>}
          </Panel>
        )}
        {!loading && (
          <section aria-labelledby="notes-list-title" className="mt-8">
            <Panel className="p-4 sm:p-6">
              <div className="flex flex-wrap items-baseline justify-between gap-3">
                <h2 id="notes-list-title" className="font-display text-2xl font-semibold text-school-blue-dark">Consulter les notes</h2>
                <p role="status" className="text-sm font-semibold text-school-blue">{filteredNotes.length} résultat{filteredNotes.length > 1 ? "s" : ""} sur {data.notes.length} note{data.notes.length > 1 ? "s" : ""}</p>
              </div>
              <label className="mt-4 block text-sm font-semibold" htmlFor="notes-search">Rechercher une note</label>
              <input id="notes-search" type="search" className={fieldClass} aria-describedby="notes-search-hint" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Élève, évaluation, statut…" />
              <p id="notes-search-hint" className="mt-2 text-sm text-school-muted">Recherchez par élève, matricule, évaluation, résultat, commentaire ou statut.</p>
            </Panel>
            {filteredNotes.length === 0 && <Panel className="mt-4 p-6 text-center"><h3 className="font-display text-xl font-semibold text-school-blue">{data.notes.length ? "Aucun résultat" : "Aucune note disponible"}</h3><p className="mt-2 text-sm leading-6 text-school-muted">{data.notes.length ? "Aucune note ne correspond à la recherche. Essayez un autre nom, une évaluation ou un statut." : "Aucune note n’est disponible pour votre profil."}</p></Panel>}
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {filteredNotes.map((note) => {
                const evaluation = data.evaluations.find((item) => item.idEvaluation === note.idEvaluation);
                const scolarite = data.scolarites.find((item) => item.idScolarite === note.idScolarite);
                return (
                  <Panel as="article" key={note.idNote} className="flex min-w-0 flex-col p-4 transition-colors hover:border-school-blue focus-within:border-school-blue sm:p-5">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <h3 className="break-words font-display text-2xl font-semibold text-school-blue-dark">{scolarite ? labels.eleve(scolarite.idEleve) : "Élève indisponible"}</h3>
                        <p className="mt-1 break-words font-semibold text-school-ink">{evaluation?.libelle ?? "Évaluation indisponible"}</p>
                      </div>
                      <p className="bg-brand-yellow px-3 py-2 font-bold text-black" aria-label={`Note : ${note.valeur} sur ${evaluation?.bareme ?? "barème indisponible"}`}>{note.valeur} <span className="text-sm font-medium">/ {evaluation?.bareme ?? "—"}</span></p>
                    </div>
                    <dl className="mt-4 grid min-w-0 gap-3 border-t border-slate-200 pt-4 text-sm leading-6">
                      <div><dt className="font-semibold text-school-muted">Matière et classe</dt><dd className="break-words">{evaluation ? labels.enseignement(evaluation.idEnseignement) : "Enseignement indisponible"}</dd></div>
                      {evaluation && <div><dt className="font-semibold text-school-muted">Période et type</dt><dd className="break-words">{labels.periode(evaluation.idPeriode)}{evaluation.typeEvaluation ? ` · ${evaluation.typeEvaluation}` : ""}</dd></div>}
                      <div className="grid gap-3 sm:grid-cols-2">
                        <div><dt className="font-semibold text-school-muted">Date d’évaluation</dt><dd>{evaluation?.dateEvaluation ?? "Date indisponible"}</dd></div>
                        <div><dt className="font-semibold text-school-muted">Statut</dt><dd className="break-words">{note.statutNote}</dd></div>
                      </div>
                      {note.dateSaisie && <div><dt className="font-semibold text-school-muted">Date de saisie</dt><dd className="break-words">{note.dateSaisie}</dd></div>}
                      {note.commentaire && <div className="bg-school-paper p-3"><dt className="font-semibold text-school-muted">Commentaire</dt><dd className="whitespace-pre-wrap [overflow-wrap:anywhere]">{note.commentaire}</dd></div>}
                    </dl>
                    {writable && <div className="mt-auto flex flex-wrap gap-3 pt-5"><Button variant="secondary" disabled={busy} onClick={() => edit(note)}>Modifier</Button><Button variant="danger" disabled={busy} onClick={() => remove(note)}>Supprimer</Button></div>}
                  </Panel>
                );
              })}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
