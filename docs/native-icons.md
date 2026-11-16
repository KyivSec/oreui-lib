# Native icons

`OreNativeIcons` exposes **59 icons**, consisting of 42 on 15×15 canvases and 17 on 16×16 canvases, including explicit small/large variants. PNGs are in `src/main/resources/assets/oreui_lib/textures/icons/native/`. No additional runtime library is needed.

## Drawing

```java
OreNativeIcons icon = OreNativeIcons.HOME;
button.setIcon(icon.texture(), false);

// A 16-GUI-pixel slot: source pixels remain one GUI pixel each.
icon.draw(graphics, x, y, 16, 0xFFFFFFFF);
// Or use the existing drawing helper, which recognizes native icon textures:
OreDrawing.icon(graphics, icon.texture(), x, y, 16, 0xFFFFFFFF);
```

`canvasSize()` returns 15 or 16. `ink()` gives the painted source rectangle. The renderer crops transparent padding for sampling while preserving its placement: a 15-pixel frame is not stretched to 16, and tiny glyphs are not inflated to fill the slot. It uses nearest filtering, no mipmaps, integer coordinates, and a common 16-pixel scaling grid for both families. Slots 16/32/48 use pixel scales 1/2/3; a 30-pixel slot keeps both canvas families at scale 1 rather than doubling only 15-pixel icons. Minecraft's GUI scale then enlarges those pixels normally.

Compact slots retain native pixels whenever the painted glyph fits; explicitly undersized slots use a nearest-filtered compatibility reduction and can lose detail. Prefer 16/32/48 for general-purpose icons. Standard compact control glyphs fit their eight-pixel slots natively. The table's four-pixel sort slots use a hand-snapped 3×2 arrow counterpart rather than downsampling the larger arrows. Sidebar icon slots accommodate the larger native symbols.

`OreImage` recognizes the new resource paths and applies the same integer fitting for icon previews. Arbitrary images still use its existing aspect-ratio behavior. Explicit `OreDrawing.image` calls remain an image-scaling API; supply actual source dimensions and integer destination multiples when preserving pixel art.

## Compatibility and loading circles

All 47 `OreIcons` enum names remain. Their no-argument `texture()` defaults map 46 names to new native artwork. `PACKAGE` keeps its old asset because no replacement box icon was supplied; the gallery's 3D entry uses the new User glyph instead. The historical misspelled `CHEVRON_RIGTH_FILL` still works and maps to the new right arrow.

Only `package_16.png` remains as the fallback for `OreIcons.PACKAGE`. `OreIcons.texture(size)` accepts 8/16/24/32/48 for source compatibility but returns the same artwork as `texture()`, not a separate size-specific file. Set the drawing slot size instead of expecting the source texture dimensions to equal that argument.

`OreIcons.raster()` resolves old named/suffixed paths to current artwork and its actual source dimensions when used through `OreDrawing` / `OreImage`. Removed physical paths no longer exist for direct texture-manager or raw blit access. The legacy enum names, including the misspelled right-arrow name, remain usable.

`OrePixelIcons.Icon` keeps its public names but draws static glyphs from `OreNativeIcons`. Loading/progress drawing uses separate `loading.png` / `progress.png` strips: sixteen 15×15 frames per strip. See `loading-animations.md` for timing, fixed-center rendering, zero/full progress and compatibility notes.

## Gallery

The opt-in [test screen](test-screen.md)'s **Media** page browses all 59 icons. It shows native-scale and integer-enlarged previews, canvas/painted dimensions, and a live icon-button example. Loading examples appear below.
