import { defineConfig } from 'vite';
import { viteSingleFile } from 'vite-plugin-singlefile';

export default defineConfig({
  plugins: [viteSingleFile()],
  server: {
    host: '0.0.0.0',
    port: 6272,
  },
  build: {
    // Single file: inline everything
    assetsInlineLimit: Infinity,
    cssCodeSplit: false,
  }
});
