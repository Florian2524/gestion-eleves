import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const backendTarget = "http://localhost:8080";

function backendProxy() {
  return {
    target: backendTarget,
    changeOrigin: true,
  };
}

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],

  server: {
    port: 5173,
    strictPort: true,

    proxy: {
      "/auth": backendProxy(),
      "/eleves": backendProxy(),
      "/classes": backendProxy(),
      "/scolarites": backendProxy(),
      "/periodes": backendProxy(),
      "/notes": backendProxy(),
      "/evaluations": backendProxy(),
      "/bulletins": backendProxy(),
      "/enseignants": backendProxy(),
      "/enseignements": backendProxy(),
      "/matieres": backendProxy(),
      "/comptes-utilisateurs": backendProxy(),
    },
  },
});
