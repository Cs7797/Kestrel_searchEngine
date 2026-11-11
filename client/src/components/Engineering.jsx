import { useEffect, useRef, useState } from "react";
import QualityComparison from "./QualityComparison";
import { animate, motion, useInView, useReducedMotion } from "motion/react";
import {
  ArrowRight,
  Braces,
  Database,
  Globe,
  Layers,
  Server,
  GitBranch,
  Box,
  ArrowUpRight,
} from "lucide-react";
export function SectionHeading({ number, eyebrow, title, description }) {
  return (
    <motion.div
      className="section-heading"
      initial={{ opacity: 0, y: 12 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.35 }}
    >
      <div className="eyebrow">
        <span>{number}</span> {eyebrow}
      </div>
      <h2>{title}</h2>
      <p>{description}</p>
    </motion.div>
  );
}
const stages = [
  [
    "Tokenize",
    "Java makes searching simpler.",
    "java · makes · searching · simpler",
    "Split on non-alphanumeric characters and lowercase using Locale.ROOT.",
  ],
  [
    "Normalize",
    "searching → search",
    "java · make · search · simpler",
    "Apply English stemming. Ranked queries remove stopwords and duplicate terms; positional indexing preserves stopwords.",
  ],
  [
    "Index",
    "term → document → positions",
    "search → doc-01 → [2]",
    "Separate title and body indexes store postings in Java maps. Adjacent positions support phrase search inside the engine.",
  ],
  [
    "Rank",
    "score = 2 × title + body",
    "BM25 · k₁ = 1.5 · b = 0.75",
    "Score term frequency, document frequency, and field length. Title scores carry a default weight of 2.0.",
  ],
  [
    "Read concurrently",
    "ReentrantReadWriteLock",
    "parallel reads · exclusive writes",
    "Queries share a read lock; index mutations hold the write lock. Phrase search is not exposed by the REST API.",
  ],
];
export function HowItWorks() {
  const [active, setActive] = useState(0);
  return (
    <section className="section" id="how-it-works">
      <SectionHeading
        number="02"
        eyebrow="UNDER THE HOOD"
        title="No black boxes. Just engineering."
        description="From a string of text to a ranked answer. Every step implemented in Java, with data structures you can inspect."
      />
      <div className="pipeline">
        <div
          className="pipeline-tabs"
          role="tablist"
          aria-label="Search processing stages"
        >
          {stages.map((s, i) => (
            <button
              role="tab"
              aria-selected={active === i}
              aria-controls="pipeline-panel"
              id={`stage-${i}`}
              key={s[0]}
              className={active === i ? "active" : ""}
              onClick={() => setActive(i)}
              onKeyDown={(e) => {
                if (
                  ["ArrowRight", "ArrowLeft", "Home", "End"].includes(e.key)
                ) {
                  e.preventDefault();
                  const next =
                    e.key === "Home"
                      ? 0
                      : e.key === "End"
                        ? stages.length - 1
                        : (active +
                            (e.key === "ArrowRight" ? 1 : -1) +
                            stages.length) %
                          stages.length;
                  setActive(next);
                  document.getElementById(`stage-${next}`)?.focus();
                }
              }}
              tabIndex={active === i ? 0 : -1}
            >
              <span>0{i + 1}</span>
              {s[0]}
              <ArrowRight size={15} />
            </button>
          ))}
        </div>
        <div
          className="pipeline-panel"
          role="tabpanel"
          id="pipeline-panel"
          aria-labelledby={`stage-${active}`}
        >
          <div className="code-label">
            <Braces size={15} /> AN ILLUSTRATIVE WALKTHROUGH
          </div>
          <motion.div
            key={active}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <h3>{stages[active][1]}</h3>
            <div className="token-line">{stages[active][2]}</div>
            <p>{stages[active][3]}</p>
          </motion.div>
          <span className="code-foot">
            Built from Java collections. No external search engine.
          </span>
        </div>
      </div>
    </section>
  );
}
const nodes = [
  {
    icon: Globe,
    title: "React frontend",
    tag: "VERCEL",
    text: "The browser sends an encoded keyword query over HTTPS to the REST API.",
  },
  {
    icon: Server,
    title: "Spring Boot API",
    tag: "RENDER",
    text: "SearchController validates the request through KestrelService. Detailed results hydrate ranked IDs from PostgreSQL.",
  },
  {
    icon: Layers,
    title: "Positional index",
    tag: "JAVA MEMORY",
    text: "Field-specific maps look up candidate documents and token positions under a shared read lock.",
  },
  {
    icon: Braces,
    title: "BM25 ranking",
    tag: "CUSTOM ENGINE",
    text: "Field-aware BM25 scores produce ranked IDs. The API returns up to 20 document previews to the browser.",
  },
];
export function Architecture() {
  const [selected, setSelected] = useState(1);
  return (
    <section className="section" id="architecture">
      <SectionHeading
        number="03"
        eyebrow="SYSTEM ARCHITECTURE"
        title="Persistent by design. Fast in memory."
        description="One Spring Boot application. A purpose-built index. PostgreSQL as the source of truth."
      />
      <div className="architecture-grid">
        {nodes.map((n, i) => (
          <button
            key={n.title}
            className={`arch-node ${selected === i ? "selected" : ""}`}
            aria-pressed={selected === i}
            onClick={() => setSelected(i)}
          >
            <n.icon size={23} />
            <span className="mono">{n.tag}</span>
            <h3>{n.title}</h3>
            {i < 3 && <ArrowRight className="node-arrow" size={19} />}
          </button>
        ))}
      </div>
      <div className="architecture-detail" aria-live="polite">
        <span className="violet">
          0{selected + 1} / {nodes[selected].title}
        </span>
        <p>{nodes[selected].text}</p>
        <ArrowUpRight size={19} />
      </div>
      <div className="infra-row">
        <div>
          <Database size={23} />
          <div>
            <h3>PostgreSQL / Supabase</h3>
            <p>
              Documents persist in the database. At startup, IndexInitializer
              rebuilds the in-memory index from stored documents.
            </p>
          </div>
        </div>
        <div>
          <GitBranch size={23} />
          <div>
            <h3>Validated. Packaged. Deployed.</h3>
            <p>
              GitHub Actions runs unit and PostgreSQL integration checks. Docker
              packages the Java backend.
            </p>
          </div>
          <Box size={22} />
        </div>
      </div>
    </section>
  );
}
function Counter({ value, decimals = 0 }) {
  const ref = useRef(null);
  const inView = useInView(ref, { once: true });
  const reduced = useReducedMotion();
  const [display, setDisplay] = useState(value);
  useEffect(() => {
    if (!inView || reduced) return;
    const animation = animate(0, value, {
      duration: 0.65,
      onUpdate: setDisplay,
    });
    return () => animation.stop();
  }, [inView, reduced, value]);
  return (
    <span ref={ref}>
      <span aria-hidden="true">
        {display.toLocaleString("en-US", {
          minimumFractionDigits: decimals,
          maximumFractionDigits: decimals,
        })}
      </span>
      <span className="sr-only">
        {value.toLocaleString("en-US", {
          minimumFractionDigits: decimals,
          maximumFractionDigits: decimals,
        })}
      </span>
    </span>
  );
}
export function Benchmarks() {
  const [mode, setMode] = useState("quality");
  const values =
    mode === "throughput"
      ? [
          ["1 thread", 67.63],
          ["2 threads", 109.36],
          ["4 threads", 215.33],
          ["8 threads", 350.58],
        ]
      : [
          ["SciDocs", 0.1614],
          ["SciFact", 0.6291],
          ["NFCorpus", 0.3186],
          ["FiQA", 0.2528],
        ];
  return (
    <section className="section" id="benchmarks">
      <SectionHeading
        number="01"
        eyebrow="MEASURED, NOT ASSUMED"
        title="Built with curiosity. Backed by numbers."
        description="Local experiments on real retrieval datasets. These figures measure the Java engine, not the hosted demo or its network latency."
      />
      <div className="metrics">
        <div>
          <span className="metric-value">
            <Counter value={25657} />
          </span>
          <span>SciDocs documents indexed</span>
        </div>
        <div>
          <span className="metric-value">
            <Counter value={350.58} decimals={2} />
            <small> QPS</small>
          </span>
          <span title="Queries completed per second in the local eight-thread run.">
            Throughput at 8 threads
          </span>
        </div>
        <div>
          <span className="metric-value">
            <Counter value={5.18} decimals={2} />
            <small>×</small>
          </span>
          <span>Throughput vs. 1 thread</span>
        </div>
        <div>
          <span className="metric-value">
            <Counter value={46.8} decimals={2} />
            <small> ms</small>
          </span>
          <span title="95% of measured queries completed within this latency.">
            P95 latency at 8 threads
          </span>
        </div>
      </div>
      <div className="benchmark-panel">
        <div className="chart-header">
          <div>
            <span className="eyebrow">LOCAL BENCHMARKS</span>
            <h3>
              {mode === "throughput"
                ? "Concurrency that makes a difference."
                : "Kestrel alongside an established baseline."}
            </h3>
          </div>
          <div className="segmented" aria-label="Benchmark chart">
            <button
              aria-pressed={mode === "throughput"}
              onClick={() => setMode("throughput")}
            >
              Throughput
            </button>
            <button
              aria-pressed={mode === "quality"}
              onClick={() => setMode("quality")}
            >
              Search quality
            </button>
          </div>
        </div>
        {mode === "quality" ? (
          <QualityComparison />
        ) : (
          <div
            className="bar-chart"
            role="img"
            aria-label={
              mode === "throughput"
                ? "Local queries per second: 1 thread 67.63, 2 threads 109.36, 4 threads 215.33, 8 threads 350.58"
                : "Local NDCG at 10: SciDocs 0.1614, SciFact 0.6291, NFCorpus 0.3186, FiQA 0.2528"
            }
          >
            {values.map(([name, value]) => (
              <div className="bar-row" key={name}>
                <span>{name}</span>
                <div className="bar-track">
                  <motion.div
                    initial={false}
                    animate={{
                      width: `${(value / (mode === "throughput" ? 400 : 1)) * 100}%`,
                    }}
                    transition={{ duration: 0.35 }}
                    className="bar-fill"
                  />
                </div>
                <strong>
                  {mode === "throughput" ? value.toFixed(2) : value.toFixed(4)}
                </strong>
              </div>
            ))}
            <div className="chart-axis">
              <span>0</span>
              <span>
                {mode === "throughput" ? "QUERIES / SECOND" : "NDCG@10"}
              </span>
              <span>{mode === "throughput" ? "400" : "1.0"}</span>
            </div>
          </div>
        )}
        <p className="chart-note">
          {mode === "throughput"
            ? "QPS = queries per second. SciDocs corpus, 200 JVM warm-up queries and 400 searches per thread configuration. P95 is the 95th percentile of query latency."
            : "NDCG@10 measures graded relevance with rank discount in the first ten results. Compare against baselines on the same dataset, not absolute quality across datasets. SciDocs: 1,000 queries, Precision@10 0.0829, Recall@10 0.1681."}
        </p>
      </div>
      <p className="fineprint">
        Local experimental results supplied by the project author. Machine, JVM,
        and workload dependent. The public demo uses a separate 70-document
        corpus.
      </p>
    </section>
  );
}
