# Interactable test screen

Join a world with the library installed on the client and run:

```text
/oreui test
```

This is a client command, available without cheats or operator permission. Opening is queued until the current chat submission has finished closing, so chat does not immediately close the test screen. No server command or network message is used.

The screen fills the Minecraft window's GUI area; it does not change the operating system's fullscreen setting. It uses the standard screen pause behavior in singleplayer.

For a separate persistent in-game demonstration, use `/oreui hud` and F7 (or `/oreui hud edit`) for cursor mode. This does not open the test screen or pause gameplay; see [HUD integration](hud.md).

## Controls

- The navigation sidebar selects ten pages. A light top NavBar provides Back, the centered title and a page-count action that opens Navigation. There is no About page; design credits remain in the mod metadata and packaged third-party notices.
- **Buttons:** five source palettes, counter, icon-only/text-and-icon/vertical content, seamless appearance, selectable icon, and five disabled-palette examples with an enable switch.
- **Selection:** switches and selection buttons remain; checkboxes use the new 12-pixel style with checked/mixed/off and disabled examples. Continuous, incremented and segmented sliders sit alongside the replacement captioned dropdown. The Controls switch enables/disables the interactive sliders/dropdown.
- **Text inputs:** live preview, working search, numeric input and key/mouse binding. Text/numeric fields use the label/input/description variant; classic `OreEditBox` remains available to consumers.
- **Colors:** two inline RGB editors with a saturation/brightness square, hue strip, hex entry and checkerboard-backed swatches directly on the page. Changes apply immediately without a popup or Done button. Six-digit hex is opaque RRGGBB; eight-digit hex is AARRGGBB. RGB edits preserve existing alpha without a separate alpha slider. Incomplete hex leaves the last valid color unchanged. Page changes and resizing preserve edits.
- **Feedback:** labels/decorations, both classic and thin outlined progress bars, existing and pixel spinners, a pixel progress ring, four tooltip silhouettes and the replaced hover/focus tooltip renderer, plus the existing toast.
- **Cards:** existing text-only cards/banners plus image cards with tags, captions/counts and an independent footer-action variant. Images are supplied dynamically; the demo references Minecraft's stone texture.
- **Accordions & tables:** collapsed/expanded text accordions, a striped sortable table with numeric Count ordering, and a horizontal scrollbar example.
- **Media:** browser for all 59 cleaned PSD icons, with native/enlarged previews, dimensions and a live icon-button example. Both 15×15 and 16×16 artwork keep integer pixels. The user's loading/progress strips and Minecraft's stone texture remain below. Legacy APIs redirect to current artwork; unused legacy PNGs and the old Figma animation atlas are removed. See `native-icons.md` and `loading-animations.md`.
- **3D previews:** native item/block models, floating/spinning motion, code-driven offset and rotation, block-state selection/zoom, and a separate player initialized with your equipped hands/armor and optional Curios accessories. Refresh copies current gear again; the loadout dropdown keeps explicit animation examples. Vanilla swing, item-use, shield, walking and crouching controls remain available. Preview controls never change the real player's inventory or perform gameplay actions. See `3d-previews.md` for the API and ticking requirements.
- **Navigation:** tabs/pagination/popups plus a modal with labeled input, required confirmation checkbox and fixed actions. All actions affect only local demo state. The gallery and modal use Minecraft's default font; `fonts.md` documents the optional API for application-supplied fonts.
- Scroll with the mouse wheel, drag the new vertical scrollbar or click its rail. Focused scrollbars accept arrows, Page Up/Down and Home/End. The horizontal example changes its displayed offset.
- Tab and Shift+Tab navigate controls; Enter or Space activates a focused button. A held activation key does not repeatedly activate an Ore button. Native slider keyboard behavior is retained: arrows adjust it while in adjustment mode; Enter/Space toggles that mode.
- Keyboard focus automatically scrolls a content control into view. Hidden and disabled controls are skipped. Raised buttons retain focus while pressed but suppress the white focus edge until release; switches outline only the handle. Other control focus outlines are unchanged.
- Escape or Back closes the topmost screen/layer first. Popups use NeoForge's native GUI layer stack and receive input instead of the gallery. Dropdowns also dismiss on an outside click, consuming that click rather than activating the gallery underneath.
- A new `/oreui test` starts with fresh values. Page changes and window/GUI-scale resizing keep existing widgets, text selection, edited values, click count, tab selection, and each gallery page's scroll position where it still fits.
- Numeric input permits intermediate decimal/scientific entries. Enter or focus loss commits finite numbers; invalid/incomplete values revert to the previous committed value without a change callback.
- Keybind capture changes only a local UI value, not Minecraft's gameplay mappings. Press any key or click a mouse button; Escape cancels and Backspace clears. Held activation keys that opened a popup are ignored until released.

The text field delegates selection, cursor movement, clipboard handling, and editing to Minecraft's `EditBox`. It is not a new editor and does not claim multiline or undo/redo features that have not been implemented.

See [Widget API](widgets.md) for component APIs and lifecycle. Incremented/segmented sliders snap both pointer and programmatic input; their arrows move one interval. Accordions activate only through their headers. Table headers sort on click; keyboard Left/Right chooses a column and Enter/Space toggles its direction.

## Public API

All UI types live under `com.kyivsec.oreui.client`. Only use them from client-side code.

Add an Ore button to an ordinary Minecraft screen with its inherited `addRenderableWidget` method:

```java
addRenderableWidget(new OreButton(20, 40, 140, 28,
        Component.literal("Apply"), OrePalette.COLORED,
        button -> applyChanges()));
```

`OreButton` extends Minecraft's `Button`; it retains `Component` labels, native tooltips, narration, active/visible flags, and focus integration. Callbacks run on press, matching vanilla buttons, not on release. `setSelected` adds a selection marker without hiding focus. `cancelPress` clears transient visual/key state when an owner cancels input or is removed. Ordinary release does not invoke the callback again.

```java
OreSlider slider = addRenderableWidget(new OreSlider(20, 80, 180, 34,
        Component.literal("Progress"), 0.5, this::updateProgress));
slider.setProgress(0.75);
```

`OreSlider` extends `AbstractSliderButton` and uses a normalized value in `[0, 1]`. `getProgress` reads it; `setProgress` clamps finite values, rejects NaN/infinity, and notifies only when the value changes. Construction does not notify. Native dragging and keyboard adjustment are retained; unavailable controls reject keyboard and drag input. Give it at least 16 pixels of width and 28 pixels of height.

`OrePalette` centralizes the five color families. Labels keep palette text colors without text shadows. `OreButton.setAppearance` selects raised, seamless, or navigation rendering; `setIcon` and `setVerticalContent` share the same activation behavior rather than adding per-variant classes.

Other public examples are `OreToggle` (checkbox/radio/switch), `OreDropdown`, `OreNumberField`, `OreKeybindButton`, `OreColorPicker`, `OreLabel` (label/tag/badge/indicator/divider/banner), `OreProgressIndicator` (bar/spinner), `OreCard`, `OreImage`, `OreToast`, and `OreNavigationBar`. Toggle/dropdown/color callbacks notify only real changes; programmatic navigation selection does not invoke its user-selection callback. The gallery coordinates radio exclusivity with ordinary callbacks; a radio control does not deselect itself on repeated activation.

`OreItemPreview`, `OreBlockPreview` and `OrePlayerPreview` share code-controlled transforms and clipping through `OreModelPreview`. They are automatically ticked when registered and visible in an `OreScreen`; ordinary Minecraft screens must call their `tick()` methods. See [Native 3D previews](3d-previews.md).

`OreNavigationBar` extends native `LinearLayout`; use `setWidth`, `setPosition`, and `visitWidgets(this::addRenderableWidget)` to integrate it into an ordinary screen. Choose vertical orientation for a navigation sidebar or horizontal orientation for tabs.

`OreColumn` arranges native widget children, navigation bars, and padded `OreSection` groups at a common width. Sections render recessed backgrounds separately from their children. `OreScreen` adds a clipped scroll viewport to a normal Minecraft screen and paints selection/focus outlines after neighboring widgets. Subclasses set `viewport`/`contentHeight`, register content with `addContentWidget`, and implement `arrangeContent` so coordinates agree with `scrollOffset`. `OrePopupScreen` combines these with a measured native column and ordinary NeoForge GUI layers:

```java
minecraft.pushGuiLayer(new OrePopupScreen(Component.literal("Confirm"), popup -> {
    popup.add(new OreLabel(popup.font(), Component.literal("Apply changes?"), 100));
    popup.addAction(Component.literal("Apply"), this::applyChanges);
}));
```

`addAction` closes its layer before invoking the action. Its overload accepts an `OrePalette`; confirmations and Done actions use `COLORED` for a green primary action, while cancellation remains `GENERAL`. Use `add` for controls that should leave the popup open. `anchor` accepts a live `ScreenRectangle` supplier for an anchored, outside-click-dismissible menu. Standard centered popups block outside clicks without dismissing. `OreColorPicker` and `OreKeybindButton` use the same popup implementation rather than separate input/modal systems.

`OreEditBox` adapts native `EditBox` drawing to a recessed surface. The text uses six-pixel horizontal padding; cursor hit testing and reported screen coordinates use the same padding. Editing/validation/clipboard behavior remains in Minecraft's editor. See `widgets.md` for composition and drawing helpers, and `native-icons.md` for the icon API.

These are initial element-family APIs, not complete resource-based themes, a full editor, every Figma button variant, or all container/layout/game-integration features from the handout.

## Manual verification checklist

These are checks to perform in-game, not claims of completed manual tests:

1. Open with `/oreui test`, without enabling cheats. Confirm the screen remains open after chat closes.
2. Click/hold the counter; release inside and outside; verify no duplicate release activation. Hold Space/Enter while focused and check that only one activation occurs.
3. Visit all ten pages. Verify both navigation bars, accordions/table, tab selection, checkboxes, exclusive radios, switch/disabled states, and dropdown selection with mouse and keyboard.
4. Edit/select non-ASCII text and copy/paste. Test search results, intermediate numeric entries and commit/revert, key/mouse capture and cancellation, and inline square/hue/hex synchronization. Check immediate color updates, square corners, drag clamping, arrow keys, Shift increments and alpha preservation. Change pages and resize; colors and hex text should survive. Short layouts scroll the inline editor normally.
5. Resize/change GUI scale on several pages and while a dropdown is open. Check values, text selection, active page, scroll positions, and anchored menu position. At the minimum normal GUI size (320×240), all ten navigation entries should remain reachable.
6. Scroll content, try clicking clipped controls behind the header/footer, and Tab into off-screen controls. Drag sliders outside and release; check clamping, keyboard adjustment, focus visibility, progress binding across pages, and spinner motion toggle.
7. Activate cards, tooltip, toast, popup, confirmation, and nested menu. Verify input does not leak through overlays; dismiss only the top layer with Escape. Hold Enter/Space while opening a popup and ensure it does not activate a second control until released.
8. Browse all icon names and the stone image; check clean icon silhouettes, the upright checkbox tick, centered text/icon groups, complete navigation outlines, a screen-centered title, aligned popup gutters, and the green confirmation action. Check all palettes, narration, and reopening a fresh session.
9. On 3D previews, change items and block states, toggle floating, rotation, zoom and offset, and verify model clipping while scrolling. Compare automatic motion at 60/120+ FPS in a paused singleplayer world and an unpaused client: floating, rotation, swing, walking and use poses should remain smoothly interpolated rather than stepping at 20 Hz. Pause/resume and change pages without pose jumps or catch-up bursts. Equip the preview player with a sword/bow/food/crossbow and test shield, crouching and reset. Confirm real equipment/inventory are unchanged and no projectiles, sounds, consumption or gameplay actions occur. Player previews outside a loaded world should show their placeholder.
10. Open the player demo with distinct armor and both hands equipped; verify all six slots, including glint/dyed/custom stacks. Remove or change gear, reopen or press Refresh, and verify old/empty slots are replaced. Check left-handed players, switching between current gear and demo loadouts, refreshing during item use, and narrow GUI-scale layouts. The examples must retain copied armor/accessories; Refresh must restore the real hands. Real stack counts, durability and data must stay unchanged during animations.
11. Repeat without Curios and with Curios 9.5.1 (`gradlew.bat runClient -PwithCurios` for development). Equip visibly rendered accessories, different cosmetic replacements, hidden slots and extra modifier-granted slots. Check captured regular/cosmetic counts, cosmetic precedence, visibility toggles, crouch/walk/swing poses and refreshing after removing accessories. Verify no equip/unequip/slot-resize events or accessory ticks occur on the preview; actual player effects/inventory remain unchanged. Accessories requiring additional mod-specific player data are outside the generic bridge's guarantee. These are pending runtime checks, not recorded passes.
