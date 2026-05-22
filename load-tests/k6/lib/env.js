export function envUrl(name, fallback) {
  const raw = (__ENV[name] || '').trim();
  const url = raw.length > 0 ? raw : fallback;
  return url.endsWith('/') ? url.slice(0, -1) : url;
}
