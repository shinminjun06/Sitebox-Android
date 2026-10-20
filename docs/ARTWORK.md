# Artwork provenance and mapping

Built-in image generation created a transparent furniture sprite atlas and a transparent
12-book sprite atlas. No existing third-party photography was downloaded. Original
school_morning.jpg remains unchanged (also identical to the supplied APK's background).

Furniture prompt: three orthographic empty 3×3 cabinets, light oak, walnut and white oak;
real irregular wood grain, recessed darker backs, thick shelf lips, coherent joinery,
upper-left sunlight; no text, no books, transparent background. Output 1536×1024.
Crop boxes (left, top, right, bottom): oak (20,77,510,894), walnut (523,77,1014,894),
white oak (1027,77,1520,894). Resampled to 768×998 transparent WebP.

Book prompt: 4×3 regular atlas, classic navy/burgundy/ivory/green gilded leather, pastel
sage/pink/lavender/cream woven linen, modern charcoal/slate/sand/sage hardcover;
rounded bindings, paper on right, blank central titles, no text, transparent background.
Output 1462×1076. Column bounds (137,337), (476,685), (814,1024), (1155,1369);
row bounds (0,328), (344,696), (710,1054). Alpha-trimmed and resampled to 160×560.
Cropping and size normalization retain the generated surfaces rather than synthesizing
striped substitute textures. Four variants per style; heights vary in live views.

Runtime: 7 books per cell. Cabinet width capped at 480dp; portrait height 1.30×width.
Book data is unchanged when paginating. Gold plates use native gradient drawable +
editable text. Canvas draws bitmap, title, star and fallback solid only; no material grain.

2.1 rendering: draw the first 76% of book artwork as the spine, compress its paper
side to 3.5% of displayed width, center live titles within the spine only, and keep
them between the foil bands (31%–72% of height). CJK titles use upright glyphs.
The pinch scene uses a bounded snapshot of the current cabinet, not a second
independently editable book dataset.
