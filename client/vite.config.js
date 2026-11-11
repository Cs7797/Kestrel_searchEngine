import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { resolveDevApiTarget } from "./config/backend.js";
export default defineConfig(({ mode, command }) => {
  // Read PORT only on the Vite server; never expose other backend environment values.
  const env = loadEnv(mode, process.cwd(), ["VITE_", "PORT"]);
  const target = resolveDevApiTarget(env);
  const proxy = {
    "/kestrel-api": {
      target,
      changeOrigin: true,
      timeout: 125000,
      proxyTimeout: 125000,
      rewrite: (path) => path.replace(/^\/kestrel-api/, ""),
    },
  };
  function restrictProxy(server) {
    server.middlewares.use((req, res, next) => {
      if (req.url?.startsWith("/kestrel-api/") && req.method !== "GET") {
        res.statusCode = 405;
        res.end("The local demo proxy only permits GET requests.");
        return;
      }
      next();
    });
  }
  return {
    // The public deployment keeps VITE_KESTREL_API_URL; PORT is a local proxy target.
    define:
      command === "serve"
        ? { "import.meta.env.VITE_KESTREL_API_URL": JSON.stringify(target) }
        : undefined,
    plugins: [
      react(),
      tailwindcss(),
      {
        name: "read-only-api-proxy",
        configureServer: restrictProxy,
      },
    ],
    server: { proxy },
  };
});
