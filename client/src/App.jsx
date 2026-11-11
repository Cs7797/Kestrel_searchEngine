import { useState } from "react";
import { motion } from "motion/react";
import {
  ArrowRight,
  ArrowUpRight,
  Github,
  Menu,
  X,
  Command,
  Search,
  Linkedin,
} from "lucide-react";
import ThemeToggle from "./components/ThemeToggle";
import SearchConsole, { statusLabels } from "./components/SearchConsole";
import { Architecture, Benchmarks, HowItWorks } from "./components/Engineering";
import Documentation, { REPO } from "./components/Documentation";
import { API_URL } from "./lib/api";
const links = [
  ["Search", "search"],
  ["Benchmarks", "benchmarks"],
  ["How it works", "how-it-works"],
  ["Architecture", "architecture"],
  ["Docs", "docs"],
];
function Brand() {
  return (
    <a href="#" className="brand" aria-label="Kestrel home">
      <img src="/kestrel.svg" alt="" width="31" height="31" />
      kestrel<span className="brand-dot">.</span>
    </a>
  );
}
export default function App() {
  const [menu, setMenu] = useState(false);
  const [status, setStatus] = useState(API_URL ? "unchecked" : "unconfigured");
  return (
    <>
      <a className="skip-link" href="#search">
        Skip to search
      </a>
      <header className="site-header">
        <div className="nav-inner">
          <Brand />
          <nav className="desktop-nav" aria-label="Main navigation">
            {links.map(([name, id]) => (
              <a key={id} href={`#${id}`}>
                {name}
              </a>
            ))}
          </nav>
          <div className="nav-actions">
            <ThemeToggle />
            <a
              className="github-icon"
              href={REPO}
              aria-label="Kestrel on GitHub"
              target="_blank"
              rel="noopener noreferrer"
            >
              <Github size={20} />
            </a>
            <a className="button small primary" href="#search">
              Try Search <ArrowUpRight size={15} />
            </a>
            <button
              className="menu-toggle"
              aria-label={menu ? "Close navigation" : "Open navigation"}
              aria-expanded={menu}
              aria-controls="mobile-nav"
              onClick={() => setMenu(!menu)}
            >
              {menu ? <X /> : <Menu />}
            </button>
          </div>
        </div>
        <motion.nav
          id="mobile-nav"
          aria-label="Mobile navigation"
          className="mobile-nav"
          initial={false}
          animate={{ height: menu ? "auto" : 0, opacity: menu ? 1 : 0 }}
          inert={!menu}
        >
          {links.map(([name, id]) => (
            <a key={id} href={`#${id}`} onClick={() => setMenu(false)}>
              {name}
              <ArrowUpRight size={16} />
            </a>
          ))}
        </motion.nav>
      </header>
      <main>
        <section className="hero">
          <div className="hero-grid" aria-hidden="true" />
          <div className="hero-content">
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45 }}
            >
              <div className="hero-eyebrow">
                <span className="tiny-square" /> INDEPENDENT ENGINEERING. FROM
                THE GROUND UP.
              </div>
              <h1>
                Search, built from
                <br />
                <span>first principles.</span>
              </h1>
              <p className="hero-description">
                A Java-powered search engine. Custom indexing, BM25 ranking,
                <br className="desktop-break" /> and concurrent queries. Every
                layer, built to be understood.
              </p>
              <div className="hero-buttons">
                <a className="button primary" href="#search">
                  Try Kestrel <ArrowRight size={17} />
                </a>
                <a className="button secondary" href="#architecture">
                  Explore architecture <ArrowUpRight size={17} />
                </a>
              </div>
              <div className="hero-meta">
                <span className={`status status-${status}`}>
                  <i />
                  {statusLabels[status]}
                </span>
                <span className="meta-divider" />
                <a href={REPO} target="_blank" rel="noopener noreferrer">
                  <Github size={14} /> Open source, start to finish{" "}
                  <ArrowUpRight size={12} />
                </a>
              </div>
            </motion.div>
            <motion.div
              className="index-visual"
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15, duration: 0.5 }}
              aria-label="Illustrative positional index for a document titled Java search"
            >
              <div className="index-visual-head">
                <span>
                  <Command size={13} /> INDEX EXPLORER
                </span>
                <span className="violet">ILLUSTRATION</span>
              </div>
              <div className="source-line">
                <span>doc-01 / title</span>
                <p>
                  <b>Java</b> <b>search</b>
                </p>
              </div>
              <div className="index-connector">
                <span />
                normalize → map → store
                <span />
              </div>
              <div className="index-table">
                <div>
                  <span>TERM</span>
                  <span>DOCUMENT</span>
                  <span>POSITION</span>
                </div>
                <div>
                  <strong>java</strong>
                  <span>doc-01</span>
                  <code>[0]</code>
                </div>
                <div>
                  <strong>search</strong>
                  <span>doc-01</span>
                  <code>[1]</code>
                </div>
              </div>
              <div className="visual-foot">
                <LayersIcon /> term → document → positions
              </div>
            </motion.div>
          </div>
          <div className="stack-strip">
            <span>BUILT ON SOLID GROUND</span>
            <div>
              Java <b>21</b>
            </div>
            <div>Spring Boot</div>
            <div>PostgreSQL</div>
            <div>Docker</div>
            <div>GitHub Actions</div>
          </div>
        </section>
        <div className="content-wrap">
          <section className="playground-section">
            <div className="playground-title">
              <div className="eyebrow">
                <Search size={14} /> THE LIVE EXPERIENCE
              </div>
              <span>Small corpus. Real search engine.</span>
            </div>
            <SearchConsole setApiStatus={setStatus} />
          </section>
          <Benchmarks />
          <HowItWorks />
          <Architecture />
          <Documentation />
          <motion.section
            className="about-section"
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.2 }}
          >
            <div>
              <div className="eyebrow">
                BUILT TO UNDERSTAND, NOT JUST TO USE.
              </div>
              <h2>
                Good engineering
                <br />
                starts with “how?”
              </h2>
            </div>
            <div>
              <p>
                Kestrel is a self-built exploration of information retrieval:
                Java collections and data structures, ranking algorithms,
                indexing, API design, concurrency, persistence, migrations,
                automated testing, containers, and cloud deployment.
              </p>
              <p>
                Built by Chetan Seth. Open source for anyone who wants to look
                closer.
              </p>
              <a
                href={REPO}
                target="_blank"
                rel="noopener noreferrer"
                className="button secondary"
              >
                <Github size={17} /> Explore the source{" "}
                <ArrowUpRight size={16} />
              </a>
              <div className="profile-links">
                <a
                  href="https://www.linkedin.com/in/chetan-seth-b3b266302/"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Linkedin size={19} /> LinkedIn <ArrowUpRight size={15} />
                </a>
                <a
                  href="https://www.github.com/cs7797"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Github size={19} /> GitHub profile <ArrowUpRight size={15} />
                </a>
              </div>
            </div>
          </motion.section>
        </div>
      </main>
      <footer>
        <Brand />
        <span>Search, built from first principles.</span>
        <a
          href="https://www.linkedin.com/in/chetan-seth-b3b266302/"
          target="_blank"
          rel="noopener noreferrer"
        >
          LinkedIn <ArrowUpRight size={14} />
        </a>
        <a
          href="https://www.github.com/cs7797"
          target="_blank"
          rel="noopener noreferrer"
        >
          GitHub profile <ArrowUpRight size={14} />
        </a>
        <a href={REPO} target="_blank" rel="noopener noreferrer">
          View on GitHub <ArrowUpRight size={14} />
        </a>
        <a href="#">Back to top ↑</a>
      </footer>
    </>
  );
}
function LayersIcon() {
  return (
    <svg
      width="14"
      height="14"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path d="m8 2 6 3-6 3-6-3 6-3Zm-6 6 6 3 6-3M2 11l6 3 6-3" />
    </svg>
  );
}
