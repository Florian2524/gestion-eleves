const variants = {
  school:
    "border-school-blue bg-school-blue text-white enabled:hover:border-school-blue-dark enabled:hover:bg-school-blue-dark",

  danger:
    "border-red-700 bg-red-700 text-white enabled:hover:border-red-800 enabled:hover:bg-red-800",

  primary:
    "border-brand-yellow bg-brand-yellow text-black enabled:hover:bg-black enabled:hover:text-brand-yellow",

  dark:
    "border-black bg-black text-white enabled:hover:bg-brand-yellow enabled:hover:text-black enabled:hover:border-brand-yellow",

  secondary:
    "border-zinc-400 bg-transparent text-zinc-800 enabled:hover:border-black enabled:hover:bg-black enabled:hover:text-white",

  ghost:
    "border-white/30 bg-transparent text-white enabled:hover:border-brand-yellow enabled:hover:text-brand-yellow",
};

export default function Button({
  type = "button",
  variant = "primary",
  className = "",
  children,
  loading = false,
  disabled = false,
  ...rest
}) {
  const variantClasses =
    variants[variant] ?? variants.primary;

  return (
    <button
      type={type}
      className={[
        "inline-flex min-h-11 max-w-full items-center justify-center gap-2 break-words text-center",
        "border px-4 py-2 text-sm font-bold",
        "transition duration-150",
        "focus-visible:outline-none",
        "focus-visible:ring-2",
        "focus-visible:ring-offset-2",
        variant === "ghost"
          ? "focus-visible:ring-brand-yellow focus-visible:ring-offset-school-blue-dark"
          : "focus-visible:ring-school-blue-dark focus-visible:ring-offset-white",
        "disabled:cursor-not-allowed",
        "disabled:border-slate-400 disabled:bg-slate-200 disabled:text-slate-700",
        variantClasses,
        className,
      ].join(" ")}
      {...rest}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {children}
    </button>
  );
}
