import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import scalaJS from '@scala-js/vite-plugin-scalajs'

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  let basePath;
  if(env["GITHUB_ACTIONS"] === "true") {
    basePath = "/" + env["GITHUB_REPOSITORY"].split("/")[1]
  } else {
    basePath = "/"
  }
  return {
    base: basePath,
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src/"),
      },
    },
    plugins: [
      scalaJS({
        cwd: './scalajs'
      }),
    ],
    worker: {
      format: 'es'
    }
  }
})
