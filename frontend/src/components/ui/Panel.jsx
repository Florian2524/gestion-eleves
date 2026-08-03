const variants = {
  light:
    "border-stone-200 bg-white text-school-ink shadow-[0_18px_50px_rgba(23,23,23,0.06)]",

  dark:
    "border-zinc-800 bg-zinc-950 text-white shadow-[0_18px_50px_rgba(0,0,0,0.18)]",

  yellow:
    "border-brand-yellow bg-brand-yellow text-black",
};

export default function Panel({
  as: Tag = "section",
  variant = "light",
  className = "",
  children,
  ...rest
}) {
  const variantClasses =
    variants[variant] ?? variants.light;

  return (
    <Tag
      className={[
        "border",
        variantClasses,
        className,
      ].join(" ")}
      {...rest}
    >
      {children}
    </Tag>
  );
}
