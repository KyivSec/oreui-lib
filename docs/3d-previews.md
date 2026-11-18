# Native 3D previews

The client-only preview widgets use Minecraft 1.21.1's native item/block renderers and player models/renderers. There are no required additional rendering dependencies, framebuffers, spawned entities or bundled model/skin assets. Curios equipment support is optional.

## Widgets

- `OreItemPreview(Component label, ItemStack item, int height)` renders an item or block's item model. It copies its input stack. The default display context is `GROUND`; use `setDisplayContext(ItemDisplayContext.GUI)` for the model's inventory presentation. Native model geometry and custom item renderers are retained; a flat sprite does not become a full 3D mesh. Fitting uses the model's declared display-transform scale; unusual custom models can be adjusted with `setModelScale`.
- `OreBlockPreview(Component label, BlockState state, int height)` renders a specific block state rather than its inventory representation, including properties such as stair/slab orientation or a furnace's lit state. It uses the single-block renderer with full-bright lighting and empty model data. This is not a miniature world: world-dependent neighbor/model data, fluids and arbitrary block-entity NBT/animations are not simulated.
- `OrePlayerPreview(Component label, int height)` preserves the entity-based renderer: a separate, unregistered `RemotePlayer` with the current player's name/skin but a preview-private UUID. It requires a loaded client world/player; otherwise it shows a placeholder. It never uses the actual player as the animation target.
- `OrePlayerPreview(Component label, int height, OrePlayerAppearance appearance)` defaults to a **standalone** native `PlayerModel` renderer. It works on the main menu with no world/local player, fake world, entity, profile lookup or skin download. `setAppearance(appearance)` updates the appearance using the selected mode; `useEntityRendering()` removes the appearance override and restores account-skin/entity rendering with the stored equipment snapshot. `getAppearance()` is null when there is no appearance override.

### Renderer-backed appearances (EMF and player-animation hooks)

```java
OrePlayerPreview preview = new OrePlayerPreview(label, 180, appearance);
preview.setAppearanceRendering(OrePlayerPreview.AppearanceRendering.PLAYER_RENDERER);
```

`PLAYER_RENDERER` uses the normal `EntityRenderDispatcher` and installed player renderer, including `setupAnim`, entity render events, renderer replacements and render layers. This provides the entity context needed by ETF/EMF and render-driven player animation mods, without a compile/runtime dependency on those optional mods. The preview entity overrides `getSkin()` and visible skin parts from `OrePlayerAppearance`, so draft texture and Classic/Slim changes reach the native renderer. Appearance updates retain the existing preview entity and its animation state; the camera is unaffected.

Each widget uses its own unregistered `RemotePlayer`, private UUID/inventory/team, and a snapshot of the source player's name/account skin. UUID-keyed mod animation state is not borrowed from the actual player. The initial position matches the source for world-dependent resource-pack predicates; no movement, world/entity ticks, game actions or networking are simulated. Dispatcher camera orientation, shadows and debug-hitbox state are temporarily overridden and restored even when rendering fails. Renderer models themselves belong to the normal dispatcher, not the standalone model cache.

Without a loaded client level/player, a supplied appearance automatically uses standalone rendering; the original no-appearance constructor still shows its unavailable placeholder. `STANDALONE` remains the default. `getAppearanceRendering()` reports the selected appearance mode; `isStandalone()` reports whether a supplied appearance is configured as standalone, not whether the compatible mode is temporarily falling back. Switching the selected mode releases entity/model caches but retains camera, appearance and equipment snapshots. Standalone model factories, pose modifiers and custom layers run only on the standalone path/fallback, not alongside the normal player renderer.

**Compatibility limits:** EMF still needs ETF, player CEM assets and settings that allow remote players (`onlyClientPlayerModel` must not exclude the preview). A mob-only resource pack does not supply player animations. UUID-dependent random model choices can differ from the real player. Mods restricted to `LocalPlayer`/the actual camera entity, attachments keyed to the actual account UUID, tick-only animation controllers or server-triggered emotes need dedicated adapters. Third-party hooks are not sandboxed, and their own clocks may ignore OreUI animation pause. Native dispatch is a compatibility entry point, not a guarantee for every animation mod; verify specific mod/pack combinations in-game. No animation state is copied from the live player.

All previews have native widget position/size, labels/narration and optional inset backgrounds. By default they do not take keyboard focus or consume clicks. Opt-in orbit/zoom interaction is described below. They can have a normal native tooltip assigned with `setTooltip`.

## Optional orbit and zoom

```java
preview.setRotation(-8, -25, 0);
preview.setInteractive(true); // Records these transforms as the home/reset view.
preview.setZoomToCursor(false); // Optional: keep zoom centered on the current model focus.
preview.setCameraTransitionDuration(0.3); // Optional: smooth 300 ms preset/reset transitions.
preview.focusView(0.75F, 3);  // Standalone player head framing; preserves orientation.
preview.resetView();         // Restores the recorded camera, not appearance or equipment.
```

`setInteractive(true)` works on item, block and player previews. Left-drag rotates through continuous horizontal turns with vertical tilt clamped to ±60 degrees. Wheel zoom is bounded to 0.75–4 times the fitted model size, anchoring the point under the cursor; it is accepted only inside the preview. `focusView(targetY,zoom)` uses a model-space vertical target and recenters screen offsets without changing orientation. The standalone player's head is approximately `0.75` above its model center; different models need their own target.

`setZoomToCursor(false)` switches wheel and keyboard zoom to model-centered zoom: screen offsets stay zero and the current model-space focus remains unchanged, regardless of mouse position. Cursor anchoring remains the default for existing consumers.

Presets and reset are immediate by default. `setCameraTransitionDuration(seconds)` opts into smoothstep ease-in/out for `focusView` and `resetView`, including focus, zoom, offsets and shortest-path yaw/roll interpolation. The duration must be finite and nonnegative; zero restores immediate changes. Camera transitions use monotonic wall time and advance on both render frames and host ticks, independently of world partial ticks or stopped pose animation. Retargeting starts from the currently interpolated view. Direct camera setters, pointer rotation/zoom and keyboard rotation/zoom interrupt the transition at that view; focus loss/drag cancellation alone does not interrupt it, so clicking a preset button can move focus safely. Changing the transition duration or interactive mode also stops a pending transition at its current view.

Interactive previews participate in Tab navigation. Focused arrow keys rotate (Shift uses larger steps), `+`/`-` zoom and Home resets the view. `cancelDrag`, focus loss, hidden/deactivated hosts, and the shared `OreWidgetSupport.cancel` hook release pointer capture without resetting the view. Input must begin inside the preview, but an already-captured drag can continue outside until release. Camera state is private to each widget and unaffected by replacing its appearance.

`OreScreen` scrolls only its content viewport. When a preview is fixed chrome outside that viewport, route wheel input to `preview.mouseScrolled(...)` first in the owning screen, then fall back to `super.mouseScrolled(...)`. Never forward input through an overlay/popover. The character editor demonstrates this routing. Rendering still clips all zoomed content to the widget bounds.

## Standalone player appearance

```java
OrePlayerAppearance appearance = new OrePlayerAppearance(
        ResourceLocation.fromNamespaceAndPath("example", "textures/preview/skin.png"),
        OrePlayerAppearance.Model.SLIM); // or CLASSIC
OrePlayerPreview preview = new OrePlayerPreview(Component.literal("Player"), 180, appearance);
preview.setWidth(160);
preview.setWalking(true);
preview.setFollowMouse(true);
addRenderableWidget(preview);
```

`OrePlayerAppearance` is immutable and reusable across widgets. Supply a **modern 64×64 player-skin layout**, choosing classic (four-pixel arms) or slim (three-pixel arms). The library does not fetch, generate, normalize legacy 64×32 images, or store appearances. A third constructor argument accepts a defensive immutable set of `PlayerModelPart` values for the hat, jacket, sleeves and trouser overlays; all are enabled by default. `CAPE` does not draw a cape on this path—use a custom visual layer or the entity renderer.

Each standalone preview owns its own mutable model/pose cache. Textures/appearances can be shared, but poses cannot leak between previews. Native transforms (`setRotation`, `setModelScale`, `setModelOffset`), background/clipping, walking in place, crouching, mouse-follow, either-hand swing/loop and animation pause use the same UI clock without requiring entity state. Standalone swing is an entity-free six-tick arm gesture, not an item-specific vanilla attack animation. `playUseItem`, use loops, held items, armor and copied equipment are not rendered on this path; stored equipment remains available when switching back to entity rendering.

### Texture ownership and reloads

- **Resource/registered IDs are borrowed.** Constructing, replacing, switching or discarding a preview never releases its appearance's texture. Resource-pack textures reload through Minecraft's texture manager. Caller-registered external textures remain the caller's responsibility, including their reload behavior and cleanup.
- **Optional owned handle:** `OrePreviewTexture.register(NativeImage)` registers a dynamic skin and returns an `AutoCloseable` handle. It requires a 64×64 image and the render thread. Validation failure leaves the image with the caller; once registration starts, ownership transfers to the dynamic texture, including cleanup if registration fails. Do not close or mutate the transferred image yourself.
- Use `handle.location()` in one or more appearances. The dynamic texture retains its pixels through Minecraft resource reloads. The owner must stop all previews/layers from using that ID before calling `handle.close()` on the render thread. Close is idempotent and releases the texture-manager entry, native image and GPU texture; `location()` then throws. Neither appearances nor previews close shared handles automatically.
- Resource reloads invalidate standalone model caches. Each variant and custom model factory is rebaked lazily against the refreshed `EntityModelSet`; factories must return fresh preview-private models. No rebake is needed just to replace a skin ID.
- `releaseRenderResources()` drops preview-owned model/entity caches without closing borrowed textures/handles, changing appearance or discarding equipment. Ordinary garbage collection releases model objects once a widget is discarded; the models do not own texture registrations. Callers own the lifetime of their custom-layer resources too.

For example, after obtaining a skin image from your own resource/input code:

```java
OrePreviewTexture texture = OrePreviewTexture.register(image); // image ownership transfers
preview.setAppearance(new OrePlayerAppearance(texture.location(), OrePlayerAppearance.Model.CLASSIC));
// When every consumer is finished (owning Screen/controller cleanup, render thread):
preview.useEntityRendering(); // or replace/discard every appearance that uses this texture
texture.close();
```

### Model, pose and visual-layer extensions

```java
preview.setStandalonePoseModifier((model, pose) -> {
    model.model().head.zRot = (float) Math.sin(pose.seconds()) * 0.1F;
});
Consumer<OrePlayerRenderContext> overlay = frame -> frame.renderSkinLayer(overlayTexture, 0xFFFFFFFF);
preview.addStandaloneLayer(overlay);
// Later, if the layer/controller is no longer needed:
preview.removeStandaloneLayer(overlay);
```

`OrePlayerPose` supplies UI seconds, walking/crouching, mouse-look angles, swing arm and progress. Base parts reset to their baked poses and visibility each frame, then receive the entity-free pose and appearance overlay visibility. The pose modifier runs next; hat/jacket/sleeve/trouser transforms are synchronized to their base parts afterward. Use a modifier to set custom part poses/visibility each frame rather than relying on previous-frame mutations.

`setStandaloneModelFactory(OrePlayerModel.Factory)` accepts `(EntityModelSet, appearanceModel) -> OrePlayerModel(root, playerModel)`. Use registered native model-layer definitions for additional baked parts, or a custom `PlayerModel<LivingEntity>` subclass. The type parameter does **not** imply an entity is constructed: the standalone renderer never calls `setupAnim` or `prepareMobModel`. A factory change clears the cache immediately; reload clears it lazily. All parts under the supplied root, including custom children, reset every frame.

Visual layers run after the base skin, in registration order. `OrePlayerRenderContext` exposes the native model/root, graphics/pose/buffers, appearance, pose inputs and full-bright packed light. `renderSkinLayer(texture,tint)` draws a skin-space overlay using the posed player model; arbitrary custom parts can call their native `ModelPart.render` with the context's pose/buffer/light. Extra root children are **not** automatically drawn by the vanilla player model: render them in a visual layer or include them in a model subclass's part traversal. Layers get individual pose-stack push/pop scopes; normal widget clipping and render-state restoration still apply. Contexts are frame-local and must not be retained. Hooks must balance their own nested pose/scissor operations, avoid gameplay calls, and manage their own assets. No vanilla entity `RenderLayer`, Curios renderer or living-entity render event is invoked on the standalone path.

### Opt-in standalone demo

Call `OrePlayerPreviewDemo.open()` from your own client-side development code to open classic Steve and slim Alex models using Minecraft's existing textures, with walking/head tracking, animation pause and a swing button. The library does not add a main-menu button. The preview area resizes with the GUI. No world/player needs to have been loaded, and the example contains no appearance editor, asset generation, persistence or gameplay logic.

## Transforms and item motion

```java
OreItemPreview item = new OreItemPreview(Component.literal("Grass block"),
        new ItemStack(Items.GRASS_BLOCK), 110);
item.setWidth(160);
item.setPosition(20, 60);
item.setRotation(20, 35, 0);       // pitch, yaw, roll in degrees
item.setModelScale(1.2F);         // multiplier on fitted model size
item.setModelOffset(12, -4, 0);   // screen-space GUI pixels, including depth
item.setFloating(true);
item.setBobAmplitude(3);         // GUI pixels
item.setBobSpeed(0.5F);          // cycles per second
item.setRotationSpeed(30);       // degrees per second; negative reverses spin
addRenderableWidget(item);
```

`setPosition` moves the entire widget. `setModelOffset` moves the model inside its fixed, clipped preview area. Rotation/scale/offset setters are available on all three widgets through `OreModelPreview`. Inputs must be finite, and scale must be positive. `setBackground(false)` hides the inset shell. `setAnimationRunning(false)` freezes the exact interpolated pose, not rendering or code-controlled transforms; resuming continues from that pose without a time jump.

## Vanilla player animations

This section describes the original **entity-based** path. Armor/held-item/Curios rendering, item-use timing, vanilla item-specific poses, capes and third-party entity-renderer layers/events require that path and a loaded client world/player. Standalone layers are separate extension hooks, not a substitute for those entity APIs.

```java
OrePlayerPreview player = new OrePlayerPreview(Component.literal("Player"), 160);
player.setMainHand(new ItemStack(Items.DIAMOND_SWORD));
player.setOffHand(new ItemStack(Items.SHIELD));
player.playSwing(InteractionHand.MAIN_HAND);
player.setSwingLoop(true);
player.setWalking(true);         // walk in place
player.setCrouching(true);
player.setFollowMouse(true);     // head tracking while the preview is hovered
addRenderableWidget(player);
```

`setEquipment` also accepts player armor slots. Equipment is copied into the preview's private inventory. `playSwing` restarts vanilla's swing timing in either hand. `setSwingLoop(true)` repeats the last selected hand's swing at a twelve-tick interval, except while an item-use animation is active.

To preview bow drawing, crossbow charging or food-use timing, equip the appropriate item and call `playUseItem(InteractionHand.MAIN_HAND)`. To raise an equipped shield, use `OFF_HAND`. `stopUsingItem()` cancels the use pose; `setUseItemLoop(true)` restarts finite-duration uses when their timer finishes. Long-duration uses, such as holding a drawn bow or raised shield, remain active until stopped. Only native **third-person** poses are rendered: first-person-only hand/camera animations are not reproduced.

The private preview player advances only render animation fields: vanilla swing timing, walk animation, pose and item-use countdown. It does not execute normal entity/AI/world ticks, item-use completion/release callbacks, swing hooks, consumption, projectile logic, equipment effects, interaction events or network actions. Its private team hides name tags without touching the world's scoreboard. The normal player renderer and its render layers still run, including other mods' rendering hooks.

## Copying equipped gear and Curios

```java
boolean copied = player.copyEquipmentFromCurrentPlayer();
// Or copy a specific player's equipment, while keeping the current local player's preview skin:
player.copyEquipmentFrom(sourcePlayer);
```

Copying is explicit, not automatic on construction or every tick. It replaces both hands and all four armor slots (including empty slots), captures the source's main arm, and copies optional Curios equipment. Each stored stack and each installed preview stack is a separate `ItemStack.copy()`. Refreshing stops the current use/swing pose; animation-loop settings are retained. A missing client player makes the convenience method return `false` without changing the previous snapshot.

This is a snapshot, not a live inventory binding. Call either method again after gear changes. `setMainHand`, `setOffHand` and `setEquipment` can still override individual vanilla slots without discarding armor or captured accessories. The snapshot is reapplied with fresh copies when the private entity is recreated for another client level. Copying equipment does not copy arbitrary player attachments, attributes, effects or mod-specific animation state.

When **Curios 9.5.1–9.x for NeoForge 1.21.1** is installed, the compatibility bridge captures regular/cosmetic items, slot identifiers and final sizes (including extra slots), render toggles, active flags and slot visibility/cosmetic settings. Minecraft's normal player renderer runs Curios' own layer, including its cosmetic precedence and visibility rules. `isCuriosSnapshotAvailable()` reports whether the source had a Curios inventory. `getCopiedCuriosCount()` and `getCopiedCosmeticCuriosCount()` count occupied captured slots, including hidden ones, not visible models or item quantities.

The optional bridge is loaded only after a Curios presence check. It creates fresh Curios slot handlers at the captured sizes and assigns copied stacks/flags directly to the private preview. It deliberately avoids inventory NBT restoration, slot-modifier reconstruction, insertion/extraction, validation, equip/unequip hooks and accessory ticks. Curios 9.5's implementation handler is used because its API has no slot-handler factory; support is bounded to the 9.x line. No Curios classes or assets are bundled. Curios is a compile-only dependency by default; for a development client with it enabled, use `gradlew.bat runClient -PwithCurios`. Normal launches without Curios remain supported.

Standard Curios-registered accessory renderers can use the copied slots. Items whose visuals depend on other mod-specific attachments, attribute effects or a renderer that only accepts the real local player may need additional compatibility. Third-party render hooks still run and are not sandboxed; the library itself neither invokes gameplay callbacks nor shares the real equipment inventory. Runtime rendering with actual accessories still needs in-game verification.

## Ticking and lifecycle

Previews use a monotonic UI animation clock, not Minecraft's world partial tick. The world partial tick can freeze in a pausing menu; the UI clock continues to advance each rendered frame. Floating/rotation and player-pose interpolation therefore follow the client frame rate, while player animation state still updates in fixed 50 ms steps. This does not increase world/server tick rate or require a performance mod.

`OreScreen` services registered `OreModelPreview` clocks automatically, including widgets added through `addContentWidget`. Hidden gallery pages pause their previews. In a plain Minecraft `Screen`, call `preview.tick()` once from the owning screen's `tick()`, including while the widget is hidden so its clock can suspend. Rendering also services the same clock; tick/render calls share elapsed time and never advance a pose twice for the same time sample. Do not call entity world ticks to animate a preview.

Long gaps over 250 ms are discarded rather than fast-forwarding through missed animations. Short catch-up is bounded to five pose steps. Pausing and hiding preserve the interpolation fraction; resuming does not include time spent inactive. These timing rules apply only to the isolated preview, not the real player or game simulation.

Entity-based player previews rebuild their private entity when the client level changes and release it when no world/player is available. Standalone previews do not reference a world/player and rebuild baked models after resource reloads instead. The widgets and texture handles are client/render-thread objects, not thread-safe background render services. Model draws are clipped against both widget bounds and any enclosing gallery/popup scissor. Buffered rendering is flushed and the pose, scissor, shader color and depth-test state are restored; lighting returns to Minecraft's standard GUI item lighting.

## Gallery

The **3D previews** sidebar page contains item selection with floating/rotation/movement controls; block-state selection and zoom; and a player initialized from current equipment, with optional accessory counts and a **Refresh equipment from my player** button. The loadout selector offers current equipment or sword/bow/apple/crossbow examples; examples replace only preview hands and add a shield, retaining copied armor and accessories. Swing, off-hand swing, item use, shield, walking, crouching, mouse-follow, animation pause and transform reset controls remain available. Selecting a preview item never changes the player's inventory or spawns anything in the world.
