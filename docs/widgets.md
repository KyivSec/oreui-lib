# Widget API

All types live under `com.kyivsec.oreui.client` and must be used from client-side code. Widgets use Minecraft's native input, layout, rendering and narration facilities.

See [Custom UI fonts](fonts.md) for application-supplied fonts, per-widget selection and modal/popup typography.

## Components

| Type | Behavior |
| --- | --- |
| `OreAccordion` | Wrapped text, `isExpanded` / `setExpanded` and callback. Only the header accepts mouse activation; Enter/Space also toggles expansion. |
| `OreProgressIndicator` | `BAR`, `OUTLINED_BAR`, `SPINNER`, `PIXEL_SPINNER` and `PIXEL_RING` variants. Determinate variants read a supplied progress value. |
| `OreDropdown` / `OreDropdownScreen` | Captioned trigger and anchored selection rows. Overflow scrolls. Outside click/Escape dismisses; opening held keys are suppressed. |
| `OreNavBar` | Top header with centered title, back and status actions. Distinct from sidebar/tab `OreNavigationBar`. |
| `OreTable` | Grouped heading, row/column headings and sorting. Left/Right chooses a header; Enter/Space sorts. `setComparator` supports numeric/domain ordering. |
| `OreTextField` | Labeled field with description. Inherits native editing/clipboard/validation. `OreEditBox` provides the plain variant. |
| `OreSteppedSlider` | Equal increments, optionally visible separators. Pointer/programmatic values snap; arrows move one step and Home/End select endpoints. `OreSlider` provides a continuous range. |
| `OreTooltip` | Top/bottom/left/right tails. `OreScreen` retains native tooltip scheduling and narration. |
| `OreModalScreen` | Back/title/close header, scrollable body, fixed actions and optional confirmation. `OrePopupScreen` provides a standard popup. |
| `OreImageCard` | Caller-supplied, aspect-preserving center-cropped image, tags, caption/count and optional independent footer action. `OreCard` provides a text card or a selectable preview/caption card. |
| `OreScrollbar` | Vertical/horizontal scrolling, dragging, track clicking and keyboard input. |
| `OreToggle` | Checkbox, radio and switch variants, including disabled state. Checkboxes support palettes and an indeterminate state. |

Accordions hold wrapped text, not arbitrary interactive children. Tables render supplied rows rather than virtualizing large datasets; place them in an `OreScreen` for scrolling. Card images/content are application-supplied.

## Usage and lifecycle

```java
OreAccordion accordion = new OreAccordion(font, title, body, false,
        expanded -> relayoutYourScreen());
OreTextField field = new OreTextField(font, 0, 0, 200, label, description);
OreSteppedSlider slider = new OreSteppedSlider(label, 10, 0.3, true,
        normalizedValue -> updateYourValue(normalizedValue));
OreProgressIndicator bar = new OreProgressIndicator(label,
        OreProgressIndicator.Kind.OUTLINED_BAR, () -> progress);
```

Relayout the screen when an accordion changes height. Slider `steps` counts intervals (1–1000); values stay normalized to `[0,1]`. `getStep()` is the integer index from 0 through `getSteps()`. Only a changed step invokes its callback; construction does not.

Each `OreTable.Row` has a heading and a defensively copied cell list matching the column count. `getRows()` is read-only. Default sorting compares displayed strings case-insensitively; a numeric comparator can use `table.setComparator(1, Comparator.comparingInt(c -> Integer.parseInt(c.getString())))`. `sortBy(column)` toggles direction, defaulting to ascending on a new column.

```java
minecraft.pushGuiLayer(new OreModalScreen(title, modal -> {
    modal.add(new OreTextField(modal.font(), 0, 0, 176, label, description));
    OreButton confirm = modal.addAction(confirmLabel, OrePalette.COLORED, this::confirm);
    confirm.active = false;
    modal.addConfirmation(checkboxLabel, false, checked -> confirm.active = checked);
    modal.addAction(Component.translatable("gui.cancel"), () -> {});
}));
```

Modal back/close/Escape cancel without invoking confirmation. Actions close the layer before invoking callbacks. Keep actions short enough to fit the fixed footer.

For a standalone scrollbar, `setMetrics(contentSize, viewSize, offset)` silently clamps layout state; `setValue` and user input notify only changed offsets. Its callback must move actual content. `OreScreen` manages a vertical scrollbar automatically.

`OreNavBar` is a `LayoutElement` and `Renderable`: register its background using `addRenderableOnly(navbar)` and controls using `navbar.visitWidgets(this::addRenderableWidget)`. Set its position/width in screen layout. `OreImageCard` is a native `LinearLayout`; register child widgets via `visitWidgets`. `OreColumn` / `OreSection` size it automatically. Ordinary widgets work in vanilla screens; `OreScreen` supplies the custom tooltip and final focus-outline pass.

## Buttons, selection and navigation

`OreSlider.setLabelVisible(false)` hides the visible label/value and centers its track in a compact widget (for example, 18 pixels high). Its message, narration, keyboard input and pointer mapping are unchanged. `isLabelVisible()` reports the setting. `OreSteppedSlider` supports the same mode. Existing sliders show labels by default.

`OreValueButton(label, value, onPress)` is a compact 20-pixel label/value action row with a chevron, hover/focus selection and native button input. `setValue(Component)` updates the displayed value and accessible message; `setSwatchColor(Integer)` optionally replaces the visible value with a color swatch (null restores text). Keep the value component synchronized with the swatch for narration/tooltips. It is useful for focused settings submenus without permanently expanded controls.

`OreLabel.setTextCentered(true)` centers a plain label's text block inside its widget independently of grid-cell alignment. Plain labels remain left-aligned by default. This is useful for fixed-width page counters whose widgets are already centered in a layout cell.

### Selectable preview cards

The original text-only `OreCard(font, title, body, accent, onPress)` constructor is unchanged. For selectable catalogue entries with room for images or future 3D content:

```java
OreCard card = new OreCard(font, title, 64,
        (graphics, bounds, mouseX, mouseY, partialTick) -> {
            // Draw caller-owned preview content inside these screen-space bounds.
        }, button -> selectOption());
card.setWidth(112);
card.setSelected(isCurrentOption);
```

This overload creates one button/focus target across the preview and caption, with a green selected caption, native keyboard/pointer activation, a focus outline and the title as its accessible message. It adds no tooltip, tags or independent footer actions. The initial height is `previewHeight + PREVIEW_CAPTION_HEIGHT + 2`; the caption is 22 pixels high. `previewBounds()` and `captionBounds()` are recomputed after movement/resizing, so a bounded grid can assign the final card height. The callback is clipped to the preview area and receives a balanced pose scope; buffered draws are flushed before the clip is released. It must balance its own nested rendering state.

Preview cards paint a complete one-pixel frame on all four sides **after** their content and caption. This permanent frame is distinct from the focus outline. Right/bottom borders use the same visible frame color as the top/left, not background-colored bevel shading; selection colors the whole perimeter green.

Preview widgets/models remain caller-owned: the card does not create entities, tick nested previews, route input to them or release their resources. Use non-interactive content and service/release any future 3D previews in the owning screen. This keeps catalogue selection separate from orbit controls. Existing text-only card sizing/rendering remains unchanged.

`OreButton.setIcon(ResourceLocation, iconOnly)` accepts standalone **16×16 PNGs from any mod/resource namespace**, not just OreUI's icon enums. Keep app-specific artwork in the app's own `assets/<mod_id>/textures/...` tree; no library registration or asset copy is needed. For example:

```java
button.setIcon(ResourceLocation.fromNamespaceAndPath("your_mod", "textures/gui/icons/head.png"), true);
button.setAppearance(OreButton.Appearance.SEAMLESS);
button.setIconSize(16);
```

Explicit `setIconSize` values apply to all button appearances, including seamless and stacked reference controls. Icon-only content is centered across the button rather than constrained by text padding. Without an explicit size, the previous raised/reference icon sizes remain unchanged. Setting an icon does not remove the button's message/narration or automatically add a tooltip.

`OreButton.setAppearance` selects raised, seamless, selection or navigation rendering. `setSelected` on a default raised button enables selection appearance; `setBadge(Component)` attaches a count badge. `setIcon`, `setIconSize` and `setVerticalContent` configure content without changing activation behavior. Callbacks run on press, not release. `cancelPress` clears transient input state.

`OreToggle.setSwitchPalette` and `setSwitchSymbols` configure switch rendering without changing its value or invoking callbacks. `setCheckboxPalette` and `setIndeterminate` configure checkbox variants. A radio does not deselect itself on repeated activation; coordinate exclusivity through callbacks.

`OreNavigationBar` uses native `LinearLayout`. Set width/position, register its background with `addRenderableOnly`, then call `visitWidgets(this::addRenderableWidget)`. Vertical navigation supports `setHeader`, `setGroupHeading` and row icons; horizontal orientation provides tabs.

`new OrePagination(pageCount, initialPage, callback)` has finite endpoints and disabled endpoint arrows. The two-argument constructor provides an open-ended five-page window. Programmatic navigation selection does not invoke its user-selection callback.

## Layout and drawing

```java
OreSection section = new OreSection(font, Component.literal("Actions"));
section.add(new OreButton(0, 0, 160, 28, Component.literal("Apply"),
        OrePalette.COLORED, button -> applyChanges()));
section.setWidth(220);
section.setPosition(20, 50);
addRenderableOnly(section);
section.visitWidgets(this::addRenderableWidget);
```

Render a section background before its children, or in `OreScreen.renderContentBackground`. `OreSection` adds 12-pixel inner padding and eight-pixel control spacing. `OreColumn` arranges ordinary widgets and supported composite layouts at a common width.

`OreDrawing` exposes `surface`, `bevel`, `field`, `track`, `raisedButton`, `buttonFaceBounds` and focus helpers. `OrePalette` supplies the five color families. `scaledText` clips overflow using screen-space scissors; do not pass pose-local coordinates to `GuiGraphics.enableScissor`.

`OreGrid` provides bounded rows/columns, weighted flexible tracks, per-cell limits, padding/alignment and merged cells. See [Grid layout](grid.md) for sizing, overflow and input-clipping rules.

`OreHudLayer` hosts persistent widgets during gameplay without a screen. See [HUD integration](hud.md) for native layer registration, display policies and explicit cursor mode.

`OreStatBar(label, color, currentSupplier, maximumSupplier)` is a compact display-only colored value/maximum bar. It samples live values for rendering/narration, clamps invalid/out-of-range values, and supports native grid resizing. The opt-in `OreHudStatTest.LAYER` example demonstrates red health and light-brown hunger bars.

`OrePlayerPreview` accepts an `OrePlayerAppearance` for standalone classic/slim native model rendering, including on the main menu without a world/player. Its original entity/equipment/Curios path remains available. See [3D previews](3d-previews.md) for appearances, texture ownership, resource reloads and custom part/layer hooks. The standalone example is available only through an explicit `OrePlayerPreviewDemo.open()` call.

`OreMenuPresets` and `OreGridScreen` provide settings, simple, split-preview, modal and anchored menu shells composed from grid regions. See [Menu presets](menu-presets.md) for the public APIs and fixed-chrome/scrolling-body behavior.

`OreScreen` paints focus/selection outlines after neighboring widgets. In a plain Minecraft screen, call a widget's `renderOutline` after its siblings when their drawing could overlap the outline.

## Icons and previews

See [Native icons](native-icons.md) for static glyphs and compatibility, [Loading animations](loading-animations.md) for frame selection, and [3D previews](3d-previews.md) for model transforms, equipment snapshots and lifecycle. The opt-in [test screen](test-screen.md) demonstrates these APIs without registering commands or main-menu buttons.

## Color selection

For an embedded editor with immediate changes:

```java
OreColorEditor editor = new OreColorEditor(font, Component.literal("Accent"), 0xFF387E24,
        color -> updateAccent(color));
editor.setWidth(128);
editor.setPosition(20, 40);
editor.visitWidgets(this::addRenderableWidget);
```

`OreColorEditor` is a native `LinearLayout` containing the square, hue strip, preview and hex field; it never opens a popup. `getColor` reads the current ARGB value; `setColor` synchronizes all controls and notifies once per changed color. Valid hex and pointer/keyboard edits apply immediately. Incomplete hex keeps the last valid color. Width adapts between 32 and 128 pixels, keeping the square compact in a scrolling page. `OreColumn` / `OreSection` size it automatically; register its children with `visitWidgets`. The Colors gallery uses this inline editor.

For a button that opens a transactional popup instead:

```java
OreColorPicker picker = new OreColorPicker(Component.literal("Accent"), 0xFF387E24,
        color -> updateAccent(color));
addRenderableWidget(picker);
```

`OreColorPicker` opens a square saturation/brightness field and a hue strip. Drag horizontally for saturation and vertically for brightness; the strip chooses hue. The popup uses inset borders, a square selection marker, a beveled hue handle and white focus edges. Its body scrolls on short layouts; Done remains fixed.

Only Done commits the draft and notifies the picker callback. Escape/close discards it. `getColor` reads the committed ARGB integer; `setColor` updates it and notifies only on an actual change. Hex accepts `RRGGBB` (opaque) or `AARRGGBB`, with an optional `#`. `format` returns `#AARRGGBB`; `parse` rejects malformed/incomplete input. An incomplete popup entry commits the last valid draft. RGB edits preserve incoming alpha; there is no alpha slider.

The field and strip are reusable independently:

```java
OreColorSquare square = new OreColorSquare(Component.literal("Color"), 0xFF387E24,
        color -> updatePreview(color));
square.setWidth(128); // Height follows width; minimum size is 8.
OreHueSlider hue = new OreHueSlider(Component.literal("Hue"), square.getHue(), square::setHue);
// Position and register both widgets using the owning screen's normal layout.
```

`OreColorSquare` exposes `getColor`, `getHue`, `getSaturation`, `getBrightness`, `setColor`, `setHue` and `setSaturationBrightness`. Coordinates are normalized to `[0,1]`; setters clamp finite values and reject NaN/infinity. Its callback reports only changed ARGB colors, not hue changes that leave a black/gray color unchanged. Gray retains the last hue; black retains hue and saturation for later edits. `OreHueSlider` exposes `getHue` / `setHue` with a callback for changed hue coordinates; 0 and 1 both render red.

Tab focuses each control. On the square, Left/Right adjusts saturation and Up/Down adjusts brightness by 1%; Shift uses 10%. Hue arrows move one degree, or ten with Shift; Home/End selects either endpoint. Disabled/hidden controls reject edits. Dragging clamps beyond the edges and stops on release/focus loss. `OreScreen` also cancels drags on layout changes, window deactivation and removal; plain screens can call `cancelDrag` themselves. Narration reports the current coordinates/color and keyboard instructions.

When synchronizing external hex input, set both `square.setColor(parsed)` and `hue.setHue(square.getHue())` with a reentrancy guard. Construction does not invoke either callback. In a plain screen, call each widget's `renderOutline` in the final focus pass if neighboring drawing could cover it.
