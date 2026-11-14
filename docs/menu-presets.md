# Grid-based menu presets

Presets define regions, not application behavior. All content remains ordinary native widgets/layouts, and all geometry is backed by `OreGrid`.

| Kind | Regions |
| --- | --- |
| `SETTINGS` | Merged header, sidebar beside scrolling body, merged footer |
| `SIMPLE` | Header, scrolling body, footer |
| `SPLIT` | Merged header, preview beside controls/body, merged footer |
| `MODAL` | Compact header, bounded scrolling body, fixed actions |
| `ANCHORED` | Compact body positioned relative to a live trigger rectangle |

## Ready-to-use screens

```java
minecraft.pushGuiLayer(new OreGridScreen(title, OreMenuPresets.Kind.SETTINGS, screen -> {
    OreGrid body = new OreGrid(2, 1).gap(8).padding(2);
    body.add(new OreTextField(screen.font(), 0, 0, 200, label, description), 0, 0);
    body.add(new OreSlider(0, 0, 200, 34, sliderLabel, value, this::setValue), 1, 0);
    screen.body(body);
    screen.sidebar(new OreNavigationBar(pages, LinearLayout.Orientation.VERTICAL, this::selectPage));
}));
```

`OreGridScreen` provides native screen-layer behavior, body scrolling, clipping-aware pointer input, focus reveal and held-opening-key suppression. The title/header and Back footer are provided by default. Supply `header`, `sidebar`, `preview` and `footer` in the build callback as supported by the selected kind. A footer can itself be a multi-column grid of actions. Only the body scrolls; header/sidebar/preview/footer stay fixed. Regions must not share widget instances.

The default title has equal left/right padding inside the header (eight pixels with the default options), preserved when resizing. Custom header layouts retain their own content padding.

`size(width,height)` limits the preferred menu dimensions. Full menus fill the available inset area up to 1104 pixels wide; modal/anchored menus default to 240×200. `options(...)` configures padding, gaps, fixed header/footer heights, sidebar width and body minimum width. `layout()` exposes the composed grid and named region bounds. Chrome widgets remain mutable, but replacing chrome is a build-time operation. `body(newGrid)` can replace body content later; `reflow()` registers newly added body widgets, remeasures changed/wrapped heights, and clamps the existing scroll position without recreating surviving fields.

For anchored menus, set `.anchor(trigger::getRectangle)` before opening, and optionally `.size(180,112)`. Menus prefer below the trigger, flip above when useful and clamp to an eight-pixel GUI edge. Outside clicks/Escape dismiss the top screen layer without activating the screen below. Anchored presets contain only a body.

The Navigation gallery page opens all five preset examples. Their controls modify only local demo state; they do not implement real settings persistence or inventory behavior.

## Layout-only integration

`OreMenuPresets.settings(header,sidebar,body,footer)`, `simple`, `split`, `modal` and `anchored` return an `OreMenuLayout`. Pass caller-owned `LayoutElement` content, set size/position and register widgets through `visitWidgets`. `bounds(region)` returns a track region; `contentBounds(region)` excludes its cell padding. Header/footer spans and row/column constraints are already defined. Configure the returned `grid()` or put another grid inside a region for additional merges/limits.

For per-cell clipping, render the layout once and register children for input only, following `grid.md`. A layout alone clips overflowing body content; `OreGridScreen` adds the scrolling host and input routing. Fixed track minima do not shrink silently if the window is too small: choose appropriate limits and test overflow at your supported GUI sizes.

Existing `OrePopupScreen` / `OreModalScreen` constructors remain available. Their established header/body/footer geometry now comes from a compatible grid preset; existing spacing, colors and action behavior remain unchanged.
