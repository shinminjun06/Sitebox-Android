# Room-aware live cabinet

The expanded library remains a frontal, interactive `BookshelfView`. Collapse captures its current furniture, books, and labels into one bounded bitmap, then `ProjectedCabinetView` maps that entire front face into the room with `Matrix.setPolyToPoly`. This is image-based 2.5D rendering; it adds no 3D engine or dependency.

A separate side and roof provide physical depth. Their texture comes from the selected cabinet asset: the uninterrupted outer upright (x .027–.072, y .09–.86) and crown (x .08–.92, y .005–.025). Neither face copies a cubby or books. Restrained translucent shading separates the faces. The view caches all matrices, paths, point arrays, and paints; `onDraw` performs no decoding or bitmap allocation. The existing asset cache and one snapshot capped at 1024 pixels wide bound bitmap memory.

## Poses

Profiles use normalized fractions of the cabinet envelope. Positive depth exposes the left face; negative depth exposes the right face. Top is roof height and rise is the small vertical difference between front corners. Existing scene placement anchors remain unchanged. The maximum front-foot Y remains the envelope bottom, keeping furniture and floor shadow grounded. Subtle rise follows near-horizontal room architecture; sloped attic roofs do not tilt the cabinet. Both ends of the receding front edge rise, and its vertical edge shortens, consistent with a camera above the crown. The rear side edge rises by roof depth plus edge rise, so the rear upright also shortens. Profile constraints require top depth at least the edge rise.

| Room | Exposed side | Depth | Top | Rise |
| --- | --- | ---: | ---: | ---: |
| Classroom | Left | .075 | .025 | .012 |
| Library | Right | .090 | .027 | .016 |
| Bedroom | Left | .085 | .031 | .015 |
| Greenhouse | Left | .045 | .023 | .008 |
| Cafe | Left | .100 | .032 | .018 |
| Tatami | Right | .035 | .028 | .006 |
| Attic | Left | .080 | .035 | .014 |
| Lab | Right | .065 | .024 | .010 |
| Wizard | Right | .080 | .030 | .014 |
| Seaside | Left | .065 | .030 | .012 |
| Train | Left | .110 | .026 | .020 |

## Animation and cancellation

The miniature remains an `ImageView` subclass, preserving its drawable, X/Y, scale, and native accessible tap target. The whole front snapshot, including labels and current book theme, deforms together. A 340ms decelerating `ValueAnimator` moves projection fraction from identity 0 to the room pose 1 alongside the existing translation/scale animation. Expansion reverses the fraction and explicitly settles at identity, including the nonanimated path.

Room selection during a transition cancels the old projection and settles the selected state. Activity destruction cancels projection and releases the snapshot drawable. Rotation recreates the saved collapsed state using the selected room and its orientation-specific crop. The expanded cabinet itself never receives the projection matrix.

## Verification

`tests/run.sh` runs the existing pure suites and asset validators plus 1,628 geometry assertions across all eleven rooms and intermediate fractions: convex front faces, positive area, bounded front/side/roof coordinates, exact identity, and retained floor contact.

`SmokeInstrumentation` exercises native pinch collapse and accessible tap expansion, including actual intermediate increasing/decreasing projection fractions. These two measurements temporarily enable animator duration scale 1 and restore the exact prior setting in `finally`, so CI's disabled animations do not bypass continuity checks. Native assertions inspect actual rendered homography corners, selected side orientation, the current theme's cabinet bitmap, floor contact, and identity restoration.

All eleven rooms rotate through portrait and landscape, checking settings/data preservation and rendered matrices after Activity recreation. Native screenshots retain `scene-{id}.png`, plus orientation-specific images; actual front/side/roof corner coordinates are exported in screen pixels to `qa/projection-{id}-{orientation}.json`. Three additional collapsed screenshots (`projected-cabinet-{finish}.png`) verify oak, walnut, and white oak independently of room and book preferences, then restore the previous finish. Android compilation, instrumentation results, and visual quality are validated by the CI run, not inferred from pure geometry tests.
