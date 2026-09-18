import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import { getEleves } from "../api/elevesApi";
import { getScolarites } from "../api/bulletinsApi";
import { createNote, deleteNote, getEvaluations, getNotes, updateNote } from "../api/notesApi";
import { listResource } from "../api/schoolApi";
import { useAuth } from "../context/authContextCore";
import { schoolLabels } from "../components/school/schoolData";

const emptyForm = { idEvaluation: "", idScolarite: "", valeur: "", commentaire: "", statutNote: "SAISIE" };
const fieldClass = "mt-2 min-h-11 w-full border border-slate-300 bg-white px-3 focus:border-school-blue";

export default function NotesPage() {
  const { auth } = useAuth();
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

  function selectEvaluation(value) {
    const evaluation = data.evaluations.find((item) => String(item.idEvaluation) === value);
    const teaching = data.enseignements.find((item) => item.idEnseignement === evaluation?.idEnseignement);
    setForm({ ...form, idEvaluation: value, idScolarite: data.scolarites.some((item) => String(item.idScolarite) === form.idScolarite && item.idClasse === teaching?.idClasse) ? form.idScolarite : "" });
  }

  function edit(note) {
    setEditing(note.idNote);
    setForm({ idEvaluation: String(note.idEvaluation), idScolarite: String(note.idScolarite), valeur: String(note.valeur), commentaire: note.commentaire ?? "", statutNote: note.statutNote });
    setError(""); setSuccess("");
    window.scrollTo({ top: 0, behavior: "smooth" });
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

  return <div className="min-h-screen bg-school-paper px-4 py-8 sm:px-6"><main className="mx-auto max-w-6xl">
    <Link to="/espace" className="font-semibold text-school-blue underline">← Tableau de bord</Link>
    <h1 className="mt-6 font-display text-4xl font-bold uppercase text-school-blue">Notes</h1>
    <p className="mt-2 text-school-muted">Résultats des élèves accessibles à votre profil.</p>
    {loading && <p role="status" className="mt-6">Chargement…</p>}
    {error && <p role="alert" className="mt-6 border border-red-300 bg-red-50 p-4 text-red-800">{error}</p>}
    {success && <p role="status" className="mt-6 border border-green-300 bg-green-50 p-4 text-green-800">{success}</p>}
    {writable && !loading && <form onSubmit={save} className="mt-6 bg-white p-6 shadow-sm">
      <h2 className="font-display text-2xl font-bold uppercase text-school-blue">{editing ? "Modifier une note" : "Saisir une note"}</h2>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <label className="font-semibold">Évaluation
          <select className={fieldClass} required disabled={busy || Boolean(editing)} value={form.idEvaluation} onChange={(event) => selectEvaluation(event.target.value)}>
            <option value="">Choisir une évaluation…</option>
            {data.evaluations.map((item) => <option key={item.idEvaluation} value={item.idEvaluation}>{item.libelle} · {labels.enseignement(item.idEnseignement)} · /{item.bareme}</option>)}
          </select>
        </label>
        <label className="font-semibold">Élève et scolarité
          <select className={fieldClass} required disabled={busy || !selectedEvaluation} value={form.idScolarite} onChange={(event) => setForm({ ...form, idScolarite: event.target.value })}>
            <option value="">Choisir un élève…</option>
            {compatible.map((item) => <option key={item.idScolarite} value={item.idScolarite}>{labels.eleve(item.idEleve)} · {labels.classe(item.idClasse)} · {item.dateDebut}</option>)}
          </select>
        </label>
        <label className="font-semibold">Note {selectedEvaluation ? `(sur ${selectedEvaluation.bareme})` : ""}
          <input className={fieldClass} type="number" required min="0" max={selectedEvaluation?.bareme} step="0.01" disabled={busy || !selectedEvaluation} value={form.valeur} onChange={(event) => setForm({ ...form, valeur: event.target.value })} />
        </label>
        <label className="font-semibold sm:col-span-2">Commentaire
          <textarea className={fieldClass} disabled={busy} value={form.commentaire} onChange={(event) => setForm({ ...form, commentaire: event.target.value })} />
        </label>
        <label className="font-semibold">Statut
          <input className={fieldClass} required maxLength={30} disabled={busy} value={form.statutNote} onChange={(event) => setForm({ ...form, statutNote: event.target.value })} />
        </label>
      </div>
      <div className="mt-5 flex gap-3"><button disabled={busy || !selectedEvaluation || !compatible.length} className="bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">{busy ? "Enregistrement…" : "Enregistrer"}</button>
        {editing && <button type="button" disabled={busy} onClick={() => { setEditing(null); setForm(emptyForm); }} className="border border-slate-300 px-5 py-3 font-bold">Annuler</button>}
      </div>
      {selectedNote && <p className="mt-3 text-sm text-school-muted">Modification de la note de {labels.eleve(data.scolarites.find((item) => item.idScolarite === selectedNote.idScolarite)?.idEleve)}.</p>}
    </form>}
    {!loading && <label className="mt-6 block font-semibold">Rechercher une note
      <input type="search" className={fieldClass} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Élève, évaluation, statut…" />
    </label>}
    {!loading && filteredNotes.length === 0 && <p className="mt-6 bg-white p-6 text-school-muted">{data.notes.length ? "Aucune note ne correspond à la recherche." : "Aucune note disponible."}</p>}
    {!loading && <div className="mt-6 grid gap-4 md:grid-cols-2">{filteredNotes.map((note) => {
      const evaluation = data.evaluations.find((item) => item.idEvaluation === note.idEvaluation);
      const scolarite = data.scolarites.find((item) => item.idScolarite === note.idScolarite);
      return <article key={note.idNote} className="bg-white p-5 shadow-sm">
        <h2 className="font-display text-2xl font-bold uppercase text-school-blue">{scolarite ? labels.eleve(scolarite.idEleve) : "Élève indisponible"}</h2>
        <p className="mt-2">{evaluation?.libelle ?? "Évaluation indisponible"} · {evaluation ? labels.enseignement(evaluation.idEnseignement) : ""}</p>
        {evaluation && <p className="mt-1 text-sm text-school-muted">{evaluation.typeEvaluation} · {labels.periode(evaluation.idPeriode)}</p>}
        <p className="mt-2 font-bold">{note.valeur} / {evaluation?.bareme ?? "—"}</p>
        <p className="mt-2 text-sm text-school-muted">Statut : {note.statutNote} · Évaluation : {evaluation?.dateEvaluation ?? "Date indisponible"}{note.dateSaisie ? ` · Saisie : ${note.dateSaisie}` : ""}</p>
        {note.commentaire && <p className="mt-2 text-school-muted">{note.commentaire}</p>}
        {writable && <div className="mt-4 flex gap-4 font-semibold"><button type="button" disabled={busy} onClick={() => edit(note)} className="text-school-blue underline disabled:opacity-50">Modifier</button><button type="button" disabled={busy} onClick={() => remove(note)} className="text-red-700 underline disabled:opacity-50">Supprimer</button></div>}
      </article>;
    })}</div>}
  </main></div>;
}
