/**
 * Project-file tool handlers (BETA) — read, write, delete, search, copy, and
 * move files inside an opened project.
 */
import type { z } from "zod";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatPage } from "../formatters.js";
import { sanitizeError } from "../utils.js";
import { RESPONSE_LIMITS } from "../constants.js";
import { isNotFoundError, parseJsonOrText } from "../http.js";
import { compactBase64, normalizeMimeType, sliceContent } from "../content-utils.js";
import { registerTool, formatResult, rethrowConflictAsActionable, textResult, type ToolResponse } from "./common.js";

export function registerFileHandlers(): void {
  registerTool({
    name: "read_project_file",
    title: "Read Project File",
    description:
      "Read any file in a project by its project-relative path — text or binary, and folder listings too. Maps to GET /projects/{projectId}/files/{path}. Behavior by path/params: " +
      "(1) a FILE path returns its content — UTF-8 text is returned verbatim, while binary is returned as base64 'content' in a JSON text envelope with byte and MIME metadata (use encoding to force 'utf-8' or binary; default 'auto' detects); " +
      "(2) a FILE path with view='meta' returns JSON metadata (name, size, extension, lastModified); " +
      "(3) a FOLDER path (empty string for the root, or a path ending in '/') lists its entries (use recursive, viewMode FLAT/NESTED, extensions, namePattern, foldersOnly); " +
      "(4) a FOLDER path with download=true returns a ZIP as base64 in the same JSON text envelope. " +
      "Optional 'version' reads a historical revision; 'branch' pins the project branch. Optional byte range (offset/length) is applied client-side AFTER fetching the whole file (the backend does not support partial transfers), so the entire file is loaded into memory; for very large/binary files, bound the RETURNED size with offset/length and read in chunks (a full file's base64 can exceed MCP message limits); a UTF-8 chunk keeps its characters whole, so its ends may move up to 3 bytes forward. A text chunk arrives in a JSON envelope with byteLength, the range it covers, nextOffset while the file goes on, and lineEnding 'CRLF' when its lines end with CR LF. Use this to read AGENTS.md, README.md, schemas, manifests, or to inspect/export xlsx rule files.",
    schema: schemas.readProjectFileSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const path = args.path;
      const { data, contentType, contentDisposition } = await client.readProjectFile(args.projectId, path, args);

      if (isListing(contentType, contentDisposition)) {
        return formatResult(parseJsonOrText(data.toString("utf-8")), args.response_format);
      }

      // File (or folder-ZIP) content. Apply the optional client-side byte range,
      // then choose text or a base64 JSON envelope.
      const total = data.length;
      const { start, end, slice, asBase64 } = sliceContent(data, {
        offset: args.offset,
        length: args.length,
        encoding: args.download ? "base64" : args.encoding,
      });
      const ranged = start !== 0 || end !== total;

      if (!asBase64 && !ranged) {
        // Text content: returned (near-)verbatim — most useful for docs/schemas/
        // manifests (response_format does not apply). The verbatim path would
        // otherwise bypass the response-size cap, so when the content exceeds the
        // 25K limit we truncate and append a continuation cursor (next byte offset)
        // so the caller can page the rest with offset/length. No more is decoded than
        // the cap can return: a character takes four bytes at most.
        let text = slice.subarray(0, 4 * RESPONSE_LIMITS.MAX_CHARACTERS).toString("utf-8");
        if (text.length > RESPONSE_LIMITS.MAX_CHARACTERS) {
          text = text.slice(0, RESPONSE_LIMITS.MAX_CHARACTERS);
          const nextOffset = start + Buffer.byteLength(text, "utf-8");
          text += `\n\n${RESPONSE_LIMITS.TRUNCATION_MESSAGE} Returned bytes ${start}–${nextOffset} of ${total}; continue with offset=${nextOffset}.`;
        }
        return textResult(text);
      }

      // Binary bytes, and a chunk of text, go in a JSON envelope telling where they are in the file, where the next
      // chunk starts and, for text, how its lines end. Although MCP permits arbitrary binary BlobResourceContents,
      // several clients route every embedded blob through their image renderer and reject XLSX/ZIP/octet-stream data
      // as an unsupported image. TextContent is the interoperable lossless transport for non-display binary files.
      // Keep it untruncated; callers can bound/page bytes with offset/length.
      const encoding = asBase64 ? "base64" : "utf-8";
      return formatResult({
        ...fileMetadata(path, args.version, contentType, encoding, total, start, end),
        ...(!asBase64 && endsLinesWithCrlf(slice) ? { lineEnding: "CRLF" } : {}),
        content: slice.toString(encoding),
      }, "json", { skipTruncation: true });
    },
  });

  registerTool({
    name: "write_project_file",
    title: "Write Project File",
    description:
      "Create or replace a file in a project by its project-relative path. Provide UTF-8 text in 'content'. For binary files such as xlsx/images/zip, provide base64 bytes in 'blob'; its advertised JSON Schema uses contentEncoding='base64'. The legacy content + encoding='base64' form remains accepted. " +
      "COMMIT: pass 'message' to commit the write to Git (a new revision is created); omit 'message' and the write stays in the project WORKING COPY (commit it later with openl_save_project). Committing saves ALL pending project changes and works only for design repositories (not 'local'). " +
      "By default missing parent folders are created (createFolders=true). If the target file already EXISTS, behavior follows conflictPolicy: FAIL (default) returns an error; OVERWRITE replaces the file in place (text 'content' whose lines end with LF, written over a text file whose lines end with CRLF, is written with CRLF, so a read and write back leaves the file unchanged); SKIP leaves the existing file unchanged (reported skipped). Use 'branch' to pin the project's branch (omit for local/non-branch repositories). Use this to add or update docs, schemas, or manifests. (Overwriting a module .xlsx replaces its bytes; to change a module's TABLES use openl_update_table / openl_append_table / openl_create_project_table.)",
    schema: schemas.writeProjectFileSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const policy = args.conflictPolicy ?? "FAIL";
      const text = textContentOf(args);
      const buffer = policy === "OVERWRITE" && text !== undefined
        ? await keepCrlf(client, args, text)
        : decodeFileContent(args);

      // Studio answers a refusal with its own reason: a file already there, a locked or protected project
      const created = await client.writeProjectFile(args.projectId, args.path, buffer, {
        createFolders: args.createFolders,
        conflictPolicy: policy,
        branch: args.branch,
      });
      const branch = args.branch ? { branch: args.branch } : {};
      if (!created && policy === "SKIP") {
        const skipped = {
          success: true,
          path: args.path,
          written: false,
          skipped: true,
          committed: false,
          ...branch,
          message: `'${args.path}' already exists; left unchanged (conflictPolicy SKIP).`,
        };
        return formatResult(skipped, args.response_format);
      }

      const commit = await commitWrite(client, args.projectId, args.message);
      const result = {
        success: true,
        path: args.path,
        action: created ? "created" : "overwritten",
        bytesWritten: buffer.length,
        committed: commit.committed,
        ...branch,
        message: writtenMessage(created, buffer.length, args.path, args.message, commit),
      };

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "delete_project_file",
    title: "Delete Project File",
    description:
      "Delete a file or folder from a project by its project-relative path. Maps to DELETE /projects/{projectId}/files/{path}. The backend auto-cleans dangling references to the deleted resource from the project configuration. Like writes, the deletion is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch (omit for local/non-branch repositories). Use this to remove legacy assets or deprecate docs. This is a destructive operation.",
    schema: schemas.deleteProjectFileSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.deleteProjectFile(args.projectId, args.path, {
        branch: args.branch,
      });

      const result = {
        success: true,
        path: args.path,
        ...(args.branch ? { branch: args.branch } : {}),
        message:
          `Deleted '${args.path}' from the project working copy. ` +
          `Use openl_save_project to commit the deletion to Git.`,
      };

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "search_project_files",
    title: "Search Project Files",
    description:
      "Search a project's files and folders by ant-glob path 'pattern' (e.g. 'rules/**/*.xlsx'), file 'extensions', resource 'type' (FILE/FOLDER/ANY), and/or a case-insensitive 'content' substring. Maps to POST /projects/{projectId}/file-search. CONTENT LIMITATION: Studio searches inside TEXT files only. It does not inspect binary formats such as XLSX/XLS/ZIP/images; find those only by path pattern, extension, or name, then read/download them separately. Combining content with a binary-only pattern or extension therefore returns no matches even when that text is visible in an Excel workbook. IMPORTANT: set recursive=true to search nested folders — by default (recursive omitted/false) only the project's TOP LEVEL is searched, and a '**' glob alone does NOT descend (so a project-wide search needs recursive=true, and to match files in subfolders use a '**/' pattern such as '**/*.xlsx', not '*.xlsx'). Scope SUBTREE (default) searches within the project and may target a historical 'version'; scope ANCESTORS walks up to the repository root. Returns matching nodes (path, name, type, size, ...), paginated client-side via 'limit'/'offset' (the response carries pagination metadata; the server returns the full match set). Use 'branch' to pin the project's branch. Use this for questions like \"where is portability loading mentioned in XML or Markdown?\" (content, recursive=true) or \"list every xlsx under rules\" (pattern '**/*.xlsx', recursive=true, without content).",
    schema: schemas.searchProjectFilesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      // Undefined fields are dropped by JSON serialization, so an empty query
      // matches everything in scope.
      const query: Types.FileSearchQuery = {
        pattern: args.pattern,
        content: args.content,
        extensions: args.extensions,
        type: args.type,
        scope: args.scope,
        recursive: args.recursive,
        from: args.from,
        version: args.version,
      };

      // The backend file-search has no server-side paging (it returns the full
      // match set), so — like openl_list_deployments/openl_list_repositories — we
      // paginate the returned array client-side and report pagination metadata.
      const results = await client.searchProjectFiles(args.projectId, query, {
        branch: args.branch,
        fields: args.fields,
      });
      return textResult(formatPage(results, args.response_format, limit, offset));
    },
  });

  registerTool({
    name: "copy_project_file",
    title: "Copy Project File",
    description:
      "Copy a file within a project to a new project-relative path. Maps to POST /projects/{projectId}/file-copy. Intermediate destination folders are created automatically. There is NO overwrite option — if destinationPath already exists the call fails with HTTP 409; choose a different destination or delete the existing file first. The copy is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch. Use this to scaffold a new module from an existing one or clone a test set.",
    schema: schemas.fileTransferSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: transferHandler("copy", "Copied", " (copy has no overwrite option)"),
  });

  registerTool({
    name: "move_project_file",
    title: "Move or Rename Project File",
    description:
      "Move or rename a file within a project. Maps to POST /projects/{projectId}/file-move. Intermediate destination folders are created automatically and the source file is deleted after the move. A destination collision fails with HTTP 409. The move is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch. Use this to rename a file or relocate it to another folder.",
    schema: schemas.fileTransferSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: transferHandler("move", "Moved", ""),
  });
}

/**
 * The bytes a write carries. The schema union guarantees one representation. Buffer.from() alone would silently
 * discard malformed base64 characters, so validation and legacy whitespace normalization happen before decoding.
 */
function decodeFileContent(args: z.output<typeof schemas.writeProjectFileSchema>): Buffer {
  const text = textContentOf(args);
  if (text !== undefined) {
    return Buffer.from(text, "utf-8");
  }
  return "blob" in args ? Buffer.from(args.blob, "base64") : Buffer.from(compactBase64(args.content), "base64");
}

/** The text a write carries, or undefined for bytes sent in `blob` or as base64. */
function textContentOf(args: z.output<typeof schemas.writeProjectFileSchema>): string | undefined {
  return "blob" in args || ("encoding" in args && args.encoding === "base64") ? undefined : args.content;
}

/**
 * Whether a read of a path answered the listing of a folder rather than the bytes of a file. A .json file is served
 * as an attachment too, so only a JSON answer that is not an attachment is a listing.
 */
function isListing(contentType: string, contentDisposition: string): boolean {
  return !/attachment/i.test(contentDisposition) && contentType.includes("application/json");
}

/**
 * The text a write replaces a file with, its lines ending as the lines of the file do. A text that ends its lines with
 * LF only, written over a text file that ends them with CRLF, gets CRLF: an agent reading the file and writing the
 * same text back leaves it unchanged, although the CR characters of the read are invisible to it.
 *
 * Bytes sent in `blob` or as base64 are written as they are, and so is a file that does not exist yet.
 */
async function keepCrlf(
  client: OpenLClient,
  args: z.output<typeof schemas.writeProjectFileSchema>,
  text: string,
): Promise<Buffer> {
  const existing = text.includes("\n") && !text.includes("\r")
    ? await client.readProjectFile(args.projectId, args.path, { branch: args.branch })
      .then(({ data }) => data, (error: unknown) => (isNotFoundError(error) ? undefined : Promise.reject(error)))
    : undefined;
  return Buffer.from(existing && endsLinesWithCrlf(existing) ? withCrlf(text) : text, "utf-8");
}

/** A text with LF line ends written with CRLF; a text that has CR characters stays as it is. */
function withCrlf(text: string): string {
  return text.includes("\r") ? text : text.replaceAll("\n", "\r\n");
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

/**
 * The metadata of the content a file read returns: where it is in the file and how it is encoded. A chunk that ends
 * before the end of the file names the offset of the next one.
 */
function fileMetadata(
  path: string,
  version: string | undefined,
  contentType: string,
  encoding: "utf-8" | "base64",
  total: number,
  start: number,
  end: number,
): Record<string, unknown> {
  const ranged = start !== 0 || end !== total;
  return {
    path: path === "" ? "/" : path,
    ...(version ? { version } : {}),
    ...(contentType ? { contentType } : {}),
    mimeType: normalizeMimeType(contentType),
    encoding,
    byteLength: total,
    returnedBytes: end - start,
    ...(ranged ? { range: { offset: start, length: end - start } } : {}),
    ...(end < total ? { nextOffset: end } : {}),
  };
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
 */
async function commitWrite(client: OpenLClient, projectId: string, message: string | undefined): Promise<WriteCommit> {
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

/** The message of a write: how many bytes went where, and whether they were committed. */
function writtenMessage(
  created: boolean,
  bytes: number,
  path: string,
  message: string | undefined,
  commit: WriteCommit,
): string {
  const written = created ? `Wrote ${bytes} byte(s) to '${path}'` : `Overwrote ${bytes} byte(s) in '${path}'`;
  return commitMessage(written, message, commit);
}

/** The message of a change to a file: what was `written`, and whether it was committed. */
function commitMessage(written: string, message: string | undefined, commit: WriteCommit): string {
  if (commit.committed) {
    return `${written} and committed the project to Git: "${message}".`;
  }
  if (!message) {
    return `${written} in the project working copy. Changes are NOT committed — pass 'message' to commit, or use openl_save_project.`;
  }
  const failure = commit.failure ? ` (${commit.failure})` : "";
  return `${written} in the working copy, but it was NOT committed${failure}. Commit with openl_save_project (design repos only).`;
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
      rethrowConflictAsActionable(
        error,
        `Cannot ${action} to '${args.destinationPath}': a file already exists there${conflictNote}. ` +
          `Choose a different destinationPath, or delete the existing file first with openl_delete_project_file.`
      );
    }
    const result = {
      success: true,
      sourcePath: args.sourcePath,
      destinationPath: args.destinationPath,
      ...(args.branch ? { branch: args.branch } : {}),
      message:
        `${done} '${args.sourcePath}' to '${args.destinationPath}' in the project working copy. ` +
        `Use openl_save_project to commit the ${action} to Git.`,
    };
    return formatResult(result, args.response_format);
  };
}
