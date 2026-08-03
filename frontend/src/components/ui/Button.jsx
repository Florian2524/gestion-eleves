const variants = {
  primary:
    "border-brand-yellow bg-brand-yellow text-black hover:bg-black hover:text-brand-yellow",

  dark:
    "border-black bg-black text-white hover:bg-brand-yellow hover:text-black hover:border-brand-yellow",

  secondary:
    "border-zinc-400 bg-transparent text-zinc-800 hover:border-black hover:bg-black hover:text-white",

  ghost:
    "border-white/30 bg-transparent text-white hover:border-brand-yellow hover:text-brand-yellow",
};

export default function Button({
  type = "button",
  variant = "primary",
  className = "",
  children,
  ...rest
}) {
  const variantClasses =
    variants[variant] ?? variants.primary;

  return (
    <button
      type={type}
      className={[
        "inline-flex min-h-11 items-center justify-center gap-2",
        "border px-4 py-2 text-sm font-bold",
        "transition duration-150",
        "focus-visible:outline-none",
        "focus-visible:ring-2",
        "focus-visible:ring-brand-yellow",
        "focus-visible:ring-offset-2",
        "disabled:cursor-not-allowed",
        "disabled:opacity-60",
        variantClasses,
        className,
      ].join(" ")}
      {...rest}
    >
      {children}
    </button>
  );
}
