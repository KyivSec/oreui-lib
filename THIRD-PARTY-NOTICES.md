# Third-party notices

## Ore UI design system

Minecraft - Ore UI Style is credited to **Miljö AB** and **Turóczi Ádám** and listed under **Creative Commons Attribution 4.0 International (CC BY 4.0)**.

- Source: https://www.figma.com/community/file/1675191037788105821/minecraft-ore-ui-style
- Miljö AB: https://www.figma.com/@miljo
- Turóczi Ádám: https://www.figma.com/@eu2kdevs
- License: https://creativecommons.org/licenses/by/4.0/
- Legal code: https://creativecommons.org/licenses/by/4.0/legalcode

This library adapts that design system into reusable, procedurally rendered Minecraft widgets. `OrePalette` uses the supplied document's Colored, General, Destructive, Warning, and Realms color variables. `OreDrawing`, `OreButton`, `OreSlider`, and the opt-in `OreUiTestScreen` example adapt the visual language into flat rectangular surfaces, highlights, shadows, visible focus, and a progress track. These are adaptations, not an exact reproduction of all source variants or motion.

The Button and Switch pass references the live Figma components at https://www.figma.com/design/L52fkxlS4dEWvbt28gDJ1p?node-id=425-8332 (Button set `425:8428`, Switch set `425:9269`). Adaptations include directional bevels, pressed face displacement, palette-colored disabled buttons, quarter-scale switch geometry, handle-only hover/focus, gray disabled switch handles, and optional procedural on/off symbols. Layout sizes and Minecraft's existing font remain project-native; no additional source images or fonts are redistributed by this pass.

The gallery additionally adapts that visual language into selection controls, labels/tags/badges/indicators, banners/cards, progress/spinner controls, navigation bars, menus/popups, dropdowns, color/keybind inputs, and toast notifications. Widgets absent from the design document use simple matching procedural surfaces. These are element-family examples, not all source variants or a complete implementation of the handout.

The subsequent read-only live-reference pass references selection/selectable icons (`2005:10461`, `2182:3001`), horizontal/vertical navigation (`2095:8282`, `2007:1143`), seamless buttons (`2140:10817`), checkbox/radio (`2009:1468`, `425:10153`), slider (`2031:8382`), textbox/search (`2032:10457`, `2008:1324`), banner/card (`2032:10390`, `2031:1882`), tag/badge/divider/indicator (`2140:11478`, `2095:8505`, `2140:11416`, `2140:10423`), popup/header/actions/menu (`2140:11122`, `2140:10504`, `2140:10998`, `2140:11227`), overlay/toast/tooltip (`2140:10445`, `2132:10300`, `2031:1644`) and pagination (`425:14637`, `425:14678`). Source text sizes are adapted using Minecraft's existing font. Composed controls and spinner/progress adaptations are not claimed as exact source components. No new images, fonts, tooling or runtime dependencies are bundled by this pass.

The navigation/popup follow-up additionally references the Test page's `Frame 1` (`2169:12406`) and `Popup` (`2147:11993`), including their instance overrides. It adapts the composed navigation panel/header/groups and separate popup body/action spacing. Existing bundled icon rasters are selected by destination resolution and sampled without linear filtering; their image bytes and attribution records are unchanged.

An earlier pass imported **47 icon names in five sizes (235 variants)** from `offline-exports/Components/Icon`, rasterized from offline SVG reconstructions with drop-shadow filters removed and the Check orientation corrected. After replacement by user-provided pixel redraws, unused raster variants and aliases were removed at the user's request. Only the 16-pixel Package fallback from that set remains packaged; it is an adapted offline reference, not a native Figma export.

`assets/oreui_lib/icon-attribution.json` in the JAR records the retained `textures/icons/package_16.png` fallback's original SVG/PNG filename, node, changes and SHA-256. The other 236 old PNG files, including unsuffixed Add/Search aliases, are no longer packaged. Legacy Java enum/size-based APIs redirect to current textures rather than retaining duplicate raster resources.

The user-PSD import below supplies current static defaults and the 59-icon Media browser. Minecraft's stone texture is referenced from the game's own resources, not redistributed. No font binaries or export tooling are bundled. The code's MIT license does not replace design or font licenses. Credits remain discoverable in mod metadata and this packaged notice; the gallery has no About page.

The 3D previews page uses Minecraft's runtime item/block/player models, the current player's skin and the game's native animation/rendering systems. These resources are referenced at runtime, not copied into this library. Its preview surfaces and controls reuse the existing Ore adaptations; no new third-party images, model files, skins, animation assets or rendering dependencies are redistributed.

Button labels and monochrome icons use the source palette's text colors, including dark content on General and Warning surfaces. Disabled button content uses half opacity rather than a universal gray. Text shadows are disabled for the library's labels and the test screen's editable field; procedural surface shadows are retained.

The visual pass restores lighter inset outlines, top/left highlights, raised depth, recessed tracks/fields/sections, and distinct popup header/action surfaces from the supplied references. Page padding and grouping are adaptations for a responsive Minecraft gallery rather than a literal copy of the prototype canvas. Geometry, all source states, fonts, and motion have not been certified pixel-identical.

The earlier handout's offline reference PNGs are reconstructions, not native Figma exports. They may differ from original geometry, clipping, crops, effects and instance overrides. The subsequent community-file exports described below are native exports. No export tooling is included in the mod.

Minecraft and related intellectual property belong to Mojang Studios and Microsoft. This project is not affiliated with or endorsed by them. Minecraft's font binaries are not redistributed. The community design license does not establish rights to third-party fonts, trademarks, screenshots, or other third-party content.

## Additional community component reference

At the user's request, additional components are adapted from **OreUI — Unofficial Ore UI cross-framework component library (Community)**: https://www.figma.com/design/xKzVWCKPVLiBHli5vEKtnL/ . The source canvas is `3:4`; its pixel-element icon collection is `3:5`. References include accordion (`63:25`), progress bar (`63:26`), dropdown (`56:201`), NavBar (`57:598`), table (`57:603`), text field (`40:7`), slider (`37:36`), tooltip (`28:156`), modal (`65:80`), card (`71:52`), scrollbar (`28:255`) and checkbox (`25:92`). These are adapted as native widgets using Minecraft's font, responsive layouts and application-supplied content.

The earlier community icon/animation atlas and its resource metadata were removed after the user supplied replacement static icons and loading/progress sheets. No icon/animation PNGs from this community file remain packaged. Component-design references and their historical attribution remain relevant. Reference canvases, the example world screenshot and source fonts are not packaged; image-card content is application supplied.

The user authorized use of this reference and conversion of its pixel icons into a sprite. Its upstream authorship and redistribution/license terms have not been independently established. The earlier handout's CC BY 4.0 attribution is **not** asserted to license this separate community file. Confirm those upstream terms before public redistribution; the code's MIT license does not license third-party design assets.

The mod logo `oreui-logo.png` is the image supplied by the user as `OreUI Lib.png`, copied unchanged for the mod-list icon.

## User-supplied pixel icon redraws

The user supplied `x15_icons.psd` and `x16_icons.psd`, then explicitly requested cleanup and integration. These contain 59 hard-edged pixel icon redraws. Their original names were opposite their actual canvas sizes. Corrected editable sources live outside mod resources at `artwork/icons/icons-15.psd` and `artwork/icons/icons-16.psd`; original Desktop files were not modified.

The mod includes the cleaned individual PNGs in `assets/oreui_lib/textures/icons/native/`. `assets/oreui_lib/native-icon-attribution.json` records source file/layer names, original PSD hashes, native/painted dimensions, removed off-canvas pixels, documented hand-redraw changes and final PNG hashes. Bag and Large Hashtag were redrawn on the integer grid for consistent visual size/stroke weight; intentional small/large variants remain distinct. No source PSDs or import tooling are bundled in the JAR.

Default static controls use these redraws. Redundant earlier explicit-size PNGs were removed; enum/size-based APIs redirect to current artwork. The redraws were supplied as adaptations of the referenced icon vocabulary; this record does not assert a separate asset license or erase historical upstream attribution/terms. The MIT code license is not a substitute for third-party asset rights.

## User-supplied loading/progress artwork

The user supplied `loading.png` and `progress.png` as replacement GUI artwork. Both are 15×240 transparent PNGs containing sixteen vertical 15×15 frames. They are copied unchanged into `assets/oreui_lib/textures/gui/`; `assets/oreui_lib/animation-attribution.json` records original filenames, SHA-256 hashes, dimensions and frame behavior. The original Desktop files were not modified, and no interpolation, resizing or recoloring was applied to the packaged PNG bytes. Runtime tinting and integer frame placement are library behavior.

These replace the former community loading/progress atlas, which is no longer bundled. This record documents the user-provided artwork and authorization, not a separate asset-license assertion.

## Optional Curios compatibility

The player preview's optional equipment adapter integrates with **Curios API**, by TheIllusiveC4 / C4: https://github.com/TheIllusiveC4/Curios . Upstream Curios sources identify their license as GNU LGPL version 3 or later (https://www.gnu.org/licenses/lgpl-3.0.html).

The build references `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` from its official Maven repository, compile-only by default. The adapter calls Curios APIs and its slot-handler constructor; no upstream source, classes, accessory assets or Curios JAR are copied or bundled into this library. Users supply Curios separately when desired; the optional development-runtime flag resolves it as a separate dependency. Accessory models/textures are provided at runtime by Curios-compatible mods, not redistributed here.

## Gradle wrapper

The official Gradle 9.2.1 wrapper scripts and JAR are distributed without modification, retaining upstream notices. Gradle's upstream license text is retained in `gradle/LICENSE`. Gradle is licensed under Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0 . The wrapper is build tooling and is not included in the mod JAR.
