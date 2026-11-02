import React from "react";
import { createRoot } from "react-dom/client";
import "leaflet/dist/leaflet.css";
import "./styles.css";
import App from "./App";
import { aplicarTema, temaInicial } from "./tema";

aplicarTema(temaInicial(), false);   // antes de pintar, para evitar un destello de otro tema

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
