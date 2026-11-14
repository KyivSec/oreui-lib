# Loading and progress strips

`loading.png` and `progress.png` are **15×240 PNGs: sixteen vertically stacked 15×15 frames** with transparent backgrounds and opaque white pixels, stored in `assets/oreui_lib/textures/gui/`.

## Use

```java
// Indeterminate loading: 100 ms per frame, 1.6 seconds per loop.
int frame = OrePixelIcons.loadingFrame(Util.getMillis());
OrePixelIcons.loader(graphics, frame, x, y, 16, 0xFFFFFFFF);

// Determinate progress in [0,1]. Zero draws nothing; full progress selects the full circle.
int progressFrame = OrePixelIcons.progressFrame(progress);
OrePixelIcons.progressRing(graphics, progressFrame, x, y, 16, 0xFFFFFFFF);
```

Existing `OreProgressIndicator.Kind.PIXEL_SPINNER` and `PIXEL_RING` automatically use these sheets. The owning UI only supplies a normalized progress value; it does not advance the progress-ring sequence by time. The existing motion toggle still controls the spinner.

Each loading frame contains a nine-pixel arc. Adjacent frames move one pixel along the same 16-pixel circle, including the last-to-first transition. Each progress frame adds one pixel, from one through sixteen. For finite positive values the frame index is `ceil(clamp(progress,0,1) * 16) - 1`; this is a quantized, upward-rounded display. Zero, negative and nonfinite progress return `-1` and draw no ring. Values above one select the full ring. Direct `loader` frame indices wrap; direct `progressRing` indices above fifteen clamp and negative indices draw nothing.

## Pixel geometry

All frames keep the same canvas and center. The renderer never auto-crops or recenters the moving arc. It uses nearest filtering, no mipmaps and integer placement, sharing the static icons' 16-pixel slot scaling grid: slots 16/32/48 use source pixel scales 1/2/3. A 15-pixel frame is not stretched to fill a 16-pixel slot. Compact slots that fit the seven-pixel ring preserve native pixels; explicitly smaller slots use a common seven-pixel crop, not per-frame bounds.

No `.mcmeta` is required. These GUI PNGs are rendered by explicit source coordinates, not automatically animated through a Minecraft block/item atlas.

`OrePixelIcons.LOADING_TEXTURE` and `PROGRESS_TEXTURE` expose the separate resource identifiers. The old `TEXTURE` constant is a deprecated alias for the loading strip; the former shared-atlas layout/path is removed. Existing `loader` and `progressRing` drawing methods remain available.

## Gallery

The **Feedback** and **Media** test pages demonstrate the spinner, motion toggle and progress ring. The last progress frame matches the native static Circle glyph.
