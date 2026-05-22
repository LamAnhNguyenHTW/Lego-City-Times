export type ApiErrorKind = 'network' | 'timeout' | 'server' | 'notfound' | 'http';

export class ApiError extends Error {
  kind: ApiErrorKind;
  status?: number;

  constructor(kind: ApiErrorKind, message: string, status?: number) {
    super(message);
    this.kind = kind;
    this.status = status;
  }
}

export function describeApiError(err: unknown): string {
  if (err instanceof ApiError) {
    switch (err.kind) {
      case 'network':
        return 'Der Server ist gerade nicht erreichbar. Bitte versuche es in einem Moment erneut.';
      case 'timeout':
        return 'Die Anfrage hat zu lange gedauert. Der Server antwortet aktuell nicht.';
      case 'server':
        return 'Der Server hat einen internen Fehler gemeldet (möglicherweise ist die Datenbank nicht erreichbar).';
      case 'notfound':
        return 'Die angeforderten Daten wurden nicht gefunden.';
      case 'http':
        return `Die Anfrage wurde abgewiesen (Status ${err.status ?? '?'}).`;
    }
  }
  return 'Unbekannter Fehler bei der Kommunikation mit dem Server.';
}

interface ApiFetchOptions extends RequestInit {
  timeoutMs?: number;
}

export async function apiFetch(input: string, init: ApiFetchOptions = {}): Promise<Response> {
  const { timeoutMs = 8000, ...rest } = init;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  let res: Response;
  try {
    res = await fetch(input, { ...rest, signal: controller.signal });
  } catch (err: unknown) {
    clearTimeout(timer);
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new ApiError('timeout', `Timeout nach ${timeoutMs} ms: ${input}`);
    }
    throw new ApiError('network', `Netzwerkfehler bei ${input}`);
  }
  clearTimeout(timer);

  if (res.status === 404) {
    throw new ApiError('notfound', `Nicht gefunden: ${input}`, 404);
  }
  if (res.status >= 500) {
    throw new ApiError('server', `Serverfehler ${res.status} bei ${input}`, res.status);
  }
  if (!res.ok) {
    throw new ApiError('http', `HTTP ${res.status} bei ${input}`, res.status);
  }

  return res;
}

export async function apiGetJson<T>(input: string, init?: ApiFetchOptions): Promise<T> {
  const res = await apiFetch(input, init);
  return (await res.json()) as T;
}
