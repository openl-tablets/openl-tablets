/**
 * Project-file tool handlers (BETA) — read, write, upload, delete, search, copy, and move files inside a project.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { z } from "zod";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatPage } from "../formatters.js";
import { IN_PLACE_NOTE, isEditedInPlace, sanitizeError } from "../utils.js";
import { isNotFoundError } from "../http.js";
import { downloadLink, INLINE_TEXT_BYTES, inlineText, uploadLink } from "../transfer-links.js";
import { registerTool, formatResult, rethrowConflictAsActionable, textResult, type ToolResponse } from "./common.js";

export function registerFileHandlers(): void {
  registerTool({
    name: "read_project_file",
    title: "Read Project File",
    description:
      "Read a file of a project by its project-relative path. " +
      `A text file of up to ${INLINE_TEXT_BYTES} bytes — text as the Content-Type OpenL Studio answers for it says: text/*, JSON, XML or YAML — is returned inline, verbatim. ` +
      "Any other file — a larger text, a workbook, an image, an archive — and a folder, as a ZIP archive, is returned as a download LINK: JSON with the url, a ready `curl` command that saves it locally, the size, and where the file lives in its repository (repositoryId, repositoryPath, branch). " +
      "Run the curl command in a shell: the bytes never enter the conversation. Where OpenL Studio authenticates, the link needs no credential, works until expiresAt and may be fetched again; an agent that has the repository itself can read repositoryPath there instead. " +
      "Read the TABLES of a workbook with openl_get_table, not as a file. Optional 'version' reads a historical revision; 'branch' pins the project's branch. List or find files with openl_search_project_files. " +
      "The answer tells the file's current 'version' — beside the inline text, or in the link JSON — which openl_write_project_file, openl_create_upload_link and openl_delete_project_file require to replace or delete the file.",
    schema: schemas.readProjectFileSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, path } = args;
      const at = { version: args.version, branch: args.branch };
      const meta = isFolderPath(path) ? undefined : await client.getProjectFileMeta(projectId, path, at);
      if (!meta || meta.type === "folder") {
        return formatResult(await downloadLink(client, { kind: "archive", projectId, path, ...at }),
          );
      }
      if (meta.size !== undefined && meta.size <= INLINE_TEXT_BYTES) {
        const text = inlineText(await client.readProjectFile(projectId, path, at));
        if (text !== undefined) {
          // The text stays verbatim: its version is told apart from it
          return { content: [{ type: "text", text }, ...versionNote(meta.version)] };
        }
      }
      const link = await downloadLink(client, { kind: "download", projectId, path, ...at }, meta.size);
      return formatResult(meta.version === undefined ? link : { ...link, version: meta.version });
    },
  });

  registerTool({
    name: "write_project_file",
    title: "Write Project File",
    description:
      "Create or replace a TEXT file in a project by its project-relative path, from UTF-8 'content'. To send a file you have on disk — a workbook, an image, any binary or large file — ask for an upload link with openl_create_upload_link and run its curl command instead of typing the content out. " +
      "COMMIT: pass 'message' to commit the write to Git (a new revision is created); omit 'message' and the write stays in the project WORKING COPY (commit it later with openl_save_project). Committing saves ALL pending project changes and works only for design repositories (not 'local'). A project edited in place (no canOpen and no canClose in the capabilities of openl_get_project) has no working copy: the change is in its files at once, and there is nothing to commit. " +
      "Without 'version' the tool creates a NEW file, missing parent folders included by default (createFolders=true), and writes nothing when a file is already at the path. To REPLACE a file, pass the 'version' openl_read_project_file answered for it: Studio replaces the file in place only while it has that version, and writes nothing when it changed since — on disk, such as in an IDE, or through another client — so read it again and repeat the change. Content whose lines end with LF, written over a file whose lines end with CRLF, is written with CRLF, so a read and write back leaves the file unchanged. Use 'branch' to pin the project's branch (omit for local/non-branch repositories). (To change a module's TABLES use openl_update_table / openl_edit_table / openl_create_project_table.)",
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
        ? rethrowConflictAsActionable(error, `A file is already at '${args.path}', and nothing was written. To replace it, read it with openl_read_project_file and pass the 'version' it answers.`)
        : Promise.reject(error)));
      const branch = args.branch ? { branch: args.branch } : {};

      const inPlace = await editedInPlace(client, args.projectId);
      const commit = inPlace ? { committed: false } : await commitWrite(client, args.projectId, args.message);
      const result = {
        success: true,
        path: args.path,
        action: created ? "created" : "overwritten",
        bytesWritten: buffer.length,
        committed: commit.committed,
        ...branch,
        message: writtenMessage(created, buffer.length, args.path, args.message, inPlace ? "inPlace" : commit),
      };

      return formatResult(result);
    },
  });

  registerTool({
    name: "create_upload_link",
    title: "Create Upload Link",
    description:
      "Get a link that uploads a file from disk into a project — a workbook, an image, any binary or large file — without its bytes passing through the conversation. Takes the target of the write: 'path' (project-relative), 'createFolders' (default true) and 'branch'; optionally the 'size' and hex 'sha256' of the local file, which Studio checks before it writes anything where it authenticates its clients (in single-user mode the link is the REST API, which checks neither). " +
      "Without 'version' the link creates a NEW file and writes nothing over a file already at the path. To REPLACE a file, pass the 'version' openl_read_project_file answered for it: the link replaces the file only while it has that version, and writes nothing when it changed since — in Excel, on disk or through another client — so read it again and repeat the change on what it holds now. " +
      "Returns JSON with the url, the method, a ready `curl` command that sends the file named after the path from the current folder, and where the file lands in its repository (repositoryId, repositoryPath, branch). Run the command in a shell, adjusting the local file name. Where OpenL Studio authenticates, the link needs no credential, works until expiresAt and writes ONCE: a failed upload needs a new link. " +
      "The file lands in the project WORKING COPY, as openl_write_project_file writes it; commit it with openl_save_project. A project edited in place (no canOpen and no canClose in the capabilities of openl_get_project) has no working copy: the change is in its files at once, and there is nothing to commit. A small text file can be written with openl_write_project_file directly.",
    schema: schemas.createUploadLinkSchema,
    annotations: {
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
      "Delete a file or folder from a project by its project-relative path. A file is deleted by the 'version' openl_read_project_file answered for it, which is required: Studio deletes nothing when the file changed since — on disk or through another client. A folder is deleted without it. The backend auto-cleans dangling references to the deleted resource from the project configuration. Like writes, the deletion is staged in the working copy — commit it with openl_save_project. A project edited in place (no canOpen and no canClose in the capabilities of openl_get_project) has no working copy: the change is in its files at once, and there is nothing to commit. Use 'branch' to pin the project's branch (omit for local/non-branch repositories). Use this to remove legacy assets or deprecate docs. This is a destructive operation.",
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

      const result = {
        success: true,
        path: args.path,
        ...(args.branch ? { branch: args.branch } : {}),
        message: await editedInPlace(client, args.projectId)
          ? `Deleted '${args.path}'. ${IN_PLACE_NOTE}`
          : `Deleted '${args.path}' from the project working copy. Use openl_save_project to commit the deletion to Git.`,
      };

      return formatResult(result);
    },
  });

  registerTool({
    name: "search_project_files",
    title: "Search Project Files",
    description:
      "Search the files and folders of a project by an ant-glob path 'pattern' (such as 'rules/**/*.xlsx'), 'extensions', 'type' (FILE, FOLDER or ANY) and a case-insensitive 'content' substring. Content is found in text files only, never inside XLSX, XLS, ZIP or images: find those by path or extension. Only the top level is searched unless recursive=true, even with a '**' glob; match files in subfolders with a '**/' pattern, such as '**/*.xlsx'. Scope SUBTREE, the default, searches the project, also at an earlier 'version'; ANCESTORS walks up to the root of the repository. To list a folder, search 'from' it with no other criteria. Each match carries its path, name, type, size and lastModified; the answer is paged by limit and offset. 'branch' pins the branch of the project.",
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
      return textResult(formatPage(results, "json", limit, offset));
    },
  });

  registerTool({
    name: "copy_project_file",
    title: "Copy Project File",
    description:
      "Copy a file within a project to a new project-relative path. Intermediate destination folders are created automatically. There is NO overwrite option — if destinationPath already exists the call fails with HTTP 409; choose a different destination or delete the existing file first. The copy is staged in the working copy — commit it with openl_save_project. A project edited in place (no canOpen and no canClose in the capabilities of openl_get_project) has no working copy: the change is in its files at once, and there is nothing to commit. Use 'branch' to pin the project's branch. Use this to scaffold a new module from an existing one or clone a test set.",
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
      "Move or rename a file within a project. Intermediate destination folders are created automatically and the source file is deleted after the move. A destination collision fails with HTTP 409. The move is staged in the working copy — commit it with openl_save_project. A project edited in place (no canOpen and no canClose in the capabilities of openl_get_project) has no working copy: the change is in its files at once, and there is nothing to commit. Use 'branch' to pin the project's branch. Use this to rename a file or relocate it to another folder.",
    schema: schemas.fileTransferSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: transferHandler("move", "Moved", ""),
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

/**
 * Whether the project has no working copy to commit, as a project edited in place. A project that cannot be read is
 * taken for one that has: the change is written either way, and only the note on committing it differs.
 */
async function editedInPlace(client: OpenLClient, projectId: string): Promise<boolean> {
  return client.getProject(projectId).then(isEditedInPlace, () => false);
}

/**
 * The message of a write: how many bytes went where, and whether they were committed, or need no commit as the
 * project is edited in place.
 */
function writtenMessage(
  created: boolean,
  bytes: number,
  path: string,
  message: string | undefined,
  commit: WriteCommit | "inPlace",
): string {
  const written = created ? `Wrote ${bytes} byte(s) to '${path}'` : `Overwrote ${bytes} byte(s) in '${path}'`;
  if (commit === "inPlace") {
    return `${written}. ${IN_PLACE_NOTE}`;
  }
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
      message: await editedInPlace(client, args.projectId)
        ? `${done} '${args.sourcePath}' to '${args.destinationPath}'. ${IN_PLACE_NOTE}`
        : `${done} '${args.sourcePath}' to '${args.destinationPath}' in the project working copy. ` +
          `Use openl_save_project to commit the ${action} to Git.`,
    };
    return formatResult(result);
  };
}
