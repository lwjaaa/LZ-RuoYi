import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { crx } from "@crxjs/vite-plugin";
import manifest from "./src/manifest.json";
import { resolve } from "path";

export default defineConfig({
  plugins: [vue(), crx({ manifest })],
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
  build: {
    rollupOptions: {
      input: {
        dashboard: resolve(__dirname, "src/dashboard/index.html"),
      },
    },
  },
  server: {
    port: 5173,
    cors: {
      origin: "*",
      credentials: true,
    },
    hmr: {
      host: "localhost",
      port: 5173,
    },
  },
  optimizeDeps: {
    exclude: ["@crxjs/vite-plugin/client"],
  },
});
