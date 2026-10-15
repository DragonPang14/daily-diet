import { createSSRApp } from "vue";
import uviewPlus from "uview-plus";
import App from "./App.vue";
import { pinia } from "./store";

export function createApp() {
  const app = createSSRApp(App);
  app.use(pinia);
  app.use(uviewPlus);
  return {
    app,
  };
}
