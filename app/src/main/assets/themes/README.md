# SiteBox texture themes

The app loads PNG files from the Android assets folder. If an image is missing, it falls back to its existing Canvas-drawn design.

## Shelf themes

- `shelves/oak/frame.png`
- `shelves/walnut/frame.png`
- `shelves/white_oak/frame.png`

The shelf image currently acts as a **wood surface texture**, stretched separately over each wooden panel and cubby. It is **not yet** a single full 3x3 bookshelf frame image. Use seamless wood textures to avoid visible seams. Full-frame bookshelf rendering requires a later layout change.

## Book themes

For each of `books/classic/`, `books/pastel/`, `books/modern/`, provide transparent portrait PNGs named `navy.png`, `burgundy.png`, `ivory.png`, `green.png`, `brown.png`, `teal.png`, `blue.png`, `tan.png`.

Book images are stretched to the current spine rectangle. Recommended starting resolution: 128 x 512 pixels per spine. The app overlays the editable book title. Drag, click, search and category grouping continue to be handled by Java.

Theme selections are saved in Android SharedPreferences. Images must be added to the repository and included in a new APK build; user-imported ZIP themes are not supported yet.
