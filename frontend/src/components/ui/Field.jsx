export default function Field({
  id,
  name,
  label,
  hint,
  error,
  as = "input",
  options = [],
  required = false,
  className = "",
  controlClassName = "",
  "aria-describedby": describedBy,
  ...rest
}) {
  const controlClasses = [
    "school-field-dark mt-2 w-full border bg-zinc-900 px-4 py-3",
    "text-zinc-100 placeholder:text-zinc-400",
    "transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-yellow disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-300",
    error
      ? "border-red-500 focus:border-red-400"
      : "border-zinc-500 focus:border-brand-yellow",
    controlClassName,
  ].join(" ");

  const errorId = error
    ? `${id}-error`
    : undefined;

  const hintId = hint ? `${id}-hint` : undefined;
  const descriptionIds = [describedBy, hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className={className}>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <label
          htmlFor={id}
          className="text-sm font-semibold text-zinc-200"
        >
          {label}

          {required && (
            <span
              className="ml-1 text-brand-yellow"
              aria-hidden="true"
            >
              *
            </span>
          )}
        </label>

        {hint && (
          <span id={hintId} className="text-xs text-zinc-300">
            {hint}
          </span>
        )}
      </div>

      {as === "textarea" && (
        <textarea
          id={id}
          name={name}
          required={required}
          className={`${controlClasses} min-h-28 resize-y`}
          aria-invalid={Boolean(error)}
          aria-describedby={descriptionIds}
          {...rest}
        />
      )}

      {as === "select" && (
        <select
          id={id}
          name={name}
          required={required}
          className={controlClasses}
          aria-invalid={Boolean(error)}
          aria-describedby={descriptionIds}
          {...rest}
        >
          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
            >
              {option.label}
            </option>
          ))}
        </select>
      )}

      {as === "input" && (
        <input
          id={id}
          name={name}
          required={required}
          className={controlClasses}
          aria-invalid={Boolean(error)}
          aria-describedby={descriptionIds}
          {...rest}
        />
      )}

      {error && (
        <p
          id={errorId}
          role="alert"
          className="mt-1 text-sm text-red-400"
        >
          {error}
        </p>
      )}
    </div>
  );
}
