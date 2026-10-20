# Classroom nook · 2.4

The 2.4 classroom uses coordinated portrait and landscape room assets with a
more distant viewpoint and an open aisle along the left side. The deeper room
composition leaves breathing space around the independently rendered cabinet.
The live cabinet is never baked into either background.

Built-in image generation edited the school classroom reference. The original
school_morning.jpg remains bundled unchanged as a fallback. The generated PNGs
were converted to RGB WebP at quality 91 without repainting room details:

- scenes/classroom_portrait.webp · 852×1846 · sill ~43%, floor ~66.5%.
- scenes/classroom_landscape.webp · 1672×941 · sill ~45%, floor ~72%.

Both compositions retain a warm sunny Korean school classroom, blue sky,
leafy school grounds, cream walls, oak trim and honey oak floors. The viewpoint
looks farther into the classroom, with a clear left aisle and open wall/floor
space toward the right for the live 3×3 cabinet. Foreground furniture stays out
of this footprint; no text, books or cabinet are baked into the room image.
The landmarks above are measured from the generated result.

Runtime uses the same CENTER_CROP transform for backdrop and furniture anchors.
Portrait cabinet height is 23% of image height, feet at 69%, center at 62% width.
Landscape uses 27% height, feet at 75%, and center at 62% width.
Safe-area clamps preserve reachability on short or cropped viewports.
Near-square windows (width/height >=0.8) also use the wide composition so that
the room floor remains visible on foldables and multiwindow views.
A soft warm radial contact shadow is a separate non-interactive layer below the
cabinet, fading with the transition. Expanded controls apply a subtle uniform
warm scrim; the complete room is visible when the cabinet is collapsed.
No books, categories, ordering or existing theme preferences change.

Validation includes wall/floor alignment checks at portrait and landscape sizes,
actual native pinch/tap/rotation tests and real screenshots. The final report
records the exact build commit and which device environment was executed.
