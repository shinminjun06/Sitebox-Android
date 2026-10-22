# Theme 1 furniture integration · 3.0.6

Only the study room is selectable and bundled. Historical design documents describe retired rooms; their artwork is no longer shipped. The independent three shelf finishes and three book styles remain available.

The compact cabinet is moved slightly toward the wall bay (.43 portrait center), with a .21 source-height footprint. Its vanishing point follows the window side (.78, .43), and .085 depth exposes the right panel. The front retains its aspect ratio and the same live book snapshot. Reduced saturation/contrast blends its timber with the room; ambient daylight and the lamp state affect both its finish and the soft floor contact shadow. Expanded furniture remains unfiltered.

Old local selections are persisted as study. Known former room IDs remain accepted by backup validation, then normalize to study; book contents and the rest of the backup are preserved. Unknown IDs remain rejected by strict backup validation.

Verification includes Java geometry checks, native rendering previews, preference and cloud-backup migration regression tests, unit tests, release lint, and signing checks. Actual phone interaction is not verified by native rendering previews.

## Reference refinement · 3.0.7

The supplied reference favors a near-frontal dark timber cabinet. The camera X is now .69 with shallow .055 depth, a .82 base brightness, and reduced saturation; stored shelf finishes remain selectable and expanded furniture is untouched. A 768×384 alpha sprite supplies the plant, small botanical frame, and mug above the cabinet. It uses the same projection and room-light filter as the live cabinet snapshot, with an opacity ramp during collapse. The stage allows drawing above the miniature view so foliage is not clipped. The sprite adds no interaction target and contains no fixed fake books. Its decoded memory is included in artwork budget validation.

## Perspective correction · 3.0.8

The previous renderer held the front snapshot as a screen-aligned rectangle and changed only the depth faces. Its tests incorrectly required a horizontal crown and a fixed apparent front aspect ratio. This could not represent the sloping room baseboard or the visible top of the low reference cabinet.

The portrait back-wall measurements use the beam (300,46)–(534,14) and baseboard (300,1073)–(612,1106) on the 854×1842 plate. Landscape uses (453,39)–(877,7) and (454,621)–(1155,666) on the 1672×941 plate. Their intersections supply separate width vanishing points and horizons for each artwork. These are an approximate calibration of illustrated artwork, not a reconstructed physical camera. Depth rays use the same horizon with the window-side depth vanishing point. Four-corner homography now projects the front snapshot (and its real books and decor) rather than leaving the front rectangular. Uprights remain vertical; crown and feet converge toward the measured width vanishing point.

Rear corners use a separate retreat factor for each upright's camera distance. A shared factor would move the rear crown toward a different width vanishing point. Floor contact shadows now use separate vertical positions for the two feet. Expansion still ends at an identity transform.

RoomPerspectiveTest checks front and rear width rays, depth rays, the visible top below the horizon, and the independently measured baseboard slope in portrait and landscape crops. SceneProjectionTest checks bounded convex faces and identity across viewport sizes and animation fractions. Native rendering checks the two shadow contact heights and retains sample images for visual inspection. These checks do not replace physical-phone evaluation of the visual result.

The portrait cabinet previously straddled the back-wall corner (artwork x≈300). Its center is now .505 and height .18, placing its left edge inside that wall bay; front feet .617 retreat to the floor below the baseboard. Landscape keeps center .40 with height .30 and feet .718. Independent regression checks compare rear foot positions with the measured lower baseboard edge and require the cabinet to stay within one wall plane.
