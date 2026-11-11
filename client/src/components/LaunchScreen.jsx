import { useEffect, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import "./launch-screen.css";

export default function LaunchScreen({ children }) {
  const [ready, setReady] = useState(false);
  const reducedMotion = useReducedMotion();

  useEffect(() => {
    const timer = setTimeout(() => setReady(true), 3000);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (ready) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [ready]);

  return (
    <AnimatePresence mode="wait">
      {!ready ? (
        <motion.div
          key="launch"
          className="launch-screen"
          role="status"
          aria-label="Loading Kestrel"
          exit={{ opacity: 0 }}
          transition={{ duration: reducedMotion ? 0 : 0.25 }}
        >
          <div className="launch-grid" aria-hidden="true" />
          <div className="launch-cross launch-cross-one" aria-hidden="true" />
          <div className="launch-cross launch-cross-two" aria-hidden="true" />
          <div className="launch-center">
            <div className="launch-emblem" aria-hidden="true">
              <svg className="launch-ring" viewBox="0 0 160 160" fill="none">
                <circle
                  cx="80"
                  cy="80"
                  r="72"
                  stroke="var(--line)"
                  strokeWidth="1"
                />
                <motion.circle
                  cx="80"
                  cy="80"
                  r="72"
                  stroke="var(--violet)"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  initial={reducedMotion ? false : { pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ duration: 3, ease: "easeInOut" }}
                />
              </svg>
              <motion.svg
                className="launch-logo"
                viewBox="0 0 40 40"
                initial={
                  reducedMotion ? false : { scale: 0.8, rotate: -8, opacity: 0 }
                }
                animate={{ scale: 1, rotate: 0, opacity: 1 }}
                transition={{ duration: 0.7, ease: [0.2, 0.7, 0.2, 1] }}
              >
                <rect width="40" height="40" rx="9" fill="var(--button-bg)" />
                <motion.path
                  d="M10 9v22h5V20l12 11h7L20 18 31 9h-8l-8 8V9z"
                  fill="var(--button-ink)"
                  stroke="var(--button-ink)"
                  strokeWidth=".4"
                  initial={
                    reducedMotion ? false : { pathLength: 0, fillOpacity: 0 }
                  }
                  animate={{ pathLength: 1, fillOpacity: 1 }}
                  transition={{
                    pathLength: { duration: 1.1, delay: 0.2 },
                    fillOpacity: { duration: 0.4, delay: 1 },
                  }}
                />
              </motion.svg>
            </div>
            <motion.div
              initial={reducedMotion ? false : { opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.3 }}
            >
              <div className="launch-wordmark">
                kestrel<span>.</span>
              </div>
              <p>Search, built from first principles.</p>
            </motion.div>
            <div className="launch-progress" aria-hidden="true">
              <motion.div
                initial={reducedMotion ? false : { scaleX: 0 }}
                animate={{ scaleX: 1 }}
                transition={{ duration: 3, ease: "easeInOut" }}
              />
            </div>
          </div>
          <div className="launch-caption" aria-hidden="true">
            JAVA ENGINE <span>·</span> POSITIONAL INDEX <span>·</span> BM25
          </div>
        </motion.div>
      ) : (
        <motion.div
          key="application"
          initial={reducedMotion ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.2 }}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
