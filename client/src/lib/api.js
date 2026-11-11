export const API_URL = (import.meta.env.VITE_KESTREL_API_URL ?? "")
  .trim()
  .replace(/\/+$/, "");
export const SERVER_UNAVAILABLE_MESSAGE =
  "The search server might be temporarily down or restarting. Please give it a few minutes, then retry.";
function responseError(status, action) {
  return new Error(
    status >= 500 && status <= 599
      ? SERVER_UNAVAILABLE_MESSAGE
      : `${action} returned HTTP ${status}. Please try again.`,
  );
}
export function apiUrl(path) {
  if (!API_URL)
    throw new Error(
      "The API URL is not configured. Set VITE_KESTREL_API_URL and rebuild the frontend.",
    );
  const base = new URL(API_URL);
  if (!["https:", "http:"].includes(base.protocol))
    throw new Error("The API URL must use HTTP or HTTPS.");
  if (window.location.protocol === "https:" && base.protocol !== "https:")
    throw new Error("This secure frontend requires an HTTPS API URL.");
  // Vite forwards local requests so the browser does not need the remote service's CORS policy.
  if (import.meta.env.DEV) return `/kestrel-api${path}`;
  return `${API_URL}${path}`;
}
export function parseResults(value) {
  if (
    !Array.isArray(value) ||
    !value.every(
      (r) =>
        r &&
        typeof r.id === "string" &&
        typeof r.title === "string" &&
        typeof r.snippet === "string" &&
        (r.url === null || typeof r.url === "string") &&
        Number.isInteger(r.rank) &&
        r.rank > 0,
    )
  ) {
    throw new Error(
      "The API returned an unexpected result format. Confirm the backend includes GET /api/search/results.",
    );
  }
  return value;
}
export async function search(query, signal) {
  const response = await fetch(
    apiUrl(`/api/search/results?q=${encodeURIComponent(query)}`),
    { signal, headers: { Accept: "application/json" } },
  );
  if (response.status === 404) return searchWithDocumentReads(query, signal);
  if (!response.ok) throw responseError(response.status, "Search");
  return parseResults(await response.json());
}

// Compatibility with the verified deployed API: ranking still comes entirely from Kestrel.
async function searchWithDocumentReads(query, signal) {
  const options = { signal, headers: { Accept: "application/json" } };
  const rankedResponse = await fetch(
    apiUrl(`/api/search/ranked?q=${encodeURIComponent(query)}`),
    options,
  );
  if (!rankedResponse.ok)
    throw responseError(rankedResponse.status, "Ranked search");
  const ids = await rankedResponse.json();
  if (!Array.isArray(ids) || !ids.every((id) => typeof id === "string"))
    throw new Error("The API returned an unexpected ranked-ID format.");
  const topIds = ids.slice(0, 20);
  const results = new Array(topIds.length);
  let next = 0;
  await Promise.all(
    Array.from({ length: Math.min(4, topIds.length) }, async () => {
      while (next < topIds.length) {
        const index = next++;
        const id = topIds[index];
        const response = await fetch(
          apiUrl(`/api/documents/${encodeURIComponent(id)}`),
          options,
        );
        if (!response.ok)
          throw responseError(response.status, "Document request");
        const doc = await response.json();
        if (
          doc.id !== id ||
          typeof doc.title !== "string" ||
          typeof doc.content !== "string" ||
          !(doc.url == null || typeof doc.url === "string")
        )
          throw new Error("The API returned an unexpected document format.");
        const content = doc.content.replace(/\s+/g, " ").trim();
        results[index] = {
          id,
          title: doc.title,
          snippet:
            content.length > 160 ? `${content.slice(0, 160)}...` : content,
          url: doc.url ?? null,
          rank: index + 1,
        };
      }
    }),
  );
  return parseResults(results);
}
export function safeExternalUrl(value) {
  if (!value) return undefined;
  try {
    const u = new URL(value);
    return ["https:", "http:"].includes(u.protocol) ? u.href : undefined;
  } catch {
    return undefined;
  }
}
