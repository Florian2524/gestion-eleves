import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { createResource, deleteResource, listResource, updateResource } from "../../api/schoolApi";
import { useAuth } from "../../context/authContextCore";
import { schoolLabels, schoolLookups } from "./schoolData";

const fieldClass = "mt-2 min-h-11 w-full border border-slate-300 bg-white px-3 text-school-ink focus:border-school-blue";
const emptyLookups = [];

export default function ResourcePage({ title, resource, idKey, fields, columns, lookups = emptyLookups, canWrite = (role) => role === "ADMIN", validate }) {
  const { auth } = useAuth();
  const writable = canWrite(auth?.role);
  const [rows, setRows] = useState([]);
  const [data, setData] = useState({});
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loadError, setLoadError] = useState("");

  const reload = useCallback(async (signal) => {
    setLoading(true); setLoadError("");
    try {
      const [items, ...choices] = await Promise.all([
        listResource(resource, { signal }),
        ...lookups.map((key) => schoolLookups[key]({ signal })),
      ]);
      if (signal?.aborted) return;
      setRows(items);
      setData(Object.fromEntries(lookups.map((key, index) => [key, choices[index]])));
    } catch (failure) {
      if (!signal?.aborted) setLoadError(failure.message || "Chargement impossible.");
    } finally { if (!signal?.aborted) setLoading(false); }
  }, [resource, lookups]);

  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => { if (!controller.signal.aborted) reload(controller.signal); }); return () => controller.abort(); }, [reload]);

  function open(row = null) {
    setEditing(row);
    setForm(Object.fromEntries(fields.map((field) => [field.name, row?.[field.name] ?? field.defaultValue ?? ""])));
    setError(""); setSuccess("");
  }

  async function save(event) {
    event.preventDefault();
    const validation = validate?.(form);
    if (validation) { setError(validation); return; }
    setError(""); setSuccess("");
    try {
      const body = Object.fromEntries(fields.map((field) => {
        const value = form[field.name];
        if (field.type === "checkbox") return [field.name, Boolean(value)];
        if (field.type === "number" || (field.type === "select" && field.lookup)) {
          return [field.name, value === "" || value == null ? null : Number(value)];
        }
        return [field.name, value === "" && field.optional ? null : value];
      }));
      if (fields.some((field) => !field.optional && (body[field.name] === null || body[field.name] === "" || (typeof body[field.name] === "number" && !Number.isFinite(body[field.name]))))) {
        setError("Renseignez tous les champs obligatoires."); return;
      }
      setBusy(true);
      if (editing) await updateResource(resource, editing[idKey], body);
      else await createResource(resource, body);
      setForm(null); setEditing(null); setSuccess(editing ? "Modification enregistrée." : "Création enregistrée.");
      await reload();
    } catch (failure) { setError(failure.message || "Enregistrement impossible."); }
    finally { setBusy(false); }
  }

  async function remove(row) {
    if (!window.confirm(`Supprimer cet élément de ${title.toLowerCase()} ?`)) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteResource(resource, row[idKey]); setSuccess("Suppression effectuée."); await reload(); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  const labels = schoolLabels(data);
  return <div className="min-h-screen bg-school-paper px-4 py-8 sm:px-6">
    <main className="mx-auto max-w-6xl">
      <Link to="/espace" className="font-semibold text-school-blue underline">← Tableau de bord</Link>
      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="font-display text-4xl font-bold uppercase text-school-blue">{title}</h1>
        {writable && <button type="button" disabled={busy} onClick={() => open()} className="bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">Ajouter</button>}
      </div>
      {loading && <p role="status" className="mt-6">Chargement…</p>}
      {error && <p role="alert" className="mt-6 border border-red-300 bg-red-50 p-4 text-red-800">{error}</p>}
      {loadError && <p role="alert" className="mt-6 border border-red-300 bg-red-50 p-4 text-red-800">{loadError}</p>}
      {success && <p role="status" className="mt-6 border border-green-300 bg-green-50 p-4 text-green-800">{success}</p>}
      {form && writable && !loading && !loadError && <form onSubmit={save} className="mt-6 bg-white p-6 shadow-sm">
        <h2 className="font-display text-2xl font-bold uppercase text-school-blue">{editing ? "Modifier" : "Créer"}</h2>
        <div className="mt-5 grid gap-4 sm:grid-cols-2">
          {fields.map((field) => <label key={field.name} className="block font-semibold text-school-ink">{field.label}
            {field.type === "select" ? <select className={fieldClass} required={!field.optional} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })}>
              <option value="">Choisir…</option>
              {(data[field.lookup] ?? []).map((item) => <option key={item[field.optionId]} value={item[field.optionId]}>{field.optionLabel(item, labels)}</option>)}
            </select> : field.type === "checkbox" ? <input className="ml-3" type="checkbox" checked={Boolean(form[field.name])} onChange={(event) => setForm({ ...form, [field.name]: event.target.checked })} />
              : <input className={fieldClass} type={field.type || "text"} min={field.min} step={field.step} maxLength={field.maxLength} required={!field.optional} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })} />}
          </label>)}
        </div>
        <div className="mt-5 flex gap-3">
          <button disabled={busy} className="bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">{busy ? "Enregistrement…" : "Enregistrer"}</button>
          <button type="button" disabled={busy} onClick={() => setForm(null)} className="border border-slate-300 px-5 py-3 font-bold">Annuler</button>
        </div>
      </form>}
      {!loading && !loadError && rows.length === 0 && <p className="mt-6 bg-white p-6 text-school-muted">Aucune donnée disponible.</p>}
      {!loading && !loadError && rows.length > 0 && <div className="mt-6 grid gap-4 md:grid-cols-2">
        {rows.map((row) => <article key={row[idKey]} className="bg-white p-5 shadow-sm">
          <dl className="grid gap-2">{columns.map((column) => <div key={column.label} className="flex flex-wrap justify-between gap-2 border-b border-slate-100 py-2"><dt className="font-semibold text-school-muted">{column.label}</dt><dd className="text-right text-school-ink">{column.render(row, labels) ?? "—"}</dd></div>)}</dl>
          {writable && <div className="mt-4 flex gap-4 font-semibold"><button type="button" disabled={busy} onClick={() => open(row)} className="text-school-blue underline disabled:opacity-50">Modifier</button><button type="button" disabled={busy} onClick={() => remove(row)} className="text-red-700 underline disabled:opacity-50">Supprimer</button></div>}
        </article>)}
      </div>}
    </main>
  </div>;
}
