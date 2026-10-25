# Closed cabinet panels and gentler portrait slope — 3.0.12

The user's screenshot showed a transparent gutter between the front right post and the projected side. The room sprite's opaque timber occupies x=10..500 of a 512px image at multiple body heights; the old side attached at x=512. A native rendered-pixel regression reproduced alpha=0 at 99% of the front source width, proving the gap was genuine transparency rather than wood colour.

ProjectedCabinetView now measures opaque body edges and the top board when a room snapshot is created. The side and roof overlap these visible edges beneath the front artwork. Each panel has an opaque wood base; its texture is sampled from a bounded source rectangle rather than projecting the entire square surface beyond a narrow UV strip. This closes both the side seam and crown seam without changing the artwork or book positions.

The portrait width vanishing point is moved farther out to reduce the front base slope from about 2.85 degrees to about 1.9 degrees. Uprights stay vertical, the reference silhouette remains within the existing seven-pixel tolerance, and foot contacts continue to use actual opaque soles. Room placement, time-based day/night switching and lamp composition stay in use.

Verification covers native side/interior opacity in portrait and landscape, reference silhouette and gentle slope, existing convex projection/animation bounds, foot contacts and rear support, real book rendering, day/night/lamp previews, all unit tests, lint and release signing. Native renders are not physical-device validation.
