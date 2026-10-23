/**
 * A mock of the transport of {@link HttpClient}: the tests register the answers, and no request leaves the process.
 *
 * `onGet(url, { params })` matches a read by its path — a string compared without its leading slash, or a regular
 * expression — and its query parameters; `onPost(url, body)` and the other writes match the body sent, parsed when it
 * is JSON. A handler registered again for the same request replaces the earlier one, unless it answers once. A request
 * no handler matches is answered with 404, but for the short status of a project, which says the project is current.
 * `history` keeps every request, by method, but that answer. The short status is a read of the project itself, so
 * only a handler naming its query answers it.
 */
import { isDeepStrictEqual } from "node:util";
import {
  buildUrl,
  HttpError,
  type HttpClient,
  type HttpHeaders,
  type HttpMethod,
  type HttpRawResponse,
  type HttpRequest,
  parseJsonOrText,
} from "../http.js";
import { STATUS_SUMMARY_QUERY } from "../client.js";

type ReplyHeaders = Record<string, string>;
type Reply = [number, unknown?, ReplyHeaders?];
type ReplyFunction = (config: HttpRequest) => Reply | Promise<Reply>;
type Responder = (config: HttpRequest) => Promise<HttpRawResponse>;
type Matcher = string | RegExp;

interface Handler {
  method: HttpMethod;
  url: Matcher;
  match: unknown;
  once: boolean;
  respond: Responder;
}

function withoutLeadingSlash(url: string): string {
  return url.startsWith("/") ? url.slice(1) : url;
}

function urlMatches(matcher: Matcher, request: HttpRequest): boolean {
  return matcher instanceof RegExp
    ? matcher.test(request.url)
    : withoutLeadingSlash(request.url) === withoutLeadingSlash(matcher);
}

/** Plain data as JSON sees it: keys holding `undefined` are left out, at any depth. */
function normalized(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(normalized);
  }
  if (value === null || typeof value !== "object" || Object.getPrototypeOf(value) !== Object.prototype) {
    return value;
  }
  return Object.fromEntries(Object.entries(value)
    .filter(([, field]) => field !== undefined)
    .map(([key, field]) => [key, normalized(field)]));
}

function valueMatches(actual: unknown, expected: unknown): boolean {
  if (expected === undefined) return true;
  const asymmetric = expected as { asymmetricMatch?: (value: unknown) => boolean };
  if (typeof asymmetric.asymmetricMatch === "function") return asymmetric.asymmetricMatch(actual);
  return isDeepStrictEqual(normalized(actual), normalized(expected));
}

/** The body a request sends, parsed when it is JSON. */
export function bodyOf(request: HttpRequest): any {
  return typeof request.data === "string" ? parseJsonOrText(request.data) : request.data;
}

function requestMatches(handler: Handler, request: HttpRequest): boolean {
  if (handler.method !== request.method) return false;
  if (!urlMatches(handler.url, request)) return false;
  if (request.method === "get") {
    const params = (handler.match as { params?: unknown } | undefined)?.params;
    // The short status reads the project itself: only a handler naming its query answers it, not one of the project
    if (params === undefined && isShortStatus(request)) return false;
    return valueMatches(request.params, params);
  }
  return valueMatches(bodyOf(request), handler.match);
}

/** The short status of a project: `GET /projects/:id` with the {@link STATUS_SUMMARY_QUERY}. */
function isShortStatus(request: HttpRequest): boolean {
  return request.method === "get"
    && /^\/?projects\/[^/]+$/.test(request.url)
    && valueMatches(request.params, STATUS_SUMMARY_QUERY);
}

function sameHandler(a: Handler, b: Handler): boolean {
  return a.method === b.method && String(a.url) === String(b.url) && isDeepStrictEqual(a.match, b.match);
}

function encode(data: unknown): ArrayBuffer {
  if (data === undefined || data === null) return new ArrayBuffer(0);
  if (data instanceof ArrayBuffer) return data;
  if (data instanceof Uint8Array) {
    return data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) as ArrayBuffer;
  }
  return encode(new TextEncoder().encode(typeof data === "string" ? data : JSON.stringify(data)));
}

function lowerCaseHeaders(headers: ReplyHeaders | undefined): HttpHeaders {
  return Object.fromEntries(Object.entries(headers ?? {}).map(([name, value]) => [name.toLowerCase(), value]));
}

/** What a handler answers. Its methods return the mock, so that registrations chain. */
class MockHandler {
  constructor(
    private readonly mock: MockHttp,
    private readonly method: HttpMethod,
    private readonly url: Matcher,
    private readonly match: unknown,
  ) {}

  reply(statusOrFunction: number | ReplyFunction, data?: unknown, headers?: ReplyHeaders): MockHttp {
    return this.add(false, statusOrFunction, data, headers);
  }

  replyOnce(statusOrFunction: number | ReplyFunction, data?: unknown, headers?: ReplyHeaders): MockHttp {
    return this.add(true, statusOrFunction, data, headers);
  }

  /** Fails the request the way a timeout of the client does. */
  timeout(): MockHttp {
    return this.register(false, (config) =>
      Promise.reject(new HttpError(`timeout of ${config.timeout}ms exceeded`, config, "timeout")));
  }

  private add(once: boolean, statusOrFunction: number | ReplyFunction, data?: unknown, headers?: ReplyHeaders): MockHttp {
    return this.register(once, async (config) => {
      const [status, body, replyHeaders] = typeof statusOrFunction === "function"
        ? await statusOrFunction(config)
        : [statusOrFunction, data, headers];
      return { status, headers: lowerCaseHeaders(replyHeaders), body: encode(body) };
    });
  }

  private register(once: boolean, respond: Responder): MockHttp {
    this.mock.addHandler({ method: this.method, url: this.url, match: this.match, once, respond });
    return this.mock;
  }
}

/** The query a request sends, as Studio reads it: every value a string, `null` and `undefined` left out. */
export function queryOf(request: HttpRequest): Record<string, string> {
  return Object.fromEntries(new URL(buildUrl(request)).searchParams);
}

export class MockHttp {
  /** Every request sent, by method. */
  readonly history: Record<HttpMethod, HttpRequest[]> = { get: [], post: [], put: [], patch: [], delete: [] };
  private handlers: Handler[] = [];
  /** Takes over the transport of the HTTP client an `OpenLClient` holds. */
  constructor(client: object) {
    (client as { http: HttpClient }).http.transport = (request) => this.handle(request);
  }

  onGet(url: Matcher, match?: { params?: unknown }): MockHandler {
    return new MockHandler(this, "get", url, match);
  }

  onDelete(url: Matcher): MockHandler {
    return new MockHandler(this, "delete", url, undefined);
  }

  onPost(url: Matcher, body?: unknown): MockHandler {
    return new MockHandler(this, "post", url, body);
  }

  onPut(url: Matcher, body?: unknown): MockHandler {
    return new MockHandler(this, "put", url, body);
  }

  onPatch(url: Matcher, body?: unknown): MockHandler {
    return new MockHandler(this, "patch", url, body);
  }

  /** Every request sent, whatever its method. */
  get requests(): HttpRequest[] {
    return Object.values(this.history).flat();
  }

  /** Forgets every handler and the history. */
  reset(): void {
    this.handlers = [];
    for (const requests of Object.values(this.history)) {
      requests.length = 0;
    }
  }

  addHandler(handler: Handler): void {
    const existing = handler.once ? -1 : this.handlers.findIndex((known) => !known.once && sameHandler(known, handler));
    if (existing >= 0) {
      this.handlers[existing] = handler;
    } else {
      this.handlers.push(handler);
    }
  }

  private async handle(request: HttpRequest): Promise<HttpRawResponse> {
    const index = this.handlers.findIndex((handler) => requestMatches(handler, request));
    if (index < 0 && isShortStatus(request)) {
      // A project no test said anything of is current. The tools that work with what Studio compiled ask it before
      // each call; the answer is left out of the history, so that a test counts the requests the tool itself makes.
      return {
        status: 200,
        headers: { "content-type": "application/json" },
        body: encode({ compileStatus: { compileState: "ok" } }),
      };
    }
    this.history[request.method].push(request);
    if (request.signal?.aborted) {
      throw new HttpError("canceled", request, "canceled");
    }
    if (index < 0) {
      return { status: 404, headers: {}, body: new ArrayBuffer(0) };
    }
    const handler = this.handlers[index];
    if (handler.once) {
      this.handlers.splice(index, 1);
    }
    return handler.respond(request);
  }
}
