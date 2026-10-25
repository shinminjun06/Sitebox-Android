# Sky assets · SiteBox 3.3.0

Real photographed panoramas from Poly Haven, CC0/public domain (https://polyhaven.com/license).
Sources: [Kloofendal 48d Partly Cloudy Pure Sky](https://polyhaven.com/a/kloofendal_48d_partly_cloudy_puresky), [Kloofendal Overcast Pure Sky](https://polyhaven.com/a/kloofendal_overcast_puresky), [Rogland Clear Night](https://polyhaven.com/a/rogland_clear_night).

Downloaded original 2k Radiance HDR files from https://dl.polyhaven.org/file/ph-assets/HDRIs/hdr/2k/ . Converted with Filament **cmgen v1.69.0** (Apache2) using `--size=1024 --type=equirect --format=png --extract=OUTPUT INPUT.hdr`. No generated cloud primitives or photographed indoor furniture are included in these textures.

| Packaged file | Resolution | SHA256 |
|---|---|---|
| models/sky_day.png | 2048×1024 | 128f747cfb36b2214dfe4976a9b47b32a6bb2934e404bea7fc61e79dd9ae51f0 |
| models/sky_cloud.png | 2048×1024 | 47dceec311111e221a1268711962af098abd3a891fc777a202c9c8af852cb268 |
| models/sky_night.png | 2048×1024 | ec518acbcf99210085ff7e9418675838b20ed237cb626e4b00fe22b98f1f822d |

The same inward-facing dome and camera serve all three skies. Day/night/overcast opacity blends continuously. All images are decoded once per renderer lifetime, cached on the GPU with mipmaps and destroyed after the scene is disposed. The actual 3D houses, roof tiles, trees and wires sit between the window and dome, so camera travel produces genuine parallax. Weather glass and roof snow remain separate, and toggling the desk lamp does not modify the sky.

The night panorama contains desert foreground. Its dome samples only the upper sky in the visible window directions (V scale0.65), so photographed desert mountains do not replace the actual neighborhood/hill silhouette when night begins. Dome positions and camera remain identical; only UV sampling changes.
