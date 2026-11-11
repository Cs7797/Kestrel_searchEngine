import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import {
  ArrowUpRight,
  Search,
  ArrowRight,
  LockKeyhole,
  LoaderCircle,
  RefreshCw,
  Terminal,
  CircleAlert,
} from "lucide-react";
import { API_URL, apiUrl, safeExternalUrl, search } from "../lib/api";
export const statusLabels = {
  unconfigured: "API not configured",
  unchecked: "API not checked",
  checking: "Connecting to API",
  online: "API responded",
  unavailable: "API connection blocked or unavailable",
};
export default function SearchConsole({ setApiStatus }) {
  const [query, setQuery] = useState("");
  const [submitted, setSubmitted] = useState("");
  const [results, setResults] = useState([]);
  const [state, setState] = useState("idle");
  const [waking, setWaking] = useState(false);
  const [error, setError] = useState("");
  const current = useRef(null);
  const input = useRef(null);
  const timers = useRef([]);
  function clearTimers() {
    timers.current.forEach(clearTimeout);
    timers.current = [];
  }
  useEffect(() => {
    if (!API_URL) return;
    const controller = new AbortController();
    setApiStatus("checking");
    let timedOut = false;
    const timeout = setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, 120_000);
    fetch(apiUrl("/actuator/health"), { signal: controller.signal })
      .then(async (r) => {
        const body = await r.json();
        if (!r.ok || body.status !== "UP") throw new Error("Not healthy");
        if (!controller.signal.aborted && !current.current)
          setApiStatus("online");
      })
      .catch(() => {
        if (!current.current && (!controller.signal.aborted || timedOut))
          setApiStatus("unavailable");
      })
      .finally(() => clearTimeout(timeout));
    return () => {
      controller.abort();
      clearTimeout(timeout);
    };
  }, [setApiStatus]);
  useEffect(
    () => () => {
      current.current?.abort();
      clearTimers();
    },
    [],
  );
  async function run(value) {
    const q = value.trim();
    if (!q) {
      input.current?.focus();
      return;
    }
    current.current?.abort();
    clearTimers();
    const controller = new AbortController();
    current.current = controller;
    setQuery(q);
    setSubmitted(q);
    setState("loading");
    setWaking(false);
    setError("");
    setApiStatus(API_URL ? "checking" : "unconfigured");
    let timedOut = false;
    timers.current = [
      setTimeout(() => setWaking(true), 5000),
      setTimeout(() => {
        timedOut = true;
        controller.abort();
      }, 120_000),
    ];
    try {
      const data = await search(q, controller.signal);
      if (current.current !== controller) return;
      setResults(data);
      setState("success");
      setApiStatus("online");
    } catch (e) {
      if (current.current !== controller) return;
      setError(
        timedOut
          ? "The search server has not responded after two minutes. It might be temporarily down or restarting. Please give it a few minutes, then retry."
          : e instanceof TypeError
            ? "Could not connect to the search server. It may be temporarily unavailable. Check your connection, or give it a few minutes, then retry."
            : e instanceof Error
              ? e.message
              : "Search could not complete. Please retry.",
      );
      setState("error");
      setApiStatus(API_URL ? "unavailable" : "unconfigured");
    } finally {
      if (current.current === controller) {
        clearTimers();
        setWaking(false);
      }
    }
  }
  return (
    <div className="search-shell" id="search">
      <div className="console-bar">
        <span>
          <Terminal size={15} /> SEARCH PLAYGROUND
        </span>
        <span className="mono">KESTREL / v1.0</span>
      </div>
      <div className="search-body">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void run(query);
          }}
          className="search-form"
        >
          <Search size={22} aria-hidden="true" />
          <label className="sr-only" htmlFor="search-query">
            Search technical documents
          </label>
          <input
            ref={input}
            id="search-query"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="What are you curious about?"
            maxLength={500}
            autoComplete="off"
          />
          <button
            className="button primary"
            type="submit"
            aria-label="Search documents"
          >
            Search <ArrowRight size={17} />
          </button>
        </form>
        <div className="suggestions">
          <span>TRY A QUERY</span>
          {["java concurrency", "BM25 ranking", "docker compose"].map((q) => (
            <button key={q} onClick={() => void run(q)}>
              {q}
              <ArrowUpRight size={12} />
            </button>
          ))}
          <details>
            <summary>More examples</summary>
            <div>
              {[
                "positional phrase search",
                "postgresql transactions",
                "machine learning",
                "precision recall ndcg",
              ].map((q) => (
                <button key={q} onClick={() => void run(q)}>
                  {q}
                  <ArrowUpRight size={12} />
                </button>
              ))}
            </div>
          </details>
        </div>
        <div
          className="result-area"
          aria-live="polite"
          aria-busy={state === "loading"}
        >
          <AnimatePresence mode="wait">
            {state === "idle" && (
              <motion.div
                key="idle"
                className="idle-state"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <div className="search-orbit">
                  <Search size={26} />
                </div>
                <h3>A little curiosity. A lot underneath.</h3>
                <p>
                  Search a curated collection of 70 technical documents.
                  <br />
                  Every result ranked by Kestrel’s own Java engine.
                </p>
                <div className="engine-tags">
                  <span>POSITIONAL INDEX</span>
                  <span>BM25 RANKING</span>
                  <span>TITLE BOOSTING</span>
                </div>
              </motion.div>
            )}
            {state === "loading" && (
              <motion.div
                key="loading"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
              >
                <div className="result-heading">
                  <span>
                    <LoaderCircle className="spin" size={16} />
                    Searching for “{submitted}”
                  </span>
                </div>
                {waking && (
                  <div className="notice">
                    <strong>Waking up the search engine…</strong>
                    <p>
                      Kestrel is hosted on Render’s free tier, so the backend
                      may need a moment to restart after inactivity. Initial
                      requests can take a minute or longer.
                    </p>
                  </div>
                )}
                <div className="skeleton" />
                <div className="skeleton short" />
                <div className="skeleton" />
              </motion.div>
            )}
            {state === "error" && (
              <motion.div
                key="error"
                className="message-state"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
              >
                <CircleAlert size={26} />
                <h3>Let’s reconnect.</h3>
                <p>{error}</p>
                <button
                  className="button secondary"
                  onClick={() => void run(submitted)}
                >
                  <RefreshCw size={15} /> Retry search
                </button>
              </motion.div>
            )}
            {state === "success" && (
              <motion.div
                key={`results-${submitted}`}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
              >
                <div className="result-heading">
                  <span>
                    {results.length}{" "}
                    {results.length === 1 ? "result" : "results"} for “
                    {submitted}”
                  </span>
                  <span className="mono">BM25 · TOP 20</span>
                </div>
                {results.length === 0 ? (
                  <div className="message-state">
                    <Search size={26} />
                    <h3>No matches. Keep exploring.</h3>
                    <p>
                      Try fewer terms or a broader topic such as “java” or
                      “search”. Stopword-only queries do not return matches.
                    </p>
                  </div>
                ) : (
                  results.map((r, i) => {
                    const url = safeExternalUrl(r.url);
                    return (
                      <motion.article
                        className="result-card"
                        key={r.id}
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: Math.min(i * 0.035, 0.25) }}
                      >
                        <span className="rank">
                          {String(r.rank).padStart(2, "0")}
                        </span>
                        <div>
                          <span className="result-id">{r.id}</span>
                          <h3>
                            {url ? (
                              <a
                                href={url}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                {r.title || "Untitled document"}
                                <ArrowUpRight size={16} />
                              </a>
                            ) : (
                              r.title || "Untitled document"
                            )}
                          </h3>
                          <p>
                            {r.snippet ||
                              "No preview is available for this document."}
                          </p>
                          {url && (
                            <a
                              className="result-url"
                              href={url}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {new URL(url).hostname}
                            </a>
                          )}
                        </div>
                      </motion.article>
                    );
                  })
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
      <div className="readonly">
        <LockKeyhole size={15} />
        <div>
          <strong>Read-only demonstration</strong>
          <p>
            This public instance uses a curated collection of technical
            documents. Document creation, editing, and deletion are disabled to
            protect the demo environment.
          </p>
        </div>
      </div>
    </div>
  );
}
