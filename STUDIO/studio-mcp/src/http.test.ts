/**
 * Unit tests for http.ts: the fetch transport against a local HTTP server.
 */

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { once } from "node:events";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { AddressInfo } from "node:net";
import { buildUrl, HttpClient, HttpError } from "./http.js";

const PROXY_VARIABLES = ["HTTP_PROXY", "http_proxy", "HTTPS_PROXY", "https_proxy", "NO_PROXY", "no_proxy"];

interface Received {
  method?: string;
  url?: string;
  headers: IncomingMessage["headers"];
  body: string;
}

describe("buildUrl", () => {
  it("repeats the key of an array, leaves out empty values, and keeps the context path", () => {
    const url = buildUrl({
      baseURL: "http://studio:8080/webstudio/rest/",
      url: "/projects",
      params: { include: ["status", "descriptor"], page: 0, branch: undefined, tag: null, flag: true },
    });
    expect(url).toBe("http://studio:8080/webstudio/rest/projects?include=status&include=descriptor&page=0&flag=true");
  });
});

describe("HttpClient over fetch", () => {
  const saved: Record<string, string | undefined> = {};
  let server: Server;
  let received: Received[];
  let answer: (req: IncomingMessage, res: ServerResponse) => void;

  const client = (onError?: (error: HttpError) => void): HttpClient => new HttpClient({
    baseUrl: `http://127.0.0.1:${(server.address() as AddressInfo).port}/rest`,
    timeout: 2000,
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    onError,
  });

  beforeEach(async () => {
    for (const name of PROXY_VARIABLES) {
      saved[name] = process.env[name];
      delete process.env[name];
    }
    received = [];
    answer = (_req, res) => {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end("[]");
    };
    server = createServer((req, res) => {
      const chunks: Buffer[] = [];
      req.on("data", (chunk: Buffer) => chunks.push(chunk));
      req.on("end", () => {
        received.push({ method: req.method, url: req.url, headers: req.headers, body: Buffer.concat(chunks).toString() });
        answer(req, res);
      });
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
  });

  afterEach(async () => {
    for (const name of PROXY_VARIABLES) {
      if (saved[name] === undefined) delete process.env[name];
      else process.env[name] = saved[name];
    }
    server.closeAllConnections();
    await server[Symbol.asyncDispose]();
  });

  it("reaches OpenL Studio directly when HTTP_PROXY names a proxy", async () => {
    // A proxy that answers nothing: a request routed through it would fail instead of reaching the server.
    process.env.HTTP_PROXY = "http://127.0.0.1:9";
    process.env.http_proxy = "http://127.0.0.1:9";

    await expect(client().get("/repos").then((response) => response.data)).resolves.toEqual([]);
    expect(received.map((request) => `${request.method} ${request.url}`)).toEqual(["GET /rest/repos"]);
  });

  it("sends JSON bodies as JSON and a form as multipart with its boundary", async () => {
    const http = client();
    await http.post("/projects", { name: "Demo" });
    const form = new FormData();
    form.append("template", new Blob([new Uint8Array([80, 75])], { type: "application/zip" }), "template.zip");
    form.append("comment", "Hi");
    await http.put("/repos/design/projects/Demo", form);

    expect(received[0].headers["content-type"]).toBe("application/json");
    expect(JSON.parse(received[0].body)).toEqual({ name: "Demo" });
    expect(received[1].headers["content-type"]).toMatch(/^multipart\/form-data; boundary=/);
    expect(received[1].body).toContain('filename="template.zip"');
    expect(received[1].body).toContain("Hi");
  });

  it("returns the headers by lower-case name, and the raw bytes when asked for them", async () => {
    answer = (_req, res) => {
      res.writeHead(200, { "Content-Type": "application/octet-stream", "Content-Disposition": "attachment" });
      res.end(Buffer.from([1, 2, 3]));
    };

    const response = await client().get<ArrayBuffer>("/files/model.xlsx", { responseType: "arraybuffer" });

    expect(response.headers).toMatchObject({ "content-type": "application/octet-stream", "content-disposition": "attachment" });
    expect([...new Uint8Array(response.data)]).toEqual([1, 2, 3]);
  });

  it("returns an answer that is not JSON as its text", async () => {
    answer = (_req, res) => {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end("not json");
    };

    await expect(client().get("/repos").then((response) => response.data)).resolves.toBe("not json");
  });

  it("fails an answer outside 2xx with the status and the parsed body, and reports it", async () => {
    answer = (_req, res) => {
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ message: "The project is not found." }));
    };
    const reported: HttpError[] = [];

    const failure = await client((error) => reported.push(error))
      .get("/projects/missing")
      .catch((error: unknown) => error);

    expect(failure).toBeInstanceOf(HttpError);
    expect(failure).toMatchObject({ kind: "status", code: undefined });
    expect((failure as HttpError).response).toEqual({ status: 404, data: { message: "The project is not found." } });
    expect(reported).toEqual([failure]);
  });

  it("fails a request for bytes outside 2xx with the parsed body of the refusal", async () => {
    answer = (_req, res) => {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ message: "The formula of B2 cannot be read." }));
    };

    const failure = await client()
      .post("/merge/conflicts/workbook", { decisions: [] }, { responseType: "arraybuffer" })
      .catch((error: unknown) => error);

    expect((failure as HttpError).response).toEqual({
      status: 400,
      data: { message: "The formula of B2 cannot be read." },
    });
  });

  it("fails as a timeout when the answer takes longer than the timeout", async () => {
    answer = () => { /* never answers */ };

    await expect(client().get("/slow", { timeout: 100 })).rejects.toMatchObject({
      kind: "timeout",
      message: "timeout of 100ms exceeded",
    });
  });

  it("fails as canceled when the caller cancels", async () => {
    answer = () => { /* never answers */ };
    await expect(client().get("/slow", { signal: AbortSignal.timeout(50) })).rejects.toMatchObject({ kind: "canceled" });
  });

  it("fails with the system code when OpenL Studio does not answer at all", async () => {
    const closed = createServer().listen(0, "127.0.0.1");
    await once(closed, "listening");
    const port = (closed.address() as AddressInfo).port;
    await closed[Symbol.asyncDispose]();
    const http = new HttpClient({ baseUrl: `http://127.0.0.1:${port}/rest`, timeout: 2000 });

    await expect(http.get("/repos")).rejects.toMatchObject({ kind: "network", code: "ECONNREFUSED" });
  });
});
