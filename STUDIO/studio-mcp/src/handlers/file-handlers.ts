/**
 * Project-file tool handlers (BETA) — read, write, upload, delete, search, copy, and move files inside a project.
 */
import { createHash } from "node:crypto";
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { z } from "zod";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatListOf, type Column } from "../markdown-list.js";
import { formatCount } from "../text.js";
import { isEditedInPlace, sanitizeError } from "../utils.js";
import { isConflictError, isNotFoundError } from "../http.js";
import { downloadLink, INLINE_TEXT_BYTES, inlineText, uploadLink } from "../transfer-links.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";

/**
 * Where a search starts. Studio takes `from` only for an ANCESTORS search and walks a SUBTREE search from the root of
 * the project, matching `pattern` against paths relative to it: a SUBTREE search from a folder becomes a recursive
 * search of a pattern under that folder — its entries, or everything under it with `recursive` — so it lists the
 * folder rather than the top level of the project. A pattern of a name, such as `*.xlsx`, matches only the top level
 * of a path: with `recursive` it is searched for at any depth, as an agent means it, not the top level only.
 */
function searchStart(args: z.output<typeof schemas.searchProjectFilesSchema>): Pick<Types.FileSearchQuery, "pattern" | "recursive" | "from"> {
  const folder = startFolder(args);
  if (!folder) {
    const anyDepth = args.recursive && args.scope !== "ANCESTORS" && args.pattern !== undefined && !args.pattern.includes("/") &&
      !args.pattern.startsWith("**");
    return { pattern: anyDepth ? `**/${args.pattern}` : args.pattern, recursive: args.recursive, from: args.from };
  }
  const pattern = args.pattern?.replace(/^\/+/, "");
  const everything = pattern ? `**/${pattern}` : "**";
  const below = args.recursive ? everything : (pattern ?? "*");
  return { pattern: `${folder}/${below}`, recursive: true };
}

/** The folder a SUBTREE search starts from, without its leading and trailing slashes; nothing for the project root. */
function startFolder(args: z.output<typeof schemas.searchProjectFilesSchema>): string | undefined {
  // Split rather than matched, which a long run of slashes slows
  const folder = args.from?.split("/").filter(Boolean).join("/");
  return args.scope === "ANCESTORS" || !folder ? undefined : folder;
}

/**
 * Whether a match is an entry of the folder a search without `recursive` starts from. Studio searches under that
 * folder recursively, so a '**' pattern matches the entries of its nested folders too.
 */
function inStartFolder(args: z.output<typeof schemas.searchProjectFilesSchema>): (node: { path: string }) => boolean {
  const folder = startFolder(args);
  return folder === undefined || args.recursive
    ? () => true
    : ({ path }) => !path.slice(folder.length + 1).replace(/\/$/, "").includes("/");
}

/** The extensions of the workbooks OpenL reads rules from. */
const WORKBOOK_EXTENSIONS = ["xls", "xlsx", "xlsm"];

/**
 * The workbooks a content search covers but never reads, as a note: Studio reads each file as text, so the
 * text of a table cell is never found. Nothing when the search names no content or covers no workbook.
 */
async function workbooksNotSearched(
  client: OpenLClient,
  args: z.output<typeof schemas.searchProjectFilesSchema>,
  query: Types.FileSearchQuery,
): Promise<string | undefined> {
  // A pattern of files of another extension, such as '**/*.xml', covers no workbook
  const patternExtension = /\.([^./*?]+)$/.exec(args.pattern ?? "")?.[1].toLowerCase();
  if (!args.content || args.scope === "ANCESTORS" || args.type === "FOLDER"
    || (patternExtension !== undefined && !WORKBOOK_EXTENSIONS.includes(patternExtension))) {
    return undefined;
  }
  const asked = args.extensions?.map((extension) => extension.toLowerCase());
  const extensions = asked ? WORKBOOK_EXTENSIONS.filter((extension) => asked.includes(extension)) : WORKBOOK_EXTENSIONS;
  if (extensions.length === 0) {
    return undefined;
  }
  const workbooks = (await client.searchProjectFiles(args.projectId,
    { ...query, content: undefined, extensions, type: "FILE" }, { branch: args.branch, fields: "path" }))
    .filter(inStartFolder(args));
  return workbooks.length === 0
    ? undefined
    : `${formatCount(workbooks.length, "workbook")}: content is never matched in a table cell; `
      + "find the tables a text is in with openl_list_tables text";
}

export function registerFileHandlers(): void {
  registerTool({
    name: "read_project_file",
    title: "Read Project File",
    description:
      "Read a file of a project by its project-relative path. " +
      `A text file of up to ${INLINE_TEXT_BYTES} bytes — text/*, JSON, XML or YAML, as the Content-Type Studio answers says — comes inline, verbatim. ` +
      "Any other file, a text with bytes its charset does not decode too, and a folder as a ZIP archive, comes as a download LINK: JSON with the url, a ready `curl` command that saves it, the size, and where the file lives in its repository (repositoryId, repositoryPath, branch). " +
      "Run the command in a shell, so the bytes never enter the conversation; where Studio authenticates, the link needs no credential and works until expiresAt. An agent that has the repository itself can read repositoryPath there. " +
      "Read the TABLES of a workbook with openl_get_table. The answer tells the current 'version' of the file, beside the inline text or in the link JSON, which a replacement or a deletion of the file takes back.",
    schema: schemas.readProjectFileSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, path } = args;
      const at = { version: args.revision, branch: args.branch };
      // The content of an earlier commit replaces nothing: only the version of the working copy is told
      const current = (version: string | undefined): string | undefined => (args.revision ? undefined : version);
      const meta = isFolderPath(path) ? undefined : await client.getProjectFileMeta(projectId, path, at)
        .catch(async (error: unknown) => (isNotFoundError(error) ? noFileAt(client, projectId, path, at) : Promise.reject(error)));
      if (!meta || meta.type === "folder") {
        return formatResult(await downloadLink(client, { kind: "archive", projectId, path, ...at }),
          );
      }
      if (meta.size !== undefined && meta.size <= INLINE_TEXT_BYTES) {
        const text = inlineText(await client.readProjectFile(projectId, path, at));
        if (text !== undefined) {
          // The text stays verbatim: its version is told apart from it
          return { content: [{ type: "text", text }, ...versionNote(current(meta.version))] };
        }
      }
      const link = await downloadLink(client, { kind: "download", projectId, path, ...at }, meta.size);
      const version = current(meta.version);
      return formatResult(version === undefined ? link : { ...link, version });
    },
  });

  registerTool({
    name: "write_project_file",
    title: "Write Project File",
    description:
      "Create or replace a TEXT file of a project by its project-relative path, from UTF-8 'content'; send a file from disk — a workbook, an image, any binary or large file — by openl_create_upload_link instead. Without 'version' it creates a NEW file, its missing folders too unless createFolders is false, and writes nothing over a file at the path; with the 'version' of a file it replaces it. Content whose lines end with LF, written over a file whose lines end with CRLF, is written with CRLF, so a read and write back leaves the file unchanged. 'message' commits the write, with every pending change of the project, in a design repository. The change stays in the working copy of an opened project until openl_save_project; Studio commits it at once to a closed project, and writes it at once into the files of a project edited in place. Change the TABLES of a workbook with openl_update_table, openl_edit_table or openl_create_project_table.",
    schema: schemas.writeProjectFileSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const buffer = args.version === undefined
        ? Buffer.from(args.content, "utf-8")
        : await keepCrlf(client, args);

      // Studio answers a refusal with its own reason: a file changed since the read, a locked or protected project
      // Without a version the write creates the file, and with one it replaces it: Studio refuses anything else.
      const created = args.version === undefined;
      await client.writeProjectFile(args.projectId, args.path, buffer, {
        createFolders: args.createFolders,
        branch: args.branch,
        version: args.version,
      }).catch((error: unknown) => (args.version === undefined
        ? rethrowWhenFileExists(error, client, args.projectId, args.path, args.branch,
          `A file is already at '${args.path}', and nothing was written. To replace it, read it with openl_read_project_file and pass the 'version' it answers.`)
        : Promise.reject(error)));
      const branch = args.branch ? { branch: args.branch } : {};

      const commit = await commitWrite(client, args.projectId, args.message);
      // The version of the file written, which Studio takes as the digest of its content, for the next replacement
      const result = {
        action: created ? "created" : "overwritten",
        version: createHash("sha256").update(buffer).digest("hex"),
        committed: commit.committed,
        ...branch,
        ...(commit.failure ? { note: `Not committed (${commit.failure}): commit with openl_save_project.` } : {}),
      };

      return formatResult(result);
    },
  });

  registerTool({
    name: "create_upload_link",
    title: "Create Upload Link",
    description:
      "Get a link that uploads a file from disk into a project — a workbook, an image, any binary or large file — without its bytes passing through the conversation. It takes the target of the write as openl_write_project_file does: 'path', 'createFolders', 'branch' and the 'version' of a file it replaces; optionally the 'size' and hex 'sha256' of the local file, which Studio checks before it writes anything where it authenticates its clients. The answer holds the url, a ready `curl` command that sends the file named after the path from the current folder, and where the file lands in its repository (repositoryId, repositoryPath, branch): run the command, which needs curl 7.76 or newer, adjusting the local file name. Where Studio authenticates, the link needs no credential, works until expiresAt and writes ONCE: a failed upload needs a new link. The change stays in the working copy of an opened project until openl_save_project; Studio commits it at once to a closed project, and writes it at once into the files of a project edited in place.",
    schema: schemas.createUploadLinkSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      if (isFolderPath(args.path)) {
        throw new ProtocolError(ProtocolErrorCode.InvalidParams,
          `'${args.path}' names a folder: an upload link writes one file, so give the path of the file.`);
      }
      return formatResult(await uploadLink(client, {
        projectId: args.projectId,
        path: args.path,
        branch: args.branch,
        version: args.version,
        createFolders: args.createFolders,
        size: args.size,
        sha256: args.sha256,
      }));
    },
  });

  registerTool({
    name: "delete_project_file",
    title: "Delete Project File",
    description:
      "Delete a file or a folder of a project by its project-relative path; a file by the 'version' openl_read_project_file answered for it. Studio removes the references the project configuration holds to it. The change stays in the working copy of an opened project until openl_save_project; Studio commits it at once to a closed project, and writes it at once into the files of a project edited in place. Destructive.",
    schema: schemas.deleteProjectFileSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      if (args.version === undefined && await isFile(client, args.projectId, args.path, args.branch)) {
        throw new ProtocolError(ProtocolErrorCode.InvalidParams,
          `'${args.path}' is a file: read it with openl_read_project_file and pass the 'version' it answers to delete it.`);
      }
      await client.deleteProjectFile(args.projectId, args.path, {
        branch: args.branch,
        version: args.version,
      });

      return formatResult({ deleted: args.path });
    },
  });

  registerTool({
    name: "search_project_files",
    title: "Search Project Files",
    description:
      "Search the files and folders of a project by an ant-glob path 'pattern' (such as 'rules/**/*.xlsx'), 'extensions', 'type' and a case-insensitive 'content' substring. A content search looks in every folder, any other search in the top level only unless recursive=true; to list a folder, search 'from' it with no other criteria. Each match carries its path, type, size and lastModified, more with fields; the answer is paged by limit and offset.",
    schema: schemas.searchProjectFilesSchema,
    aliases: { query: "content" },
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (asked, client): Promise<ToolResponse> => {
      // A text is looked for in every folder unless the agent asks for the top level: the files that hold text are
      // seldom at the top of a project
      const args = { ...asked, recursive: asked.recursive ?? (asked.content !== undefined && asked.scope !== "ANCESTORS" ? true : undefined) };
      const { limit, offset } = args;

      // Undefined fields are dropped by JSON serialization, so an empty query
      // matches everything in scope.
      const query: Types.FileSearchQuery = {
        content: args.content,
        extensions: args.extensions,
        type: args.type,
        scope: args.scope,
        version: args.revision,
        ...searchStart(args),
      };

      // The backend file-search has no server-side paging (it returns the full
      // match set), so — like openl_list_deployments/openl_list_repositories — we
      // paginate the returned array client-side and report pagination metadata.
      const [found, notSearched] = await Promise.all([
        client.searchProjectFiles(args.projectId, query, {
          branch: args.branch,
          // A match names its file by its path, of which its name, folder and extension are parts; an ANCESTORS match
          // carries its content too
          fields: args.fields ?? (args.scope === "ANCESTORS" ? undefined : "path,type,size,lastModified"),
        }),
        workbooksNotSearched(client, args, query),
      ]);
      // An ANCESTORS search answers each file whole, up to 1 MB: a file larger than an inline text goes without it
      const results = found.filter(inStartFolder(args)).map((node) => (node.content !== undefined && Buffer.byteLength(node.content, "utf8") > INLINE_TEXT_BYTES
        ? { ...node, content: undefined, contentOmitted: true }
        : node));
      return textResult(args.scope === "ANCESTORS"
        ? ancestorFiles(results)
        : formatListOf(results, limit, offset, {
          noun: "matches", one: "match",
          columns: matchColumns(args.fields),
          notes: notSearched ? [`Not searched: ${notSearched}.`] : [],
        }));
    },
  });

  registerTool({
    name: "copy_project_file",
    title: "Copy Project File",
    description:
      "Copy a file of a project to a new project-relative path, creating its missing folders. A file already at destinationPath fails the call with 409: choose another path or delete that file first. The change stays in the working copy of an opened project until openl_save_project; Studio commits it at once to a closed project, and writes it at once into the files of a project edited in place.",
    schema: schemas.fileTransferSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: transferHandler("copy", "copied", " (copy has no overwrite option)"),
  });

  registerTool({
    name: "move_project_file",
    title: "Move or Rename Project File",
    description:
      "Move or rename a file of a project, creating the missing folders of the destination. A file already at the destination fails the call with 409. The change stays in the working copy of an opened project until openl_save_project; Studio commits it at once to a closed project, and writes it at once into the files of a project edited in place.",
    schema: schemas.fileTransferSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: transferHandler("move", "moved", ""),
  });
}

/** Whether a project-relative path names a folder: the project root, or a path ending in a slash. */
function isFolderPath(path: string): boolean {
  return path === "" || path === "/" || path.endsWith("/");
}

/** Whether a file is at the path; a folder, or nothing at all, is not one. */
async function isFile(client: OpenLClient, projectId: string, path: string, branch: string | undefined): Promise<boolean> {
  if (isFolderPath(path)) {
    return false;
  }
  // Studio answers the metadata of a file only
  return client.getProjectFileMeta(projectId, path, { branch })
    .then(() => true, (error: unknown) => (isNotFoundError(error) ? false : Promise.reject(error)));
}

/**
 * The refusal of a read of a path no file takes. The path may name a folder without the slash that ends one, or lead
 * with the folder of the project, as the project list answers it, where the path is relative to that folder: the
 * refusal names the path of the file then.
 */
async function noFileAt(
  client: OpenLClient,
  projectId: string,
  path: string,
  at: { version?: string; branch?: string },
): Promise<never> {
  const within = path.includes("/") ? path.slice(path.indexOf("/") + 1) : "";
  const found = within !== "" && await client.getProjectFileMeta(projectId, within, at).then(() => true, () => false);
  throw new ProtocolError(
    ProtocolErrorCode.InvalidParams,
    found
      ? `No file is at '${path}': a path is relative to the folder of the project, and the file is '${within}'.`
      : `No file is at '${path}'. A folder is read as a ZIP archive by a path that ends with '/': '${path}/'.`,
  );
}

/**
 * Rethrow the 409 of a write at `path` as `message` when a file is there: Studio tells that cause in no words of its
 * own. Any other 409 — a project another user locked, a file where a folder of the path should be — passes on with
 * the reason Studio gives.
 */
async function rethrowWhenFileExists(
  error: unknown,
  client: OpenLClient,
  projectId: string,
  path: string,
  branch: string | undefined,
  message: string,
): Promise<never> {
  if (isConflictError(error) && await isFile(client, projectId, path, branch)) {
    throw new ProtocolError(ProtocolErrorCode.InvalidRequest, message);
  }
  throw error;
}

/** The note beside the text of a file read inline: its version, which a replacement or a deletion names back. */
function versionNote(version: string | undefined): { type: "text"; text: string }[] {
  return version === undefined ? [] : [{
    type: "text",
    text: `version: ${version} — pass it as 'version' to openl_write_project_file or openl_delete_project_file to replace or delete this file.`,
  }];
}

/**
 * The text a write replaces a file with, its lines ending as the lines of the file do. A text that ends its lines with
 * LF only, written over a text file that ends them with CRLF, gets CRLF: an agent reading the file and writing the
 * same text back leaves it unchanged, although the CR characters of the read are invisible to it.
 *
 * A file that does not exist yet is written as the text is.
 */
async function keepCrlf(client: OpenLClient, args: z.output<typeof schemas.writeProjectFileSchema>): Promise<Buffer> {
  const text = args.content;
  const existing = text.includes("\n") && !text.includes("\r")
    ? await client.readProjectFile(args.projectId, args.path, { branch: args.branch })
      .then(({ data }) => data, (error: unknown) => (isNotFoundError(error) ? undefined : Promise.reject(error)))
    : undefined;
  return Buffer.from(existing && endsLinesWithCrlf(existing) ? text.replaceAll("\n", "\r\n") : text, "utf-8");
}

/** Whether the lines of a text end with CRLF, every one of them. */
function endsLinesWithCrlf(data: Buffer): boolean {
  let lineFeeds = 0;
  for (let i = data.indexOf(0x0a); i >= 0; i = data.indexOf(0x0a, i + 1)) {
    if (i === 0 || data[i - 1] !== 0x0d) {
      return false;
    }
    lineFeeds++;
  }
  return lineFeeds > 0;
}

/** Whether a write was committed, and why not when the commit failed. */
interface WriteCommit {
  committed: boolean;
  failure?: string;
}

/**
 * Commits a written file when the write has a commit message: saves the project, which creates a revision. Saving
 * commits all the pending changes of the project, not just this file, as OpenL has no commit of one file. Without a
 * message the write stays in the working copy.
 *
 * Studio commits a write to a closed project itself, and a project edited in place has nothing to commit.
 */
async function commitWrite(client: OpenLClient, projectId: string, message: string | undefined): Promise<WriteCommit> {
  // A project that cannot be read is taken for an opened one: the change is written either way
  const project = await client.getProject(projectId).catch(() => undefined);
  if (project !== undefined && isEditedInPlace(project)) {
    return { committed: false };
  }
  if (project?.status === "CLOSED") {
    return { committed: true };
  }
  if (!message) {
    return { committed: false };
  }
  try {
    await client.saveProject(projectId, message);
    return { committed: true };
  } catch (error) {
    // e.g. a 'local' repository (no Git): the file is already written to the working copy, so the write reports it
    // could not be committed instead of failing
    return { committed: false, failure: sanitizeError(error) };
  }
}

/**
 * The handler of the tool that copies or moves a project file: the two differ only in the verb, and in the note on a
 * destination that exists already.
 */
function transferHandler(
  action: "copy" | "move",
  done: string,
  conflictNote: string,
): (args: z.output<typeof schemas.fileTransferSchema>, client: OpenLClient) => Promise<ToolResponse> {
  return async (args, client) => {
    try {
      await client.transferProjectFile(
        action,
        args.projectId,
        { sourcePath: args.sourcePath, destinationPath: args.destinationPath },
        { branch: args.branch },
      );
    } catch (error) {
      await rethrowWhenFileExists(
        error, client, args.projectId, args.destinationPath, args.branch,
        `Cannot ${action} to '${args.destinationPath}': a file already exists there${conflictNote}. ` +
          `Choose a different destinationPath, or delete the existing file first with openl_delete_project_file.`
      );
    }
    return formatResult({ [done]: args.destinationPath });
  };
}

/** The columns of a table of matches: the fields asked for, or the path, the type, the size and the date. */
function matchColumns(fields: string | undefined): Array<Column<Types.FsNode>> {
  const names = (fields ?? "path,type,size,lastModified").split(",").map((name) => name.trim()).filter(Boolean);
  return names.map((name) => ({ header: name, value: (node) => (node as unknown as Record<string, unknown>)[name] }));
}

/** The files an ANCESTORS search finds, from the folder searched up to the repository root, each with its content. */
function ancestorFiles(files: Array<Types.FsNode & { contentOmitted?: boolean }>): string {
  if (files.length === 0) {
    return "No file of that name above the folder.";
  }
  return files.map((file) => {
    const content = file.contentOmitted
      ? `The content is over ${INLINE_TEXT_BYTES} bytes: read it with openl_read_project_file.`
      : file.content ?? "";
    return `## ${file.path}\n\n${content}`;
  }).join("\n\n");
}
