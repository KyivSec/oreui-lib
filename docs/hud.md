# Persistent HUD widgets

HUD layers use NeoForge's native GUI-layer registration. They do not open a `Screen`, pause gameplay, change the global font or require another rendering mod. Rendering is display-only unless a layer explicitly enables interaction and the user enters cursor mode.

## Demo

- `/oreui hud` toggles the local demo; `/oreui hud on` and `off` set its visibility.
- `/oreui hud edit` enables it and enters cursor mode. **F7** toggles cursor mode for the topmost eligible interactive Ore layer; remap this in Controls → Ore UI Library.
- **Escape** closes a HUD popover first, then exits cursor mode. Tab/Shift+Tab traverses widgets. Sliders retain their native adjustment-mode controls.
- The demo has progress, a slider, a counter, an anchored dropdown and an isolated floating block preview. All values are local UI state.

## Registering a layer

Call from your client `RegisterGuiLayersEvent` listener:

```java
OreHudLayer layer = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "status"), () -> {
    OreGrid grid = new OreGrid(2, 1).gap(6).padding(8);
    grid.add(new OreLabel(Minecraft.getInstance().font, title, 180), 0, 0);
    grid.add(new OreProgressIndicator(label, OreProgressIndicator.Kind.BAR, this::progress), 1, 0);
    return grid;
}).size(200, 80).anchor(OreHudAnchor.TOP_RIGHT);
OreHudManager.register(event, layer);
```

`registerAbove` / `registerBelow` accept an already registered vanilla/own layer ID for ordering; a null reference means above/below all. IDs must be unique. Keep the layer instance to change `enabled`, size, anchor, offset or visibility policy. `content()` returns its lazily built grid; call `invalidateLayout()` after structural or height-changing edits. Content builders run after a world/resources are available and are recreated on world changes, so do not capture old world/player objects.

Nine anchors and explicit safe-area insets keep panels inside the GUI bounds. Safe areas are configurable; the library does not infer every vanilla/other-mod HUD's occupied rectangle. Oversized panels clamp to the available area and scroll overflowing content. Cell hit testing matches clipping. Scroll/drag capture and final focus outlines use the same native widgets/helpers as screens.

By default layers require a world/player, respect F1, hide behind screens/loading overlays and allow spectators. `showWithScreens`, `respectHideGui`, `showSpectator` and `visibleWhen` configure display policy. Input always yields to a real screen/loading overlay or an inactive window. Hidden layers suspend preview clocks without changing individual widget visibility. World changes discard old widget/preview state.

## Interaction safety

Use `.interactive(true)` and `OreHudManager.startInteraction(layer.id())` for an opt-in cursor mode. `stopInteraction()` exits it. Mouse capture is restored only when appropriate; opening another screen, losing focus, hiding/disabling the layer, F1 or leaving the world cancels capture. Cursor mode owns pointer/keyboard input so outside clicks cannot attack, use an item or re-grab the mouse underneath. F2 screenshots remain available. Held UI keys remain suppressed until release when returning to gameplay. Focused labels are announced through Minecraft's narrator.

Opening a regular screen or pushing a GUI layer immediately dismisses HUD popovers and clears HUD focus/drag capture without re-grabbing the mouse. Disabling a layer also dismisses its popovers immediately. Cancellation is silent: a slider plays its release click only after an actual pointer drag ends, not while hidden or during cleanup. Escape exits interaction; the persistent display remains enabled until explicitly hidden with `enabled(false)` or `/oreui hud off`.

NeoForge's raw key event is not cancellable. Small **client-only** keyboard/mouse callback mixins route input before vanilla gameplay handling; they do nothing when interaction is inactive (apart from suppressing already-consumed held-key releases/repeats). Rendering and layout remain native. No packets/game actions are sent by this host; consumers remain responsible for their own callbacks.

HUD dropdowns are bound to screen-independent anchored popovers with scrolling, keyboard selection, held-opening-key suppression and outside-click dismissal. `openPopover(grid, anchorSupplier, onClose)` also hosts custom inline content. Use `OreColorEditor` for inline color editing. Explicitly screen-opening widgets such as `OreColorPicker` and `OreKeybindButton` retain their screen behavior; if used, the HUD yields input to the new screen instead of pretending those dialogs are inline.

All widgets and layer mutations belong to the client thread. Native mouse capture, event-hook compatibility, narration and visual/input behavior must still be tested in-game: verify F1, resizing/scales, screens/popovers, focus loss, disconnect/reconnect and 60/120+ FPS previews before distribution. Automated host/policy tests are not a substitute for those checks.
