import { lightControlClass } from "../ui/controlStyles";

const groups = [
  ["Enseignement et période", ["idEnseignement", "idPeriode"]],
  ["Description de l’évaluation", ["libelle", "typeEvaluation", "dateEvaluation"]],
  ["Paramètres de notation", ["bareme", "coefficientEvaluation"]],
];
const hints = {
  idEnseignement: "Choisissez l’enseignement correspondant à la matière et à la classe concernées.",
  idPeriode: "Sélectionnez la période et l’année scolaire de l’évaluation.",
  bareme: "Nombre de points maximum de l’évaluation, par exemple 20.",
  coefficientEvaluation: "Poids de cette évaluation dans la moyenne de la matière.",
};

export function EvaluationFields({ fields, form, setForm, data, labels, busy }) {
  return (
    <div className="mt-4 min-w-0 space-y-5">
      <p className="text-sm leading-6 text-school-muted">Les champs marqués d’un astérisque (*) sont obligatoires. Renseignez le contexte de l’évaluation avant ses paramètres de notation.</p>
      {groups.map(([legend, names]) => (
        <fieldset key={legend} className="min-w-0 border-t border-slate-200 pt-4">
          <legend className="pr-3 text-sm font-bold text-school-blue">{legend}</legend>
          <div className="grid min-w-0 gap-4 sm:grid-cols-2">
            {names.map((name) => {
              const field = fields.find((item) => item.name === name);
              const id = `evaluation-${name}`;
              const describedBy = hints[name] ? `${id}-hint` : undefined;
              return (
                <div key={name} className={`min-w-0 ${name === "libelle" ? "sm:col-span-2" : ""}`}>
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor={id}>{field.label}</label>{!field.optional && <span aria-hidden="true">*</span>}</div>
                  {field.type === "select" ? (
                    <select id={id} className={lightControlClass} required={!field.optional} disabled={busy} aria-describedby={describedBy} value={form[name]} onChange={(event) => setForm({ ...form, [name]: event.target.value })}>
                      <option value="">Choisir…</option>
                      {(data[field.lookup] ?? []).map((item) => <option key={item[field.optionId]} value={item[field.optionId]}>{field.optionLabel(item, labels)}</option>)}
                    </select>
                  ) : (
                    <input id={id} className={lightControlClass} type={field.type || "text"} min={field.min} step={field.step} maxLength={field.maxLength} required={!field.optional} disabled={busy} aria-describedby={describedBy} value={form[name]} onChange={(event) => setForm({ ...form, [name]: event.target.value })} />
                  )}
                  {hints[name] && <p id={describedBy} className="mt-2 text-sm leading-6 text-school-muted">{hints[name]}</p>}
                </div>
              );
            })}
          </div>
        </fieldset>
      ))}
    </div>
  );
}

export function EvaluationDetails({ row, columns, labels }) {
  return (
    <>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <h2 className="min-w-0 flex-1 break-words font-display text-2xl font-semibold text-school-blue-dark">{row.libelle ?? "—"}</h2>
        <p className="bg-brand-yellow px-3 py-2 text-sm font-bold text-black">Barème : {row.bareme ?? "—"} points</p>
      </div>
      <dl className="mt-4 grid min-w-0 gap-3 border-t border-slate-200 pt-4 text-sm leading-6 sm:grid-cols-2">
        {columns.filter((column) => !["Évaluation", "Barème"].includes(column.label)).map((column) => (
          <div key={column.label} className="min-w-0">
            <dt className="font-semibold text-school-muted">{column.label}</dt>
            <dd className="[overflow-wrap:anywhere]">{column.render(row, labels) ?? "—"}</dd>
          </div>
        ))}
      </dl>
    </>
  );
}
