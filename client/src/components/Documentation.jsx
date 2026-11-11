import { useState } from "react";
import { Check, Copy, ArrowUpRight, BookOpen } from "lucide-react";
import { SectionHeading } from "./Engineering";
import { API_URL } from "../lib/api";
export const REPO = "https://github.com/Cs7797/Kestrel_searchEngine";
function CodeBlock({ code, label }) {
  const [copied, setCopied] = useState(false);
  const [failed, setFailed] = useState(false);
  return (
    <div className="code-block">
      <div>
        <span>{label}</span>
        <button
          aria-label={`Copy ${label}`}
          onClick={async () => {
            try {
              await navigator.clipboard.writeText(code);
              setCopied(true);
              setFailed(false);
              setTimeout(() => setCopied(false), 2000);
            } catch {
              setFailed(true);
            }
          }}
        >
          {copied ? <Check size={14} /> : <Copy size={14} />}{" "}
          {copied ? "Copied" : failed ? "Select text to copy" : "Copy"}
        </button>
      </div>
      <pre>
        <code>{code}</code>
      </pre>
    </div>
  );
}
const endpoints = [
  [
    "GET",
    "/api/search/results?q=java",
    "Ranked previews: id, title, snippet, nullable url, rank. Up to 20 results.",
  ],
  [
    "GET",
    "/api/search/ranked?q=java",
    "All matching document IDs ordered by relevance.",
  ],
  [
    "GET",
    "/api/documents",
    "All persisted document entities: id, title, content, url.",
  ],
  ["GET", "/api/documents/{id}", "A document entity, or HTTP 404."],
  ["GET", "/actuator/health", "Spring Boot health status."],
  [
    "POST",
    "/api/documents",
    "Create a document. Disabled on the public instance.",
  ],
  [
    "PUT",
    "/api/documents/{id}",
    "Replace a document. Disabled on the public instance.",
  ],
  [
    "DELETE",
    "/api/documents/{id}",
    "Delete a document. Disabled on the public instance.",
  ],
];
export default function Documentation() {
  const [tab, setTab] = useState("Overview");
  const tabs = ["Overview", "Getting started", "API reference", "Limitations"];
  return (
    <section className="section" id="docs">
      <SectionHeading
        number="04"
        eyebrow="DEVELOPER DOCUMENTATION"
        title="Go from the interface to the implementation."
        description="The interesting part is underneath. Explore the API, run the engine locally, or read the source."
      />
      <div className="docs-layout">
        <div
          className="docs-nav"
          role="tablist"
          aria-label="Documentation sections"
          aria-orientation="vertical"
        >
          {tabs.map((t, i) => (
            <button
              role="tab"
              id={`doc-tab-${i}`}
              aria-controls="docs-panel"
              aria-selected={tab === t}
              tabIndex={tab === t ? 0 : -1}
              className={tab === t ? "active" : ""}
              key={t}
              onClick={() => setTab(t)}
              onKeyDown={(e) => {
                if (["ArrowDown", "ArrowUp", "Home", "End"].includes(e.key)) {
                  e.preventDefault();
                  const n =
                    e.key === "Home"
                      ? 0
                      : e.key === "End"
                        ? tabs.length - 1
                        : (i + (e.key === "ArrowDown" ? 1 : -1) + tabs.length) %
                          tabs.length;
                  setTab(tabs[n]);
                  document.getElementById(`doc-tab-${n}`)?.focus();
                }
              }}
            >
              {t}
              <ArrowUpRight size={14} />
            </button>
          ))}
          <a href={REPO} target="_blank" rel="noopener noreferrer">
            Source on GitHub <ArrowUpRight size={14} />
          </a>
        </div>
        <div
          className="docs-content"
          role="tabpanel"
          id="docs-panel"
          aria-labelledby={`doc-tab-${tabs.indexOf(tab)}`}
        >
          {tab === "Overview" && (
            <>
              <BookOpen size={24} className="violet" />
              <h3>A search engine you can reason about.</h3>
              <p>
                Kestrel explores information retrieval by implementing the index
                and ranking algorithm directly in Java. It connects data
                structures, concurrency, API design, and persistence in one
                working system.
              </p>
              <p>
                Separate positional title and body indexes map terms to document
                IDs and positions. BM25 scores each field with k₁ = 1.5 and b =
                0.75, then combines the scores with a 2.0 title weight.
              </p>
              <p>
                PostgreSQL persists documents through Spring Data JPA and Flyway
                migrations. The application rebuilds the index on startup.
                Database writes update the index after transaction commit.
                ReentrantReadWriteLock permits simultaneous searches and
                exclusive index writes.
              </p>
              <a
                className="text-link"
                href={`${REPO}/tree/main/src/main/java/com/kestrel`}
                target="_blank"
                rel="noopener noreferrer"
              >
                Read the Java implementation <ArrowUpRight size={15} />
              </a>
            </>
          )}
          {tab === "Getting started" && (
            <>
              <h3>Run Kestrel locally.</h3>
              <p>
                Requirements: Java 21, Maven, Node.js 22.12+ and Docker with
                Compose. Run backend commands from the repository root.
              </p>
              <CodeBlock
                label="Docker Compose"
                code={
                  "# Set KESTREL_DB_PASSWORD in your shell first\ndocker compose up --build"
                }
              />
              <p>
                Compose creates PostgreSQL and the backend on port 8080. The
                local database starts empty; document ingestion is available
                through the local API. Public writes must remain disabled.
              </p>
              <CodeBlock
                label="Frontend setup"
                code={
                  "cd client\nnpm ci\n# Copy .env.example to .env.local\n# Set VITE_KESTREL_API_URL=http://localhost:8080\nnpm run dev"
                }
              />
              <CodeBlock
                label="Backend unit tests (no database)"
                code={
                  'mvn "-Dtest=DocumentTest,IndexTest,TitleAwareRankingTest,EnglishStemmerUtilTest,DocumentLifecycleTest,InvertedIndexConcurrencyTest,SearchControllerTest" test'
                }
              />
              <p>
                Database integration tests require a dedicated local
                kestrel_test database and KESTREL_TEST_DB_PASSWORD. Follow the
                repository’s test profile. Backend credentials stay in server
                environment variables; the frontend receives only the public API
                origin.
              </p>
            </>
          )}
          {tab === "API reference" && (
            <>
              <h3>The REST surface.</h3>
              <p>
                Keyword queries use BM25 ranking. Quotes do not enable phrase
                mode. Blank queries return HTTP 400. Snippets are the first 160
                content characters, not query-centered extracts.
              </p>
              <div className="endpoint-list">
                {endpoints.map(([method, path, text]) => (
                  <div key={method + path}>
                    <code
                      className={
                        method === "GET" ? "method" : "method restricted"
                      }
                    >
                      {method}
                    </code>
                    <div>
                      <code>{path}</code>
                      <p>{text}</p>
                    </div>
                  </div>
                ))}
              </div>
              <CodeBlock
                label="Search request"
                code={`curl "${API_URL || "https://YOUR-RENDER-SERVICE.onrender.com"}/api/search/results?q=java%20concurrency"`}
              />
              <CodeBlock
                label="Illustrative response schema — not live results"
                code={
                  '[\n  {\n    "id": "document-id",\n    "title": "Document title",\n    "snippet": "Document content preview...",\n    "url": null,\n    "rank": 1\n  }\n]'
                }
              />
              <p>
                Local mutation request schema:{" "}
                <code>
                  {
                    "{ id: string, title: string, content: string, url: string | null }"
                  }
                </code>
                . ID and title must be nonblank; content cannot be null. PUT
                requires the body ID to match the path. POST returns 201; PUT
                and DELETE return 200 with{" "}
                <code>{"{ message, id, title }"}</code>. With
                KESTREL_WRITES_ENABLED=false, mutations return 403.
              </p>
            </>
          )}
          {tab === "Limitations" && (
            <>
              <h3>Clear boundaries. Better engineering.</h3>
              <ul className="limitations">
                <li>
                  The index lives in memory and is rebuilt from PostgreSQL after
                  every restart.
                </li>
                <li>
                  Render Free can sleep. Initial requests can take a minute or
                  longer; the frontend waits up to two minutes and offers a
                  manual retry.
                </li>
                <li>
                  The public instance uses a curated 70-document corpus. It does
                  not contain the local benchmark datasets.
                </li>
                <li>
                  Public document creation, editing, and deletion are disabled
                  by deployment configuration.
                </li>
                <li>
                  Index synchronization with PostgreSQL is not fully atomic
                  across concurrent database transactions.
                </li>
                <li>
                  Phrase search is implemented in the Java engine, but is not
                  exposed by the REST API.
                </li>
                <li>
                  Local benchmark measurements exclude hosted HTTP and database
                  latency and are not production guarantees.
                </li>
              </ul>
              <p>
                Deployment: Vercel serves this frontend, Render runs Spring
                Boot, and Supabase hosts PostgreSQL.
              </p>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
