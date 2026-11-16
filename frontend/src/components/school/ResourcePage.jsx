import Panel from "../ui/Panel";
import Button from "../ui/Button";
import StatusMessage from "../ui/StatusMessage";
import { lightControlClass } from "../ui/controlStyles";
import ConnectedHeader from "../layout/ConnectedHeader";
import { useCallback, useEffect, useState } from "react";
import { attachProfile, createResource, deleteResource, listResource, updateResource } from "../../api/schoolApi";
import { useAuth } from "../../context/authContextCore";
import { schoolLabels, schoolLookups } from "./schoolData";

import PersonneChoice from "./PersonneChoice";
import { profileLabels } from "./personneProfiles";

const identityFields = new Set(["nom", "prenom", "emailContact", "telephone", "adresse"]);
const fieldClass = lightControlClass;
const emptyLookups = [];

export default function ResourcePage({ title, resource, idKey, getId, description, fields, columns, lookups = emptyLookups, profileType, canWrite = (role) => role === "ADMIN", validate, formFields: FormFields, rowDetails: RowDetails, formLabel, fieldGroups, primaryColumn }) {
  const rowId = getId ?? ((row) => row[idKey]);
  const { auth } = useAuth();
  const writable = canWrite(auth?.role);
  const [creationMode, setCreationMode] = useState("new");
  const [selectedPerson, setSelectedPerson] = useState("");
  const [people, setPeople] = useState([]);
  const [rows, setRows] = useState([]);
  const [data, setData] = useState({});
  const [editing, setEditing] = useState(null);
  const attaching = Boolean(profileType && !editing && creationMode === "existing");
  const activeFields = attaching ? fields.filter((field) => !identityFields.has(field.name)) : fields;
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loadError, setLoadError] = useState("");

  const reload = useCallback(async (signal) => {
    setLoading(true); setLoadError("");
    try {
      if (profileType && writable) setPeople(await listResource("personnes", { signal }));
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
  }, [resource, lookups, profileType, writable]);

  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => { if (!controller.signal.aborted) reload(controller.signal); }); return () => controller.abort(); }, [reload]);

  function open(row = null) {
    setCreationMode("new"); setSelectedPerson("");
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
      const body = Object.fromEntries(activeFields.map((field) => {
        const value = form[field.name];
        if (field.type === "checkbox") return [field.name, Boolean(value)];
        if (field.type === "number" || (field.type === "select" && field.lookup)) {
          return [field.name, value === "" || value == null ? null : Number(value)];
        }
        return [field.name, value === "" && field.optional ? null : value];
      }));
      if (activeFields.some((field) => !field.optional && (body[field.name] === null || body[field.name] === "" || (typeof body[field.name] === "number" && !Number.isFinite(body[field.name]))))) {
        setError("Renseignez tous les champs obligatoires."); return;
      }
      if (attaching && !selectedPerson) { setError("Choisissez une personne existante."); return; }
      setBusy(true);
      if (attaching) await attachProfile(Number(selectedPerson), profileType, body);
      else if (editing) await updateResource(resource, rowId(editing), body);
      else await createResource(resource, body);
      setForm(null); setEditing(null); setSuccess(editing ? "Modification enregistrée." : "Création enregistrée.");
      await reload();
    } catch (failure) {
      const validationMessages = Object.values(failure.details?.validationErrors ?? {});
      setError(validationMessages.join(" ") || failure.message || "Enregistrement impossible.");
    }
    finally { setBusy(false); }
  }

  async function remove(row) {
    if (!window.confirm(profileType ? "Retirer uniquement ce profil ? La personne, ses autres profils et son compte seront conservés." : `Supprimer cet élément de ${title.toLowerCase()} ?`)) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteResource(resource, rowId(row)); setSuccess("Suppression effectuée."); await reload(); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  const labels = schoolLabels(data);
  return <div className="min-h-screen bg-school-paper">
    <ConnectedHeader /><main id="main-content" tabIndex={-1} className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      {primaryColumn ? (
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 w-full border-l-4 border-brand-yellow pl-4 sm:w-auto sm:flex-1">
            <h1 className="font-display text-3xl font-bold uppercase text-school-blue-dark [overflow-wrap:anywhere] sm:text-4xl">{title}</h1>
            {description && <p className="mt-2 max-w-3xl text-sm leading-6 text-school-muted">{description}</p>}
          </div>
          {writable && <Button type="button" disabled={busy} onClick={() => open()} variant="primary">Ajouter</Button>}
        </div>
      ) : (
        <>
      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="min-w-0 break-words font-display text-3xl font-bold uppercase text-school-blue sm:text-4xl">{title}</h1>
        {writable && <Button type="button" disabled={busy} onClick={() => open()} variant="school">Ajouter</Button>}
      </div>
      {description && <p className="mt-4 text-school-muted">{description}</p>}
        </>
      )}
      {loading && <StatusMessage className="mt-6">Chargement…</StatusMessage>}
      {error && <StatusMessage variant="error" className="mt-6">{error}</StatusMessage>}
      {loadError && <StatusMessage variant="error" className="mt-6">{loadError}</StatusMessage>}
      {success && <StatusMessage variant="success" className="mt-6">{success}</StatusMessage>}
      {form && writable && !loading && !loadError && <Panel as="form" onSubmit={save} aria-label={formLabel} className="mt-6 p-4 sm:p-6">
        <h2 className="font-display text-2xl font-bold uppercase text-school-blue">{formLabel ? `${editing ? "Modifier" : "Créer"} — ${formLabel}` : editing ? "Modifier" : "Créer"}</h2>
        {profileType && !editing && <label className="mt-4 block font-semibold">Création du profil
          <select disabled={Boolean(primaryColumn && busy)} value={creationMode} onChange={(event) => { setCreationMode(event.target.value); setSelectedPerson(""); }} className={fieldClass}>
            <option value="new">Créer une nouvelle personne</option>
            <option value="existing">Rattacher à une personne existante</option>
          </select>
        </label>}
        {attaching && <PersonneChoice people={people} value={selectedPerson} onChange={setSelectedPerson} profileType={profileType} disabled={busy} />}
        {profileType && editing && <p className="mt-4 text-sm leading-6 text-school-muted">Les nom, prénom et coordonnées sont communs à tous les profils de cette personne.</p>}
        {FormFields ? <FormFields fields={activeFields} form={form} setForm={setForm} data={data} labels={labels} busy={busy} editing={editing} fieldGroups={fieldGroups} resource={resource} /> : (
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            {activeFields.map((field) => <label key={field.name} className="block font-semibold text-school-ink">{field.label}
              {field.type === "select" ? <select className={fieldClass} disabled={Boolean(editing && field.immutable)} required={!field.optional} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })}>
                <option value="">Choisir…</option>
                {(data[field.lookup] ?? []).map((item) => <option key={item[field.optionId]} value={item[field.optionId]}>{field.optionLabel(item, labels)}</option>)}
              </select> : field.type === "checkbox" ? <input className="ml-3" type="checkbox" checked={Boolean(form[field.name])} onChange={(event) => setForm({ ...form, [field.name]: event.target.checked })} />
                : <input className={fieldClass} type={field.type || "text"} min={field.min} step={field.step} maxLength={field.maxLength} required={!field.optional} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })} />}
            </label>)}
          </div>
        )}
        <div className="mt-5 flex flex-wrap gap-3">
          <Button type="submit" loading={busy} disabled={busy} variant="school">{busy ? "Enregistrement…" : "Enregistrer"}</Button>
          <Button type="button" disabled={busy} onClick={() => setForm(null)} variant="secondary">Annuler</Button>
        </div>
      </Panel>}
      {!loading && !loadError && rows.length === 0 && (primaryColumn ? <Panel className="mt-6 p-5"><h2 className="font-display text-xl font-semibold text-school-blue-dark">Aucune donnée disponible.</h2><p className="mt-2 text-sm text-school-muted">{writable ? "Utilisez Ajouter pour renseigner un premier élément." : "Aucun élément n’est disponible pour votre profil."}</p></Panel> : <p className="mt-6 bg-white p-6 text-school-muted">Aucune donnée disponible.</p>)}
      {!loading && !loadError && rows.length > 0 && <div className={primaryColumn ? `mt-6 grid items-start gap-4 md:grid-cols-2 ${columns.length <= 4 ? "xl:grid-cols-3" : ""}` : "mt-6 grid gap-4 md:grid-cols-2"}>
        {rows.map((row) => <Panel as="article" key={rowId(row)} className={primaryColumn ? "min-w-0 p-4 transition-colors hover:border-school-blue focus-within:border-school-blue sm:p-5" : "min-w-0 p-5"}>
          {RowDetails ? <RowDetails row={row} columns={columns} labels={labels} primaryColumn={primaryColumn} /> : (
            <dl className="grid gap-2">{columns.map((column) => <div key={column.label} className="flex flex-wrap justify-between gap-2 border-b border-slate-100 py-2"><dt className="font-semibold text-school-muted">{column.label}</dt><dd className="min-w-0 break-words text-right text-school-ink [overflow-wrap:anywhere]">{column.render(row, labels) ?? "—"}</dd></div>)}</dl>
          )}
          {profileType && <p className="mt-3 break-words text-sm text-school-muted">Personne #{row.idPersonne} · {profileLabels(people.find((person) => person.idPersonne === row.idPersonne)?.profils ?? [profileType])}</p>}
          {writable && <div className="mt-4 flex flex-wrap gap-3 font-semibold"><Button type="button" disabled={busy} onClick={() => open(row)} variant="secondary">Modifier</Button><Button type="button" disabled={busy} onClick={() => remove(row)} variant="danger">Supprimer</Button></div>}
        </Panel>)}
      </div>}
    </main>
  </div>;
}
