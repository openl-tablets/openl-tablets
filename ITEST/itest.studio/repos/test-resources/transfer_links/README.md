# Transfer links

A client with its own credentials asks for a link, and a request without any credential moves the file through it.

- `010-setup` — a project from an archive, its id captured;
- `020-download` — a download link answers every `GET` until it expires, and refuses a `PUT`;
- `030-upload` — an upload link writes its body once, as the files API does, and a second `PUT` is refused;
- `040-mismatch` — an upload that does not match its declared SHA-256 writes nothing;
- `050-unknown` — an unknown link is refused as an expired or a spent one is.
