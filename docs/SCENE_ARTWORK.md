# Classroom nook · 2.2

The 2.1 room was attractive but its foreground desk, narrow wall and tall window did
not provide a planned furniture footprint. 2.2 uses two coordinated room assets.
The live cabinet is never baked into either background.

Built-in image generation edited the existing school_morning.jpg reference.
The reference remains bundled unchanged as a fallback. New RGB WebP files are
quality 93, converted from the generated PNG without repainting room details:

- scenes/classroom_portrait.webp · 852×1846 · sill ~45.5%, floor ~77%.
- scenes/classroom_landscape.webp · 1672×941 · sill ~40%, floor ~81%.

Portrait prompt: retain warm sunny Korean school classroom, blue sky and leafy
school grounds; front-facing level wall; windows above 45% image height, clear
cream wall underneath, oak skirting and honey oak floor; unobstructed center
for an independently rendered 3×3 cabinet. Plant and chalkboard at left margin,
minimal desk corner at far bottom right, no center clutter, no text or cabinet.

Landscape prompt: same classroom in 16:9, windows above ~36%, broad clear wall,
skirting around79%, floor below; empty center for live bookshelf. Warm upper-left
sunlight, straight wall, no foreground desks, no text or baked furniture.
The generated result's actual landmarks are measured above rather than assumed
from the prompt's requested coordinates.

Runtime uses the same CENTER_CROP transform for backdrop and furniture anchors.
Portrait cabinet height is 31% of image height, feet at80%, center at52% width.
Landscape uses38% height and84% feet to remain beneath its lower windowsill.
Safe-area clamps preserve reachability on short or cropped viewports. A subtle
warm radial contact shadow is a separate non-interactive layer below the cabinet,
fading with the transition. No books, categories, ordering or preferences change.

Validation includes wall/floor alignment checks at portrait and landscape sizes,
actual native pinch/tap/rotation tests and real screenshots. The final report
records the exact build commit and which device environment was executed.
