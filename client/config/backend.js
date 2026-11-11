export function resolveDevApiTarget(env) {
  const configured = (
    env.PORT ||
    env.VITE_KESTREL_API_URL ||
    "http://localhost:8080"
  ).trim();
  const target = /^\d+$/.test(configured)
    ? `http://localhost:${configured}`
    : configured;
  let url;
  try {
    url = new URL(target);
  } catch {
    throw new Error(
      "PORT must be a backend port (e.g. 8080) or an HTTP(S) origin (e.g. http://localhost:8080).",
    );
  }
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.pathname !== "/" ||
    url.search ||
    url.hash
  ) {
    throw new Error(
      "The backend target must be an HTTP(S) origin without credentials, paths, or query parameters.",
    );
  }
  if (
    /^\d+$/.test(configured) &&
    (Number(configured) < 1 || Number(configured) > 65535)
  ) {
    throw new Error("The backend port must be between 1 and 65535.");
  }
  return url.origin;
}
