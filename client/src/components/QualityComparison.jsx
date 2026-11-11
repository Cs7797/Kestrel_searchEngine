import { motion } from "motion/react";
export const BASELINE_SOURCE =
  "https://castorini.github.io/pyserini/2cr/beir.html";
const datasets = [
  { name: "SciDocs", kestrel: 0.1614, baseline: 0.158 },
  { name: "SciFact", kestrel: 0.6291, baseline: 0.665 },
  { name: "NFCorpus", kestrel: 0.3186, baseline: 0.325 },
  { name: "FiQA", kestrel: 0.2528, baseline: 0.236 },
];
export default function QualityComparison() {
  return (
    <div className="quality-comparison">
      <div className="chart-legend">
        <span>
          <i />
          Kestrel · local
        </span>
        <span>
          <i className="baseline-swatch" />
          Anserini BM25 multifield · published
        </span>
      </div>
      <p className="comparison-unit">
        NDCG@10 · higher is better · shared 0–1 scale
      </p>
      <div
        className="comparison-chart"
        aria-label="NDCG at 10 on the same BEIR test datasets"
      >
        {datasets.map((row) => (
          <div className="comparison-dataset" key={row.name}>
            <h4>{row.name}</h4>
            {[
              ["Kestrel", row.kestrel],
              ["Anserini", row.baseline],
            ].map(([name, value]) => (
              <div className="comparison-row" key={name}>
                <span>{name}</span>
                <div className="bar-track">
                  <motion.div
                    className={`bar-fill ${name === "Anserini" ? "baseline-fill" : ""}`}
                    initial={false}
                    animate={{ width: `${value * 100}%` }}
                    transition={{ duration: 0.3 }}
                  />
                </div>
                <strong>{value.toFixed(name === "Anserini" ? 3 : 4)}</strong>
              </div>
            ))}
          </div>
        ))}
        <div className="chart-axis">
          <span>0</span>
          <span>NDCG@10</span>
          <span>1.0</span>
        </div>
      </div>
      <div className="comparison-source">
        <p>
          Published reference:{" "}
          <a href={BASELINE_SOURCE} target="_blank" rel="noopener noreferrer">
            Pyserini BEIR reproductions — BM25 MF ↗
          </a>
          . Baseline values are published to three decimal places; checked
          November 11, 2026.
        </p>
        <details>
          <summary>How to read this comparison</summary>
          <p>
            Both series use the corresponding BEIR test datasets and NDCG@10.
            Kestrel’s author supplied the local results; the Anserini/Lucene
            baseline is reported by Pyserini and was not rerun here. Pyserini’s
            multifield recipe weights title and contents equally (1.0 each).
            Kestrel uses title weight 2.0, k₁ = 1.5, and b = 0.75. Analyzer,
            stemming, stopwords, BM25 defaults, and tie handling can differ.
            Kestrel uses linear relevance gains, consistent with trec_eval’s
            convention. Treat these as reference scores, not a controlled
            head-to-head experiment or a statistical significance claim.
          </p>
        </details>
      </div>
    </div>
  );
}
