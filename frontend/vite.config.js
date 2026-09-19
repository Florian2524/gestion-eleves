import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],

  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.js"],
  },

  server: {
    port: 5173,

    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,

        rewrite: (path) =>
          path.replace(/^\/api/, ""),
      },
    },
  },
});
