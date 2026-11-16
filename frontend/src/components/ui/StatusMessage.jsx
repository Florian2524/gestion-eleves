const variants = {
  error: "border-red-300 bg-red-50 text-red-800",
  success: "border-green-300 bg-green-50 text-green-800",
  loading: "border-school-blue-light bg-school-blue-light/40 text-school-blue-dark",
};

export default function StatusMessage({ variant = "loading", children, className = "" }) {
  return (
    <p role={variant === "error" ? "alert" : "status"} className={`min-w-0 break-words [overflow-wrap:anywhere] border px-4 py-3 text-sm leading-6 ${variants[variant] ?? variants.loading} ${className}`}>
      {children}
    </p>
  );
}
