# Upload Integrity

How OpenL Studio tells a whole uploaded file from a part of one, so a rule module that did not arrive in full
never reaches a project.

## Why a signature is not enough

An upload used to be checked by its first four bytes: `50 4B 03 04` for a ZIP-based workbook, `D0 CF 11 E0` for a
legacy one. Those bytes say what the content claims to be, not whether all of it arrived — and a client that
loses part of a payload sends a perfectly valid beginning. The file is stored, the API answers success, and the
module fails much later, when someone opens it
([EPBDS-16379](https://jira.eisgroup.com/browse/EPBDS-16379)).

Apache POI cannot close that gap either. A workbook that lost only its ZIP tail — the central directory and the
end-of-central-directory record, about 3% of the bytes — is opened by `OPCPackage.open(File)`, by
`OPCPackage.open(InputStream)` and by `WorkbookFactory.create(...)`, all reporting every sheet. `ZipPackage`
first tries random access and, when that fails, silently falls back to a stream source that walks local file
headers only, which is exactly the damage worth detecting.

## What is verified

`FileIntegrityValidator` (`org.openl.studio.common.validation`) reads the structure each format records about
itself. It runs **before** the content reaches a repository and before POI is involved at all.

- **`.xlsx`, `.xlsm`, `.zip`** — the archive is opened through its **central directory**, which lives at the end
  of the file, so content that lost its tail is refused instead of being read up to the cut. Every entry is then
  read to its end and matched against the size and the CRC32 the directory records for it. An `.xlsx` or `.xlsm`
  must also carry `[Content_Types].xml`, so a plain archive renamed to a workbook is refused.
- **`.xls`** — the OLE2 document is opened and its workbook stream (`Workbook`, or `Book` for BIFF5) is walked to
  the end through the document's own allocation table.
- **Anything else** — written as it arrives. Its format is unknown here, so there is nothing to verify against.

A file the format libraries refuse to read is reported as damaged, whether they signal it by an `IOException` or
by an unchecked exception of their own.

An archive that is about to be unpacked into a project is checked one step further: every workbook it carries is
verified as a workbook of its own, because an archive records what each entry held when it was packed and stays
intact around a module that was already damaged before it went in. What is left beside a module is not read as
one — the owner file Excel keeps while a module is open (`~$Name.xlsx`), the resource fork macOS packs into
`__MACOSX`, and an entry the archive records as empty. Whether an empty module is of any use is answered by
compiling it, not by this check.

## Where it runs

Every path that accepts uploaded content and writes it to a project or a repository:

| Path | Entry point |
|---|---|
| Files API — create, update, multipart upload | `ProjectFilesServiceImpl` |
| Files API — archive expanded into a folder | `FileArchiveSupport.readArchive` |
| Project created from uploaded module files | `DesignTimeRepositoryController.createFromContent` |
| Project created from an uploaded archive | `ZipArchiveValidator` — the archive **and** the workbooks it carries |
| Merge conflict resolved with an uploaded file | `ProjectsMergeConflictsServiceImpl` |

> [!Note]
> A new endpoint that accepts uploaded content is expected to verify it the same way. Reading the directory of an
> archive is the only check that sees the damage; a signature check, POI, and any streaming ZIP reader do not.

## Cost and bounds

The central directory is read in one seek; verifying the entries costs one pass over the uncompressed content.
Every bound the check applies is listed here, because each one narrows what it promises:

- **An upload of more than 1000 MB is refused**, not checked. Content that arrives as a stream has to be copied
  to a temporary file before it can be read at random, and the copy stops at that bound rather than filling the
  file system; the client is told so by `file.content.too-large.message`. It is the same bound the other upload
  paths of OpenL Studio apply.
- **An archive that unpacks to more than 2 GB keeps the structural check and skips the per-entry checksums.**
  Reading it would turn the verification into a decompression bomb of its own. The archive is still refused when
  it lost its tail — the damage an interrupted upload leaves — but a change inside an entry of such an archive is
  not seen.
- **A workbook of more than 100 MB carried by an archive keeps the checksum the archive records for it** and is
  not opened as a workbook of its own, which would mean holding it in memory. A rule module is orders of
  magnitude smaller.

Two more bounds keep an entry that lies about its size from being read without end:

- **Verifying an archive** stops one byte past the size the directory declares for the entry, and refuses the
  entry when what arrives is longer.
- **Expanding an archive into a folder** reads each entry into memory, so the caller's own caps apply first —
  100 MB per entry and 200 MB in total — and the checksum is compared once the entry is read. That path also
  refuses an archive that stores its entry names in an encoding of its own: a ZIP name is UTF-8 only when the
  archive says so, and bytes of any other encoding would name the written file after characters nobody can
  address.

Random access needs a file, while the REST layer hands over a stream, so a verified upload is copied to a
temporary file that only its owner can read. A file API upload keeps that copy as the content to write: it is
opened with `DELETE_ON_CLOSE` and removed as soon as the write closes the stream, and on POSIX systems it is
unlinked the moment it is opened, so an interrupted request leaves nothing behind. An expanded archive holds its
copy only while its entries are read and deletes it afterwards. Content that is already held in memory — the
entries of an uploaded archive — is verified in place, without a copy.

## What is out of scope

The check answers "did all of it arrive", not "is this the file the author meant to send". A client that damages
a payload **before** hashing or sending it produces a request that is consistent with itself; only a digest taken
from the original file, carried end to end, would catch that. `Content-Digest` (RFC 9530) support is tracked
separately.
