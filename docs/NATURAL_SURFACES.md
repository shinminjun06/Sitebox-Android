# Full furniture wood surfaces · 2.6

The previous miniature stretched a narrow upright crop over an invented side
board. These three full-panel surfaces instead supply naturally irregular wood
grain for the physical cuboid's actually visible side and top.

Assets are `app/src/main/assets/themes/shelves/{oak,walnut,white_oak}/surface.webp`.
Each is RGB 1024×1024, WebP quality 90, converted/resampled from an individually
generated square PNG. Total encoded size is about 0.6 MB. No grain was painted
or synthesized with Python; the conversion only sets production dimensions and
encoding. The existing frontal cabinet image remains the expanded UI asset.

The built-in image creation tool used the corresponding existing cabinet as a
material/color reference. Shared prompt: broad continuous solid furniture wood
panel, flat orthographic albedo, even diffuse neutral lighting, subtle irregular
vertical pores and grain, restrained cathedral figures, no object, seam, bevel,
border, baked shadow or perspective. Avoid repetitive stripes, cracks and knots.

- Oak: warm honey caramel light oak, fine pores, softly satin surface.
- Walnut: medium dark chocolate walnut, nuanced low-contrast flowing grain.
- White oak: pale warm ivory beige, delicate gray-beige pores, light whitewash.

All three generated surfaces were visually inspected alongside their cabinet.
The runtime decodes on demand through the existing bounded asset cache, never
inside `onDraw`, and falls back safely if a surface cannot load. Room lighting
is applied to the whole miniature, while face visibility and camera perspective
determine whether a surface is drawn at all.
