import { lightControlClass } from "../ui/controlStyles";

const hints = {
  "classes.idProfesseurPrincipal": "Facultatif : associez un enseignant déjà enregistré à cette classe.",
  "periodes.anneeScolaire": "Facultative. Format AAAA-AAAA, avec deux années consécutives, par exemple 2026-2027. Une valeur vide reste non renseignée.",
  "periodes.dateDebutSaisie": "Facultative : début de la fenêtre de saisie des notes.",
  "periodes.dateFinSaisie": "Facultative : fin de la fenêtre de saisie des notes.",
  "ponderations.coefficientMatiere": "Coefficient commun à cette matière dans cette classe. Les bulletins calculés à nouveau utiliseront cette pondération.",
  "enseignements.coefficientMatiere": "Doit correspondre à la pondération de la classe et de la matière. Un nouveau couple reprend le coefficient saisi.",
  "inscriptions.idEleve": "Sélectionnez l’élève concerné par cette inscription.",
  "scolarites.idEleve": "Sélectionnez l’élève dont vous renseignez le parcours scolaire.",
  "scolarites.idClasse": "La classe sélectionnée indique également l’année scolaire de cette affectation.",
};

export function ManagementFields({ fields, form, setForm, data, labels, busy, editing, fieldGroups, resource }) {
  return (
    <div className="mt-4 min-w-0 space-y-5">
      <p className="text-sm leading-6 text-school-muted">Les champs marqués d’un astérisque (*) sont obligatoires. Les autres champs sont facultatifs.</p>
      {fieldGroups.map(([legend, names]) => {
        const groupedFields = fields.filter((field) => names.includes(field.name));
        if (groupedFields.length === 0) return null;
        return (
          <fieldset key={legend} className="min-w-0 border-t border-slate-200 pt-4">
            <legend className="pr-3 text-sm font-bold text-school-blue">{legend}</legend>
            <div className="grid min-w-0 gap-4 sm:grid-cols-2">
              {groupedFields.map((field) => {
                const id = `${resource}-${field.name}`;
                const immutable = Boolean(editing && field.immutable);
                const hint = immutable ? "Cette association ne peut pas être modifiée." : hints[`${resource}.${field.name}`];
                const hintId = hint ? `${id}-hint` : undefined;
                return (
                  <div key={field.name} className={`min-w-0 ${field.name === "adresse" ? "sm:col-span-2" : ""}`}>
                    {field.type === "checkbox" ? (
                      <label className="flex min-h-11 items-center gap-3 text-sm font-semibold" htmlFor={id}>
                        <input id={id} className="size-5 shrink-0 accent-school-blue" type="checkbox" disabled={busy} checked={Boolean(form[field.name])} onChange={(event) => setForm({ ...form, [field.name]: event.target.checked })} />{field.label}
                      </label>
                    ) : (
                      <>
                        <div className="flex gap-1 text-sm font-semibold text-school-ink"><label htmlFor={id}>{field.label}</label>{!field.optional && <span aria-hidden="true">*</span>}</div>
                        {field.type === "select" ? (
                          <select id={id} className={lightControlClass} disabled={busy || immutable} required={!field.optional} aria-describedby={hintId} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })}>
                            <option value="">Choisir…</option>
                            {(data[field.lookup] ?? []).map((item) => <option key={item[field.optionId]} value={item[field.optionId]}>{field.optionLabel(item, labels)}</option>)}
                          </select>
                        ) : (
                          <input id={id} className={lightControlClass} type={field.type || "text"} min={field.min} step={field.step} maxLength={field.maxLength} required={!field.optional} disabled={busy} aria-describedby={hintId} value={form[field.name]} onChange={(event) => setForm({ ...form, [field.name]: event.target.value })} />
                        )}
                      </>
                    )}
                    {hint && <p id={hintId} className="mt-2 text-sm leading-6 text-school-muted">{hint}</p>}
                  </div>
                );
              })}
            </div>
          </fieldset>
        );
      })}
    </div>
  );
}

export function ManagementDetails({ row, columns, labels, primaryColumn }) {
  const mainColumn = columns.find((column) => column.label === primaryColumn);
  return (
    <>
      <p className="text-xs font-semibold uppercase tracking-wider text-school-muted">{mainColumn.label}</p>
      <h2 className="mt-1 font-display text-2xl font-semibold text-school-blue-dark [overflow-wrap:anywhere]">{mainColumn.render(row, labels) ?? "—"}</h2>
      <dl className="mt-4 grid min-w-0 gap-x-4 gap-y-3 border-t border-slate-200 pt-4 text-sm leading-6 sm:grid-cols-2">
        {columns.filter((column) => column !== mainColumn).map((column) => (
          <div key={column.label} className={`min-w-0 ${column.label === "Coefficient" ? "bg-school-paper px-3 py-2" : ""}`}>
            <dt className="font-semibold text-school-muted">{column.label}</dt>
            <dd className="text-school-ink [overflow-wrap:anywhere]">{column.render(row, labels) ?? "—"}</dd>
          </div>
        ))}
      </dl>
    </>
  );
}
