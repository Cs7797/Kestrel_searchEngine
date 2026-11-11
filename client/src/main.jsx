import React from "react";
import ReactDOM from "react-dom/client";
import { MotionConfig } from "motion/react";
import App from "./App";
import LaunchScreen from "./components/LaunchScreen";
import "./styles.css";
import "./theme.css";
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <MotionConfig reducedMotion="user">
      <LaunchScreen>
        <App />
      </LaunchScreen>
    </MotionConfig>
  </React.StrictMode>,
);
