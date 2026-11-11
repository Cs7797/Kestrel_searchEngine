# Kestrel frontend

React, JavaScript, Vite, Tailwind CSS, Motion, and Lucide. A responsive, read-only interface to the original Java engine. No generated or mock search results. The index explorer is explicitly an educational illustration.

## Run locally

Use Node.js 22.12+ (Node 22 LTS recommended) and npm.

```powershell
cd D:\Kestrel\client
npm ci
npm run dev
```

Before running Vite, create `client/.env.local` (ignored by Git):

```dotenv
PORT=http://localhost:8080
VITE_KESTREL_API_URL=https://kestrel-search.onrender.com
```

Open http://127.0.0.1:5173. For local development, `PORT=http://localhost:8080` (or `PORT=8080`) selects the backend reached by the read-only Vite proxy. This is the backend target, not the Vite listening port. Start Spring Boot separately on that port. If PORT is absent, the proxy uses VITE_KESTREL_API_URL, then defaults to localhost:8080.

For production builds, `VITE_KESTREL_API_URL=https://kestrel-search.onrender.com` remains the public API origin; PORT does not replace it in the built frontend. Configure the exact deployed frontend origin in Render's backend CORS settings when hosting. Restart Vite after changing environment values. Never put database credentials in frontend configuration.

```powershell
npm test
npm run build
npm run preview
```

Tests supply a public example API origin and replace fetch; no live service is contacted by unit tests. The production build emits `client/dist`. Maven remains independent of this folder.

## Live demo and backend integration

The read-only frontend is deployed at [kestrel-search-engine.vercel.app](https://kestrel-search-engine.vercel.app/), using a curated 70-document demonstration corpus separate from the local benchmark datasets.

The inspected working tree exposes `GET /api/search/results?q=...` returning an array of `{id, title, snippet, url, rank}`. The existing service returns up to 20 results and truncates content previews to 160 characters. `url` may be null. Phrase search exists only at engine level; quoted queries are not a supported REST phrase mode.

Verified on November 11, 2026: the Render detailed-results endpoint returns HTTP 200 with real results and `Access-Control-Allow-Origin: https://kestrel-search-engine.vercel.app`. Local development uses a GET-only Vite proxy. For an older backend returning 404 for the detailed route, the adapter can fetch real ranked IDs and their documents (up to 20, four concurrent reads), preserving backend order. No mock results are used.

`FrontendCorsConfig.java` is the only backend integration addition: exact origins, GET-only access to `/api/**` and `/actuator/health`, no credentials, no wildcards. Defaults allow `http://localhost:5173`, `http://127.0.0.1:5173`, and `https://kestrel-search-engine.vercel.app`. The environment setting below replaces these defaults when set. Origins must not have a trailing slash. If Vite chooses another port, use port 5173 or explicitly add that origin. For `npm run preview`, add port 4173 temporarily.

For Render, configure:

```text
KESTREL_WRITES_ENABLED=false
KESTREL_FRONTEND_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://kestrel-search-engine.vercel.app
```

Update the final origin if the Vercel production domain changes. Do not use wildcards for preview deployments. CORS does not enforce read-only access for nonbrowser clients: the existing server-side writes flag must be false on the public deployment. No Java algorithms, migrations, existing tests, Docker files, or workflows were changed.

## Vercel deployment (when ready)

1. Import the GitHub repository into Vercel.
2. Set **Root Directory** to `client` and framework to **Vite**.
3. Select Node.js **22.x**. Install command: `npm ci`. Build command: `npm run build`. Output directory: `dist`.
4. Add `VITE_KESTREL_API_URL=https://kestrel-search.onrender.com` for the desired environments.
5. Deploy. Add the assigned exact HTTPS origin to Render's `KESTREL_FRONTEND_ORIGINS` and redeploy the backend with the detailed-search endpoint and CORS class.
6. Check health and execute `java concurrency` from the deployed frontend. Confirm real titles, snippets, and ranks appear. Confirm public writes remain disabled.

`vercel.json` supports SPA fallback and security headers. Navigation currently uses same-page anchors, so every section has a directly linkable hash without an additional router dependency. Backend hosting stays on Render; PostgreSQL stays on Supabase.

## Behavior and accessibility

- A single initial health request reports observed reachability; there is no uptime polling.
- Search uses URL encoding, runtime shape validation, AbortController cancellation, and a 120-second timeout. New searches cancel older searches. A request still waiting after 5 seconds shows the Render wake-up explanation. Retry is manual.
- No browser writes or admin controls. Only HTTP(S) result links are rendered.
- Semantic forms, visible focus, skip link, live result announcements, keyboard-operable tabs, mobile navigation, and reduced-motion support.
- Chart numbers come from the supplied local measurements, not API telemetry. Benchmarks are distinct from the public 70-document corpus.
- Font family: DM Sans with IBM Plex Mono via Google Fonts, with system fallbacks. Visual direction references Render's restrained dark surfaces, typography, borders, and technical diagrams; all branding and SVG artwork are original.

## Structure

The header includes a keyboard-accessible light-mode switch. Themes use semantic colors in `src/theme.css`, and the chosen theme is saved locally when browser storage is available. Body copy is 16–18 px, with larger technical labels and stronger borders in both themes.

Benchmark results follow the live experience. The default search-quality view pairs Kestrel's local NDCG@10 with the published **Anserini/Lucene BM25 multifield** reference from [Pyserini's BEIR reproductions](https://castorini.github.io/pyserini/2cr/beir.html), checked November 11, 2026: SciDocs 0.158, SciFact 0.665, NFCorpus 0.325, FiQA 0.236. Baselines are published to three decimal places and were not rerun locally. The chart uses a shared 0–1 axis. The expandable methodology note explains equal title/body weighting in the reference and Kestrel's 2.0 title weight, plus analyzer and ranking differences; these are contextual reference scores, not a controlled head-to-head experiment.

- `src/App.jsx`: shell, navigation, hero, about, footer.
- `src/components/SearchConsole.jsx`: real search and lifecycle states.
- `src/components/Engineering.jsx`: search stages, architecture, benchmarks.
- `src/components/Documentation.jsx`: source-verified docs and copy controls.
- `src/lib/api.js`: configuration, requests, response validation, safe links.
- `src/styles.css`: visual system and responsive layouts.

Deployment links and complete backend setup are documented in the [main README](../Readme.md).
