# Solid books, window daylight and outdoor depth

The user explicitly wants actual 3D books, directional light and a more natural exterior. Continue the already authorized implementation and APK delivery without additional approval stops. Preserve Java/Views, Filament1.69, the supplied GLB, account/storage/schema, 3×3 categories, 7 books/page, native CRUD/drag and the reversible shared camera.

Replace the flat book quad with merged, closed hardcover meshes: a rounded spine, front/back boards, recessed paper block and head/tail edges. Existing native artwork and real titles form the spine atlas; actual book bounds and atlas UVs share BookArtworkView geometry. PBR surfaces receive directional and lamp light and cast shadows. Labels retain a small native texture plane. No new external model asset is required; geometry derives from data and measured GLB cubbies.

The source back wall is solid across its photographed window. Runtime-replace only that wall with matching plaster strips around the actual opening, hide the flat neighborhood image, retain the authored frame/curtains/sill and all furniture. A merged outdoor neighborhood has pitched tiled roofs, windows, trees, poles, terrain and layered sky in world space. Approaching the shelf therefore changes outside perspective. Weather changes sky, atmospheric contrast, roof snow and glass rain, with no book reload.

Keep one directional sun/moon through the window, a shadowed desk spotlight positioned under the shade and weaker, directional sky irradiance. All real walls/ceiling cast shadows; the window aperture admits light. Preserve readable nights and camera-independent exposure/lighting. No extra editor fill.

Alternatives considered: adding depth behind the old full-shelf texture leaves fake paper edges; individual imported GLBs per book require unnecessary new assets and draw calls. Procedural closed meshes with the existing title artwork provide solid books with bounded buffers and real app data. Keeping a photograph with baked mullions repeats the current window problem; world-space neighborhood geometry fits the supplied room's rendering style.

Implementation decisions are the builder's calls: modest authored roof/window detail, stable variations from book data, merged GPU meshes and no continuous weather animation. Original GLB bytes remain unchanged. Verify geometry/manifold indices/normals, real GPU appearance/lighting, native management, lifecycle, signed install and public APK downloads. Do not claim live Google OAuth or physical-device60fps.
