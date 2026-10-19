/**
 * The HTTP client the server calls the OpenL Studio REST API with, on the `fetch` of Node.js.
 *
 * A request names its path relative to the base URL, its query parameters, headers, cancellation signal, timeout and
 * the type of the answer it expects. An array parameter is sent as the key repeated for each value, the way Spring
 * reads it. A body that is not a string, a form or bytes is sent as JSON.
 *
 * A request fails with an {@link HttpError} whose `kind` tells why: an answer outside 2xx, which the error carries;
 * its timeout; its cancellation; or a network failure, with the code of the system error.
 *
 * The client calls OpenL Studio at the address Studio gave it. The `fetch` of Node.js ignores `HTTP_PROXY` unless
 * `NODE_USE_ENV_PROXY` or `--use-env-proxy` turns the proxy of the environment on; OpenL Studio starts the server
 * without `NODE_USE_ENV_PROXY`.
 */

import { trimChar } from "./text.js";

/** Query parameters; `null` and `undefined` values are left out, and an array repeats its key. */
type HttpParams = Record<string, unknown>;

/** Response headers by lower-case name. */
export type HttpHeaders = Record<string, string>;

interface HttpRequestOptions {
  params?: HttpParams;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /** Milliseconds the whole exchange may take; the default of the client when omitted. */
  timeout?: number;
  /** `arraybuffer` returns the raw bytes; otherwise a JSON answer is parsed and any other one returned as text. */
  responseType?: "arraybuffer";
}

export type HttpMethod = "get" | "post" | "put" | "patch" | "delete";

/** One request, as the transport sees it. */
export interface HttpRequest extends HttpRequestOptions {
  method: HttpMethod;
  /** Path relative to `baseURL`. */
  url: string;
  baseURL: string;
  headers: Record<string, string>;
  timeout: number;
  /** The body as it is sent. */
  data?: string | FormData | Uint8Array<ArrayBuffer>;
}

export interface HttpResponse<T = unknown> {
  status: number;
  data: T;
  headers: HttpHeaders;
}

/** What a transport answers before the body is decoded. */
export interface HttpRawResponse {
  status: number;
  headers: HttpHeaders;
  body: ArrayBuffer;
}

type HttpTransport = (request: HttpRequest) => Promise<HttpRawResponse>;

/** Why a request failed: an answer outside 2xx, its timeout, its cancellation, or a failure of the connection. */
type HttpFailure = "status" | "timeout" | "canceled" | "network";

/** A failed request: the answer of OpenL Studio when there is one. */
export class HttpError extends Error {
  constructor(
    message: string,
    readonly config: HttpRequest,
    readonly kind: HttpFailure,
    readonly response?: { status: number; data: unknown },
    /** The code of the system error behind a failure without an answer, such as `ECONNREFUSED`. */
    readonly code?: string,
  ) {
    super(message);
    this.name = "HttpError";
  }
}

/** True for an error of a request of {@link HttpClient}. */
export function isHttpError(error: unknown): error is HttpError {
  return error instanceof HttpError;
}

/** The failure of a request that got no answer, such as a refused connection. */
function networkError(error: unknown, request: HttpRequest): HttpError {
  return new HttpError(error instanceof Error ? error.message : String(error), request, "network");
}

/** True for a request that ran out of its time. */
export function isRequestTimeout(error: unknown): boolean {
  return isHttpError(error) && error.kind === "timeout";
}

/** True for a request of OpenL Studio answered with 404 (Not Found). */
export function isNotFoundError(error: unknown): boolean {
  return isHttpError(error) && error.response?.status === 404;
}

/** True for a request of OpenL Studio answered with 409 (Conflict). */
export function isConflictError(error: unknown): boolean {
  return isHttpError(error) && error.response?.status === 409;
}

interface HttpClientOptions {
  baseUrl: string;
  /** Milliseconds a request may take unless it names its own timeout. */
  timeout: number;
  headers?: Record<string, string>;
  /** Called with every failed request before it is thrown. */
  onError?: (error: HttpError) => void;
}

/** The full address of a request: the base URL, the path and the query. */
export function buildUrl(request: Pick<HttpRequest, "baseURL" | "url" | "params">): string {
  const base = trimChar(request.baseURL, "/", false);
  const url = new URL(`${base}/${request.url.startsWith("/") ? request.url.slice(1) : request.url}`);
  for (const [key, value] of Object.entries(request.params ?? {})) {
    for (const item of Array.isArray(value) ? value : [value]) {
      if (item !== undefined && item !== null) {
        url.searchParams.append(key, String(item));
      }
    }
  }
  return url.toString();
}

/** The codes of the system errors that are timeouts: of the connection, and of undici's own header and body waits. */
const TIMEOUT_CODES: ReadonlySet<string> = new Set([
  "ETIMEDOUT",
  "UND_ERR_CONNECT_TIMEOUT",
  "UND_ERR_HEADERS_TIMEOUT",
  "UND_ERR_BODY_TIMEOUT",
]);

/** Sends a request with `fetch`. */
const fetchTransport: HttpTransport = async (request) => {
  const deadline = AbortSignal.timeout(request.timeout);
  try {
    const response = await fetch(buildUrl(request), {
      method: request.method.toUpperCase(),
      headers: request.headers,
      body: request.data,
      signal: request.signal ? AbortSignal.any([request.signal, deadline]) : deadline,
    });
    return { status: response.status, headers: Object.fromEntries(response.headers), body: await response.arrayBuffer() };
  } catch (error) {
    if (request.signal?.aborted) {
      throw new HttpError("canceled", request, "canceled");
    }
    if (deadline.aborted) {
      throw new HttpError(`timeout of ${request.timeout}ms exceeded`, request, "timeout");
    }
    // The system error is the cause of the TypeError fetch throws; read its fields, whatever realm it comes from.
    const cause = (error as { cause?: { code?: unknown; message?: unknown } } | undefined)?.cause;
    const code = typeof cause?.code === "string" ? cause.code : undefined;
    const message = typeof cause?.message === "string" ? cause.message : String((error as Error)?.message ?? error);
    throw new HttpError(message, request, code && TIMEOUT_CODES.has(code) ? "timeout" : "network", undefined, code);
  }
};

function serializeBody(
  data: unknown,
  headers: Record<string, string>,
): string | FormData | Uint8Array<ArrayBuffer> | undefined {
  if (data === undefined || data === null) return undefined;
  if (data instanceof FormData) {
    // fetch writes the multipart type with its boundary itself.
    for (const name of Object.keys(headers)) {
      if (name.toLowerCase() === "content-type") delete headers[name];
    }
    return data;
  }
  if (typeof data === "string") return data;
  // Bytes are told apart by their shape, not their class, so that a Buffer of any realm is sent as bytes.
  if (ArrayBuffer.isView(data)) return new Uint8Array(data.buffer as ArrayBuffer, data.byteOffset, data.byteLength);
  return JSON.stringify(data);
}

/** A JSON text parsed, or the text itself when it is not JSON. */
export function parseJsonOrText(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function decodeBody(body: ArrayBuffer, responseType: HttpRequestOptions["responseType"]): unknown {
  if (responseType === "arraybuffer") return body;
  const text = new TextDecoder().decode(body);
  return text === "" ? undefined : parseJsonOrText(text);
}

export class HttpClient {
  /** Sends the requests; a test puts a mock here. */
  transport: HttpTransport = fetchTransport;

  constructor(private readonly options: HttpClientOptions) {}

  get<T = unknown>(url: string, options?: HttpRequestOptions): Promise<HttpResponse<T>> {
    return this.request<T>("get", url, undefined, options);
  }

  delete<T = unknown>(url: string, options?: HttpRequestOptions): Promise<HttpResponse<T>> {
    return this.request<T>("delete", url, undefined, options);
  }

  post<T = unknown>(url: string, data?: unknown, options?: HttpRequestOptions): Promise<HttpResponse<T>> {
    return this.request<T>("post", url, data, options);
  }

  put<T = unknown>(url: string, data?: unknown, options?: HttpRequestOptions): Promise<HttpResponse<T>> {
    return this.request<T>("put", url, data, options);
  }

  patch<T = unknown>(url: string, data?: unknown, options?: HttpRequestOptions): Promise<HttpResponse<T>> {
    return this.request<T>("patch", url, data, options);
  }

  private async request<T>(
    method: HttpMethod,
    url: string,
    data: unknown,
    options: HttpRequestOptions = {},
  ): Promise<HttpResponse<T>> {
    const headers = { ...this.options.headers, ...options.headers };
    const request: HttpRequest = {
      ...options,
      method,
      url,
      baseURL: this.options.baseUrl,
      headers,
      timeout: options.timeout ?? this.options.timeout,
      data: serializeBody(data, headers),
    };
    try {
      const raw = await this.transport(request);
      const decoded = decodeBody(raw.body, request.responseType);
      if (raw.status < 200 || raw.status >= 300) {
        throw new HttpError(`Request failed with status code ${raw.status}`, request, "status", {
          status: raw.status,
          data: decoded,
        });
      }
      return { status: raw.status, data: decoded as T, headers: raw.headers };
    } catch (error) {
      const failure = isHttpError(error) ? error : networkError(error, request);
      this.options.onError?.(failure);
      throw failure;
    }
  }
}
