# Theme 1: sunny study room (2.7)

The user's approved portrait example is the visual reference: seated at the foreground desk, large right window, blue bed, left wall cabinet. The background plates remove the baked-in cabinet and its perched objects; the existing live cabinet, books and editable category labels occupy that space instead.

Built-in imagegen edited the supplied reference into `study_portrait.webp`, and recomposed that plate into `study_landscape.webp`. Prompt constraints: retain desk/window/bed/door, reconstruct bare plaster and parquet in the cabinet area, no replacement shelf or fixed category text. Production plates are RGB WebP quality 88, with longest edge under 1920, each below 500 KB and each decoded bitmap below 8 MiB. Originals remain outside the Git repository.

Source anchors: portrait center x=.36, front feet y=.605, height=.28; landscape x=.35, feet=.69, height=.43. Camera principal point .52/.43, depth ratio .06, room brightness .86. The separate physical perspective, animation and contact-shadow renderer is reused; the full-size management UI stays frontal. Both orientations have independent artwork and anchors.

Background cache (8 MiB), artwork cache (16 MiB) and sampled preview cache (2 MiB) are independent so browsing rooms cannot evict furniture images. No RGB565 reduction, unsafe recycling, or new rendering loop is introduced. Current-room bitmap reuse and both cache bounds are asserted on the device, and all caches clear on memory pressure. The active finish, its full surface and four book variants use about 8.3 MiB, below the 16 MiB artwork bound. Total bounded cache budget stays at 26 MiB; the aim is less decode churn, not an unmeasured claim of lower total process memory.

Theme 1 is first in the picker. On its first introduction only, fresh installations and the former default classroom selection switch to study; explicit other room choices remain. A persisted introduction flag allows later manual return to classroom. Existing bookmarks, categories, sorting and independent shelf/book styles are untouched. Classroom remains selectable alongside all ten other older concepts.

Validation: pure Java catalogue, crop/safe-area geometry, introduction/selection rules, source asset decode/size budgets; native CI covers all 12 rooms in both orientations and existing features. Native results and screenshots must be inspected for the actual release commit. This update does not establish that a previously reported device security warning is resolved.

Native visual review found a small overlap between the portrait left foot and foreground desk books. The final front-foot anchors and depth ratio put the rear near the source wall-floor seam (.595 portrait, .675 landscape) and keep the front feet behind the foreground objects. A source-grounding regression assertion covers both plates.
