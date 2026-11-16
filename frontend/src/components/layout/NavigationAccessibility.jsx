import { useEffect, useRef } from "react";
import { useLocation } from "react-router";

export default function NavigationAccessibility() {
  const { pathname } = useLocation();
  const previousPath = useRef(pathname);

  useEffect(() => {
    // Preserve initial autofocus and in-page interactions (including query/hash changes).
    if (previousPath.current === pathname) return;
    previousPath.current = pathname;
    // A redirect may temporarily render no page; focus its final destination instead.
    document.getElementById("main-content")?.focus();
  }, [pathname]);

  function skipToContent(event) {
    const content = document.getElementById("main-content");
    if (content) {
      event.preventDefault();
      content.focus();
    }
  }

  return (
    <a href="#main-content" onClick={skipToContent} className="skip-link">
      Aller au contenu principal
    </a>
  );
}
