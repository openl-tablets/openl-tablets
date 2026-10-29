/**
 * Links that move a file of a project between the machine of an agent and OpenL Studio without its bytes passing
 * through the model: the agent runs the `curl` command of the link. The design is
 * `Docs/architecture/mcp-file-transfer.md`.
 *
 * Where Studio authenticates its clients, the link is a transfer link Studio issues: short-lived, its id the only
 * credential, an upload one used once. In single-user mode, where nobody is authenticated, the link is the address
 * of the REST API itself.
 */
import { entityTag, type OpenLClient } from "./client.js";
import { trimChar } from "./text.js";
import { IN_PLACE_NOTE, isEditedInPlace } from "./utils.js";
import type * as Types from "./types.js";

/** The most bytes of a text file a tool answers inline; a larger or a binary file is answered with a link. */
export const INLINE_TEXT_BYTES = 5000;

/** What a download link moves. */
type DownloadTarget =
  | { kind: "download" | "archive"; projectId: string; path: string; branch?: string; version?: string }
  | {
    kind: "conflictVersion";
    projectId: string;
    /** The path of the conflicted file in the repository, as the merge conflict names it. */
    path: string;
    side: "base" | "ours" | "theirs";
    /** The branch of the side, which holds that version; none for the base. */
    branch?: string;
  };

/** Where an upload link writes, and what the file must be. */
interface UploadTarget {
  projectId: string;
  path: string;
  branch?: string;
  /** The version of the file the upload replaces; none for an upload that creates a file. */
  version?: string;
  createFolders: boolean;
  size?: number;
  sha256?: string;
}

/**
 * The text of a file a tool answers inline: a file of up to {@link INLINE_TEXT_BYTES} bytes whose content type is
 * text — `text/*`, or a JSON, XML or YAML type — decoded in the charset the type names, or UTF-8. Anything else is
 * answered with a link, whatever its bytes look like, and gets `undefined`.
 *
 * The text keeps a byte order mark, and bytes the charset does not decode send the file by a link too: an agent
 * writing back a text that lost them would change the file.
 */
export function inlineText(file: Types.ProjectFileResponse): string | undefined {
  if (file.data.length > INLINE_TEXT_BYTES || !isTextType(file.contentType)) {
    return undefined;
  }
  const charset = /charset=["']?([\w-]+)/i.exec(file.contentType)?.[1] ?? "utf-8";
  try {
    return new TextDecoder(charset, { fatal: true, ignoreBOM: true }).decode(file.data);
  } catch {
    // A charset the runtime does not know, or bytes it does not decode
    return undefined;
  }
}

function isTextType(contentType: string): boolean {
  // The media type without its parameters: `text/plain` of `text/plain; charset=utf-8`
  const type = contentType.split(";")[0].trim();
  return type.startsWith("text/") || /^application\/([\w.+-]+\+)?(json|xml|yaml|x-yaml)$/.test(type);
}

/**
 * A link that downloads the target, with its size when the tool knows it, where the file lives in its repository and
 * a `curl` command to run.
 */
export async function downloadLink(
  client: OpenLClient,
  target: DownloadTarget,
  size?: number,
): Promise<Record<string, unknown>> {
  const [{ url, expiresAt }, location] = await Promise.all([
    linkOf(client, target, () => singleUserDownloadUrl(client, target)),
    locationOf(client, target.projectId, target.path, target.branch, target.kind === "conflictVersion"),
  ]);
  return {
    url,
    method: "GET",
    ...(expiresAt ? { expiresAt } : {}),
    ...(size === undefined ? {} : { size }),
    ...location,
    curl: `curl -fsS -o ${quote(localName(target))} ${quote(url)}`,
    note: expiresAt
      ? "Run curl to save the file; the link works until it expires, a cut download is fetched again with it."
      : "Run curl to save the file.",
  };
}

/**
 * A link that uploads a file to the target, with where the file lands in its repository and a `curl` command.
 *
 * An upload naming the version of a file replaces it while it has that version, and creates a file otherwise: Studio
 * writes nothing over a file changed since the read, nor over a file the agent did not read.
 */
export async function uploadLink(client: OpenLClient, target: UploadTarget): Promise<Record<string, unknown>> {
  const { version, ...where } = target;
  const local = target.path.split("/").at(-1) ?? target.path;
  const [{ url, expiresAt }, project] = await Promise.all([
    linkOf(client, {
      kind: "upload",
      ...where,
      ...(version === undefined ? { conflictPolicy: "FAIL" } : { expectedVersion: version }),
    }, () => client.projectFileUrl(target.projectId, target.path, version === undefined
      ? { createFolders: String(target.createFolders), conflictPolicy: "FAIL", branch: target.branch }
      : { branch: target.branch })),
    client.getProject(target.projectId),
  ]);
  // A transfer link takes one PUT; the REST API creates a file with a POST and replaces one with a PUT
  const method = expiresAt || version !== undefined ? "PUT" : "POST";
  const ifMatch = version === undefined ? undefined : "If-Match: " + entityTag(version);
  const replacing = ifMatch === undefined ? "" : `-X PUT -H ${quote(ifMatch)} `;
  // An upload prints the answer of Studio on a failure: why it refused the file, which -f would hide
  const curl = expiresAt
    ? `curl -sS --fail-with-body -T ${quote(local)} ${quote(url)}`
    : `curl -sS --fail-with-body ${replacing}--data-binary @${quote(local)} -H 'Content-Type: application/octet-stream' ${quote(url)}`;
  return {
    url,
    method,
    ...(expiresAt ? { expiresAt } : {}),
    ...locationIn(project, target.path, target.branch),
    curl,
    note: (expiresAt ? "The link writes once: a failed upload needs a new link. " : "")
      + (isEditedInPlace(project)
        ? IN_PLACE_NOTE
        : "The file lands in the project working copy; commit it with openl_save_project."),
  };
}

/**
 * The address of a link and when it expires: a transfer link of Studio where Studio authenticates, the address of
 * the REST API itself, which never expires, in single-user mode.
 */
async function linkOf(
  client: OpenLClient,
  request: Types.TransferLinkRequest,
  singleUserUrl: () => string,
): Promise<{ url: string; expiresAt?: string }> {
  if (!(await client.getSettings()).userMode) {
    return { url: singleUserUrl() };
  }
  const issued = await client.createTransferLink(request);
  return { url: new URL(issued.path, client.publicUrl).href, expiresAt: issued.expiresAt };
}

function singleUserDownloadUrl(client: OpenLClient, target: DownloadTarget): string {
  if (target.kind === "conflictVersion") {
    return client.conflictFileUrl(target.projectId, target.path, target.side);
  }
  return client.projectFileUrl(target.projectId, target.path, {
    branch: target.branch,
    version: target.version,
    download: target.kind === "archive" ? "true" : undefined,
  });
}

/**
 * Where a file of a project lives in its repository: the repository, the path of the file inside it — the folder of
 * the project followed by the project-relative path — and the branch, the one the project is on unless another is
 * named. The path of a conflicted file is already a path in the repository, on the branch of its side, and the base
 * version is on no branch.
 */
async function locationOf(
  client: OpenLClient,
  projectId: string,
  path: string,
  branch: string | undefined,
  inRepository = false,
): Promise<Record<string, unknown>> {
  return locationIn(await client.getProject(projectId), path, branch, inRepository);
}

/** Where a file of the given project lives in its repository; see {@link locationOf}. */
function locationIn(
  project: Types.ProjectViewModel,
  path: string,
  branch: string | undefined,
  inRepository = false,
): Record<string, unknown> {
  const folder = inRepository ? undefined : project.path && trimChar(project.path, "/", false);
  // A folder keeps its trailing slash in the repository path
  const relative = path.replace(/^\/+/, "");
  let repositoryPath = relative;
  if (folder) {
    repositoryPath = relative ? `${folder}/${relative}` : `${folder}/`;
  }
  const onBranch = inRepository ? branch : branch ?? project.branch;
  return {
    ...(project.repository ? { repositoryId: project.repository } : {}),
    repositoryPath,
    ...(onBranch ? { branch: onBranch } : {}),
  };
}

/** The name the downloaded file is saved under: the name of the file, or of the folder with `.zip`. */
function localName(target: DownloadTarget): string {
  const name = trimChar(target.path, "/", false).split("/").at(-1) ?? "";
  if (target.kind === "archive") {
    return `${name || "project"}.zip`;
  }
  return name || "file";
}

/** A single quote inside a single-quoted word of a POSIX shell: close the quotes, an escaped quote, reopen them. */
const QUOTED_QUOTE = String.raw`'\''`;

/** A word for a POSIX shell, in single quotes. */
function quote(word: string): string {
  return `'${word.replaceAll("'", QUOTED_QUOTE)}'`;
}
