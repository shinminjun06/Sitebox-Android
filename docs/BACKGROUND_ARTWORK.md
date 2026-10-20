# 2.3 · Spatial background collection

The supplied323642.png concept sheet guided the ten distinct interiors. Original
classroom remains default. No weather/time/network integration is included.
Ten individual production portrait images were made with the built-in image
creation tool, inspected, and encoded to RGB WebP (851×1848). Quality91 except
high-detail greenhouse82 to keep its download below500KB. Total new artwork is
under4MB; thumbnails decode at1/8 size with a separate2MiB bounded cache.
Full-size artwork uses the existing24MiB LRU, not an eager11-room bitmap list.

Shared generation prompt: premium cozy painterly-photoreal interior, tall9:19.5
phone framing, coherent architecture, frontal furniture-placement plane, clear
wall and floor for a separately rendered cabinet, no phone/UI/text/collage and
no actual cabinet in its future installation zone. Distinct room prompts:

- library: towering walnut library, cathedral window, chandelier, books and right reading desk.
- bedroom: private cozy bedroom, bed/desk left, sunny window and cream wall right.
- greenhouse: curved glass conservatory, lush edge plants, pale stone wall and floor.
- cafe: oak coffeehouse, espresso counter left, sunny street window right, amber pendant lights.
- tatami: Japanese cedar/shoji room, garden maple/cherry view, straw mats, low tea table right.
- attic: honey oak rafters, dormer skylight, leather chair/desk left, plaster knee wall right.
- lab: silver-white futuristic study, cyan accents, panoramic city, scientific desk right.
- wizard: Gothic tower, purple moonlit castle view, warm candles, magic objects right.
- seaside: whitewashed coastal villa, turquoise sea, linen curtains, pale blue sofa left.
- train: walnut/brass rail compartment, green bench left, alpine window, clear panel right.

Requested layout percentages in generation were guidance, not assumed truth.
The final PNGs were visually inspected and furniture anchors tuned to their
actual wall/floor boundaries. These source-image coordinates are in SceneTheme:

| ID | Center x | Feet y | Cabinet height |
|---|---:|---:|---:|
|library|.34|.69|.18|
|bedroom|.65|.62|.20|
|greenhouse|.62|.74|.17|
|cafe|.68|.69|.22|
|tatami|.42|.68|.21|
|attic|.68|.58|.19|
|lab|.36|.67|.21|
|wizard|.36|.68|.20|
|seaside|.66|.66|.15|
|train|.68|.70|.23|

Paths: app/src/main/assets/scenes/{id}.webp. New themes use one tall production
image and an anchor-aware crop in wide/square viewports. The same image matrix
positions the live cabinet and its separate contact shadow, preserving a visible
floor instead of simply cropping the middle. Classroom retains its two existing
orientation compositions. Full-size library controls and bookmark state stay
independent of the selected room. New preference backgroundTheme stores stable IDs;
unknown IDs and unavailable artwork fall back to the original classroom.
