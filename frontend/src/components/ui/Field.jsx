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
  ...rest
}) {
  const controlClasses = [
    "mt-2 w-full border bg-zinc-900 px-4 py-3",
    "text-zinc-100 placeholder:text-zinc-600",
    "outline-none transition",
    error
      ? "border-red-500 focus:border-red-400"
      : "border-zinc-700 focus:border-brand-yellow",
  ].join(" ");

  const errorId = error
    ? `${id}-error`
    : undefined;

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
          <span className="text-xs text-zinc-500">
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
          aria-describedby={errorId}
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
          aria-describedby={errorId}
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
          aria-describedby={errorId}
          {...rest}
        />
      )}

      {error && (
        <p
          id={errorId}
          className="mt-1 text-sm text-red-400"
        >
          {error}
        </p>
      )}
    </div>
  );
}
