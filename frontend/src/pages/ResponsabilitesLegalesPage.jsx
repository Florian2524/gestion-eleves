import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { createLegalLink, deleteLegalLink, listLegalLinks } from "../api/adminApi";
import { getEleves } from "../api/elevesApi";
import { listResource } from "../api/schoolApi";

const control = "mt-2 min-h-11 w-full border border-slate-300 bg-white px-3 text-school-ink focus:border-school-blue";
const personName = (person) => `${person.prenom} ${person.nom}`;

export default function ResponsabilitesLegalesPage() {
  const [links, setLinks] = useState([]);
  const [eleves, setEleves] = useState([]);
  const [responsables, setResponsables] = useState([]);
  const [form, setForm] = useState({ idResponsable: "", idEleve: "" });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const reload = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [currentLinks, currentEleves, currentResponsables] = await Promise.all([
        listLegalLinks({ signal }), getEleves({ signal }), listResource("responsables", { signal }),
      ]);
      if (signal?.aborted) return;
      setLinks(currentLinks); setEleves(currentEleves); setResponsables(currentResponsables);
    } catch (failure) { if (!signal?.aborted) setError(failure.message || "Chargement impossible."); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, []);

  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => reload(controller.signal)); return () => controller.abort(); }, [reload]);

  async function save(event) {
    event.preventDefault(); setBusy(true); setError(""); setSuccess("");
    try {
      await createLegalLink({ idResponsable: Number(form.idResponsable), idEleve: Number(form.idEleve) });
      setForm({ idResponsable: "", idEleve: "" }); setSuccess("Lien créé."); await reload();
    } catch (failure) { setError(failure.message || "Création impossible."); }
    finally { setBusy(false); }
  }

  async function remove(link) {
    if (!window.confirm(`Supprimer le lien entre ${link.prenomResponsable} ${link.nomResponsable} et ${link.prenomEleve} ${link.nomEleve} ?`)) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteLegalLink(link.idResponsable, link.idEleve); setSuccess("Lien supprimé."); await reload(); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  return <div className="min-h-screen bg-school-paper px-4 py-8 sm:px-6"><main className="mx-auto max-w-5xl">
    <Link to="/espace" className="font-semibold text-school-blue underline">← Tableau de bord</Link>
    <h1 className="mt-6 font-display text-4xl font-bold uppercase text-school-blue">Responsabilités légales</h1>
    <p className="mt-3 text-school-muted">Associer chaque responsable légal aux élèves dont il peut consulter les données.</p>
    {error && <p role="alert" className="mt-6 border border-red-300 bg-red-50 p-4 text-red-800">{error}</p>}
    {success && <p role="status" className="mt-6 border border-green-300 bg-green-50 p-4 text-green-800">{success}</p>}
    {loading && <p role="status" className="mt-6">Chargement…</p>}
    {!loading && <>
      <form onSubmit={save} className="mt-6 bg-white p-6 shadow-sm">
        <h2 className="font-display text-2xl font-bold uppercase text-school-blue">Créer un lien</h2>
        <div className="mt-5 grid gap-4 sm:grid-cols-2">
          <label className="font-semibold text-school-ink">Responsable légal<select className={control} required disabled={busy} value={form.idResponsable} onChange={(event) => setForm({ ...form, idResponsable: event.target.value })}><option value="">Choisir…</option>{responsables.map((item) => <option key={item.idPersonne} value={item.idPersonne}>{personName(item)}</option>)}</select></label>
          <label className="font-semibold text-school-ink">Élève<select className={control} required disabled={busy} value={form.idEleve} onChange={(event) => setForm({ ...form, idEleve: event.target.value })}><option value="">Choisir…</option>{eleves.map((item) => <option key={item.idPersonne} value={item.idPersonne}>{personName(item)}</option>)}</select></label>
        </div>
        <button disabled={busy || !responsables.length || !eleves.length} className="mt-5 bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">{busy ? "Enregistrement…" : "Associer"}</button>
        {(!responsables.length || !eleves.length) && <p className="mt-3 text-school-muted">Créez d’abord un responsable et un élève.</p>}
      </form>
      {links.length === 0 ? <p className="mt-6 bg-white p-6 text-school-muted">Aucune responsabilité légale enregistrée.</p> : <div className="mt-6 grid gap-4 sm:grid-cols-2">{links.map((link) => <article key={`${link.idResponsable}-${link.idEleve}`} className="bg-white p-5 shadow-sm"><p className="font-semibold text-school-ink">{link.prenomResponsable} {link.nomResponsable} <span aria-hidden="true">→</span> {link.prenomEleve} {link.nomEleve}</p><button type="button" disabled={busy} onClick={() => remove(link)} className="mt-4 text-red-700 underline disabled:opacity-50">Supprimer le lien</button></article>)}</div>}
    </>}
  </main></div>;
}
