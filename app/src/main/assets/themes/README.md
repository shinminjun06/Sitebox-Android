# SiteBox artwork themes, format v1

Shelf and book selections are independent and persisted with legacy preference keys.
Each built-in theme has its own folder and `theme.json`. Furniture uses `cabinet.webp`,
books use four transparent `0.webp`–`3.webp` variants. Titles, favorite markers and
category names are live Android text, never baked into the image.

Artwork was generated as two production sprite atlases using the built-in image
generation tool, then cropped/resampled into deployable WebP assets. No procedural
wood or book texture generation occurs at build time. Source prompt and processing
bounds are documented in `docs/ARTWORK.md`.

Shelf geometry is a shared normalized 3×3 template in `LibraryRules.cell`.
The JSON documents the same bounds for future third-party adapters; v1 runtime
loads bundled IDs only. User ZIP importing is **not** enabled in this release.
A future importer must reject traversal/absolute/symlink paths, limit compressed
and expanded byte counts, restrict file count and PNG/WebP dimensions, and stage
validated immutable packages privately before activation. Do not directly unzip
untrusted themes into the application's assets or preferences directories.
