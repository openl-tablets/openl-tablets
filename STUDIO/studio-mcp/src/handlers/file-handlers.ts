/**
 * Project-file tool handlers (BETA) — read, write, delete, search, copy, and
 * move files inside an opened project.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatResponse, paginateResults } from "../formatters.js";
import { isAxiosError, sanitizeError } from "../utils.js";
import { RESPONSE_LIMITS } from "../constants.js";
import {
  compactBase64,
  looksBinary,
  normalizeMimeType,
} from "../content-utils.js";
import { registerTool, rethrowConflictAsActionable, type ToolResponse } from "./common.js";

export function registerFileHandlers(): void {
  registerTool({
    name: "read_project_file",
    category: "Project Files",
    title: "Read Project File",
    description:
      "Read any file in a project by its project-relative path — text or binary, and folder listings too. Maps to GET /projects/{projectId}/files/{path}. Behavior by path/params: " +
      "(1) a FILE path returns its content — UTF-8 text is returned verbatim, while binary is returned as base64 'content' in a JSON text envelope with byte and MIME metadata (use encoding to force 'utf-8' or binary; default 'auto' detects); " +
      "(2) a FILE path with view='meta' returns JSON metadata (name, size, extension, lastModified); " +
      "(3) a FOLDER path (empty string for the root, or a path ending in '/') lists its entries (use recursive, viewMode FLAT/NESTED, extensions, namePattern, foldersOnly); " +
      "(4) a FOLDER path with download=true returns a ZIP as base64 in the same JSON text envelope. " +
      "Optional 'version' reads a historical revision; 'branch' pins the project branch. Optional byte range (offset/length) is applied client-side AFTER fetching the whole file (the backend does not support partial transfers), so the entire file is loaded into memory; for very large/binary files, bound the RETURNED size with offset/length and read in chunks (a full file's base64 can exceed MCP message limits). Use this to read AGENTS.md, README.md, schemas, manifests, or to inspect/export xlsx rule files.",
    schema: schemas.readProjectFileSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const path = typedArgs.path ?? "";

      const { data, contentType, contentDisposition } = await client.readProjectFile(
        typedArgs.projectId,
        path,
        {
          view: typedArgs.view,
          download: typedArgs.download,
          recursive: typedArgs.recursive,
          viewMode: typedArgs.viewMode,
          extensions: typedArgs.extensions,
          namePattern: typedArgs.namePattern,
          foldersOnly: typedArgs.foldersOnly,
          version: typedArgs.version,
          branch: typedArgs.branch,
          fields: typedArgs.fields,
        }
      );

      // Distinguish a file/ZIP download (attachment) from a JSON listing/metadata
      // response. A .json FILE is also served as an attachment, so check the
      // Content-Disposition first and only then fall back to the Content-Type.
      const isAttachment = /attachment/i.test(contentDisposition);
      const isJson = contentType.includes("application/json");
      if (!isAttachment && isJson) {
        let parsed: unknown;
        try {
          parsed = JSON.parse(data.toString("utf-8"));
        } catch {
          parsed = data.toString("utf-8");
        }
        return { content: [{ type: "text", text: formatResponse(parsed, format) }] };
      }

      // File (or folder-ZIP) content. Apply the optional client-side byte range,
      // then choose text or a base64 JSON envelope.
      const total = data.length;
      const start = Math.min(typedArgs.offset ?? 0, total);
      const end =
        typedArgs.length !== undefined ? Math.min(start + typedArgs.length, total) : total;
      const slice = data.subarray(start, end);
      const ranged = start !== 0 || end !== total;

      const forceBinary = typedArgs.encoding === "base64" || typedArgs.download === true;
      const forceText = typedArgs.encoding === "utf-8";
      const asBase64 = forceBinary || (!forceText && looksBinary(slice));

      if (!asBase64) {
        // Text content: returned (near-)verbatim — most useful for docs/schemas/
        // manifests (response_format does not apply). The verbatim path would
        // otherwise bypass the response-size cap, so when the content exceeds the
        // 25K limit we truncate and append a continuation cursor (next byte offset)
        // so the caller can page the rest with offset/length.
        let text = slice.toString("utf-8");
        if (text.length > RESPONSE_LIMITS.MAX_CHARACTERS) {
          text = text.slice(0, RESPONSE_LIMITS.MAX_CHARACTERS);
          const nextOffset = start + Buffer.byteLength(text, "utf-8");
          text += `\n\n${RESPONSE_LIMITS.TRUNCATION_MESSAGE} Returned bytes ${start}–${nextOffset} of ${total}; continue with offset=${nextOffset}.`;
        }
        return { content: [{ type: "text", text }] };
      }

      const metadata = {
        path: path === "" ? "/" : path,
        ...(typedArgs.version ? { version: typedArgs.version } : {}),
        ...(contentType ? { contentType } : {}),
        mimeType: normalizeMimeType(contentType),
        encoding: "base64" as const,
        byteLength: total,
        returnedBytes: slice.length,
        ...(ranged ? { range: { offset: start, length: slice.length } } : {}),
      };

      // Although MCP permits arbitrary binary BlobResourceContents, several
      // clients route every embedded blob through their image renderer and
      // reject XLSX/ZIP/octet-stream data as an unsupported image. TextContent
      // is the interoperable lossless transport for non-display binary files.
      // Keep it untruncated; callers can bound/page bytes with offset/length.
      const envelope = { ...metadata, content: slice.toString("base64") };
      return {
        content: [{
          type: "text",
          text: formatResponse(envelope, "json", { skipTruncation: true }),
        }],
      };
    },
  });

  registerTool({
    name: "write_project_file",
    category: "Project Files",
    title: "Write Project File",
    description:
      "Create or replace a file in a project by its project-relative path. Provide UTF-8 text in 'content'. For binary files such as xlsx/images/zip, provide base64 bytes in 'blob'; its advertised JSON Schema uses contentEncoding='base64'. The legacy content + encoding='base64' form remains accepted. " +
      "COMMIT: pass 'message' to commit the write to Git (a new revision is created); omit 'message' and the write stays in the project WORKING COPY (commit it later with openl_save_project). Committing saves ALL pending project changes and works only for design repositories (not 'local'). " +
      "By default missing parent folders are created (createFolders=true). If the target file already EXISTS, behavior follows conflictPolicy: FAIL (default) returns an error; OVERWRITE replaces the file in place; SKIP leaves the existing file unchanged (reported skipped). Use 'branch' to pin the project's branch (omit for local/non-branch repositories). Use this to add or update docs, schemas, or manifests. (For a NEW file the tool POSTs/creates; OVERWRITE is performed via PUT/update — overwriting a module .xlsx replaces its bytes but to change a module's TABLES use openl_update_table / openl_append_table / openl_create_project_table.)",
    schema: schemas.writeProjectFileSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // The schema union guarantees one representation. Buffer.from() alone
      // would silently discard malformed base64 characters, so validation and
      // legacy whitespace normalization happen before decoding.
      const blobInput = "blob" in typedArgs;
      const legacyBase64 = "encoding" in typedArgs && typedArgs.encoding === "base64";
      const suppliedContent = blobInput ? typedArgs.blob : typedArgs.content;
      const normalizedContent = legacyBase64
        ? compactBase64(suppliedContent)
        : suppliedContent;
      const buffer = Buffer.from(normalizedContent, blobInput || legacyBase64 ? "base64" : "utf-8");
      const policy = typedArgs.conflictPolicy ?? "FAIL";

      // Create with POST. POST is create-only — an existing file yields 409, and
      // the backend does NOT honor conflictPolicy on a single-file POST. So we
      // implement conflictPolicy here: OVERWRITE replaces via PUT (updateResource,
      // in-place — no delete), SKIP leaves the existing file, FAIL surfaces 409.
      let metadata: unknown;
      let action: "created" | "overwritten" = "created";
      try {
        metadata = await client.writeProjectFile(typedArgs.projectId, typedArgs.path, buffer, {
          // Schema default is true; materialize it here since handlers receive raw
          // args (zod .default() only shapes the published JSON Schema, it is not
          // applied at call time). Pass false explicitly to require an existing parent.
          createFolders: typedArgs.createFolders ?? true,
          branch: typedArgs.branch,
        });
      } catch (error) {
        if (!(isAxiosError(error) && error.response?.status === 409)) {
          throw error;
        }
        if (policy === "OVERWRITE") {
          // Replace the existing file in place via PUT (backend updateResource).
          await client.updateProjectFile(typedArgs.projectId, typedArgs.path, buffer, {
            branch: typedArgs.branch,
          });
          action = "overwritten";
        } else if (policy === "SKIP") {
          const skipped = {
            success: true,
            path: typedArgs.path,
            written: false,
            skipped: true,
            committed: false,
            ...(typedArgs.branch ? { branch: typedArgs.branch } : {}),
            message: `'${typedArgs.path}' already exists; left unchanged (conflictPolicy SKIP).`,
          };
          return { content: [{ type: "text", text: formatResponse(skipped, format) }] };
        } else {
          // FAIL (default). The caller did not request OVERWRITE/SKIP, so this is a
          // genuine, actionable conflict (not a contradictory "set what you set").
          throw new ProtocolError(
            ProtocolErrorCode.InvalidRequest,
            `Cannot write '${typedArgs.path}': a file already exists there. ` +
              `Set conflictPolicy: "OVERWRITE" to replace it, "SKIP" to leave it unchanged, or write to a different path.`
          );
        }
      }

      // "message present -> commit": after writing to the working copy, commit it to
      // Git via saveProject (a PATCH that creates a revision). Saving commits ALL
      // pending project changes, not just this file (OpenL has no per-file commit).
      // Without a message the write simply stays in the working copy.
      let committed = false;
      let commitNote: string | undefined;
      if (typedArgs.message) {
        try {
          const saveResult = await client.saveProject(typedArgs.projectId, typedArgs.message);
          committed = saveResult.success !== false;
          commitNote = saveResult.message;
        } catch (error) {
          // e.g. a 'local' repository (no Git) — the file is already written to the
          // working copy, so report it couldn't be committed instead of failing.
          commitNote = sanitizeError(error);
        }
      }

      const verb = action === "overwritten" ? "Overwrote" : "Wrote";
      const prep = action === "overwritten" ? "in" : "to";
      const result = {
        success: true,
        path: typedArgs.path,
        action,
        bytesWritten: buffer.length,
        committed,
        ...(typedArgs.branch ? { branch: typedArgs.branch } : {}),
        ...(metadata && typeof metadata === "object" && Object.keys(metadata as object).length > 0
          ? { metadata }
          : {}),
        message:
          committed
            ? `${verb} ${buffer.length} byte(s) ${prep} '${typedArgs.path}' and committed the project to Git: "${typedArgs.message}".`
            : typedArgs.message
              ? `${verb} ${buffer.length} byte(s) ${prep} '${typedArgs.path}' in the working copy, but it was NOT committed${commitNote ? ` (${commitNote})` : ""}. Commit with openl_save_project (design repos only).`
              : `${verb} ${buffer.length} byte(s) ${prep} '${typedArgs.path}' in the project working copy. Changes are NOT committed — pass 'message' to commit, or use openl_save_project.`,
      };

      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });

  registerTool({
    name: "delete_project_file",
    category: "Project Files",
    title: "Delete Project File",
    description:
      "Delete a file or folder from a project by its project-relative path. Maps to DELETE /projects/{projectId}/files/{path}. The backend auto-cleans dangling references to the deleted resource from the project configuration. Like writes, the deletion is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch (omit for local/non-branch repositories). Use this to remove legacy assets or deprecate docs. This is a destructive operation.",
    schema: schemas.deleteProjectFileSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      await client.deleteProjectFile(typedArgs.projectId, typedArgs.path, {
        branch: typedArgs.branch,
      });

      const result = {
        success: true,
        path: typedArgs.path,
        ...(typedArgs.branch ? { branch: typedArgs.branch } : {}),
        message:
          `Deleted '${typedArgs.path}' from the project working copy. ` +
          `Use openl_save_project to commit the deletion to Git.`,
      };

      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });

  registerTool({
    name: "search_project_files",
    category: "Project Files",
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
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      // Undefined fields are dropped by JSON serialization, so an empty query
      // matches everything in scope.
      const query: Types.FileSearchQuery = {
        pattern: typedArgs.pattern,
        content: typedArgs.content,
        extensions: typedArgs.extensions,
        type: typedArgs.type,
        scope: typedArgs.scope,
        recursive: typedArgs.recursive,
        from: typedArgs.from,
        version: typedArgs.version,
      };

      // The backend file-search has no server-side paging (it returns the full
      // match set), so — like openl_list_deployments/openl_list_repositories — we
      // paginate the returned array client-side and report pagination metadata.
      const results = await client.searchProjectFiles(typedArgs.projectId, query, {
        branch: typedArgs.branch,
        fields: typedArgs.fields,
      });
      const paginated = paginateResults(results, limit, offset);

      return {
        content: [{
          type: "text",
          text: formatResponse(paginated.data, format, {
            pagination: { limit, offset, total: paginated.total_count },
          }),
        }],
      };
    },
  });

  registerTool({
    name: "copy_project_file",
    category: "Project Files",
    title: "Copy Project File",
    description:
      "Copy a file within a project to a new project-relative path. Maps to POST /projects/{projectId}/file-copy. Intermediate destination folders are created automatically. There is NO overwrite option — if destinationPath already exists the call fails with HTTP 409; choose a different destination or delete the existing file first. The copy is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch. Use this to scaffold a new module from an existing one or clone a test set.",
    schema: schemas.copyProjectFileSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      try {
        await client.copyProjectFile(
          typedArgs.projectId,
          { sourcePath: typedArgs.sourcePath, destinationPath: typedArgs.destinationPath },
          { branch: typedArgs.branch }
        );
      } catch (error) {
        rethrowConflictAsActionable(
          error,
          `Cannot copy to '${typedArgs.destinationPath}': a file already exists there (copy has no overwrite option). ` +
            `Choose a different destinationPath, or delete the existing file first with openl_delete_project_file.`
        );
      }

      const result = {
        success: true,
        sourcePath: typedArgs.sourcePath,
        destinationPath: typedArgs.destinationPath,
        ...(typedArgs.branch ? { branch: typedArgs.branch } : {}),
        message:
          `Copied '${typedArgs.sourcePath}' to '${typedArgs.destinationPath}' in the project working copy. ` +
          `Use openl_save_project to commit the copy to Git.`,
      };

      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });

  registerTool({
    name: "move_project_file",
    category: "Project Files",
    title: "Move or Rename Project File",
    description:
      "Move or rename a file within a project. Maps to POST /projects/{projectId}/file-move. Intermediate destination folders are created automatically and the source file is deleted after the move. A destination collision fails with HTTP 409. The move is staged in the working copy — commit it with openl_save_project. Use 'branch' to pin the project's branch. Use this to rename a file or relocate it to another folder.",
    schema: schemas.moveProjectFileSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      try {
        await client.moveProjectFile(
          typedArgs.projectId,
          { sourcePath: typedArgs.sourcePath, destinationPath: typedArgs.destinationPath },
          { branch: typedArgs.branch }
        );
      } catch (error) {
        rethrowConflictAsActionable(
          error,
          `Cannot move to '${typedArgs.destinationPath}': a file already exists there. ` +
            `Choose a different destinationPath, or delete the existing file first with openl_delete_project_file.`
        );
      }

      const result = {
        success: true,
        sourcePath: typedArgs.sourcePath,
        destinationPath: typedArgs.destinationPath,
        ...(typedArgs.branch ? { branch: typedArgs.branch } : {}),
        message:
          `Moved '${typedArgs.sourcePath}' to '${typedArgs.destinationPath}' in the project working copy. ` +
          `Use openl_save_project to commit the move to Git.`,
      };

      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });
}
