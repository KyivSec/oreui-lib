# Native 3D previews

The client-only preview widgets use Minecraft 1.21.1's existing item, block and player renderers. There are no required additional rendering dependencies, framebuffers, spawned entities or copied model/skin assets. Curios equipment support is optional.

## Widgets

- `OreItemPreview(Component label, ItemStack item, int height)` renders an item or block's item model. It copies its input stack. The default display context is `GROUND`; use `setDisplayContext(ItemDisplayContext.GUI)` for the model's inventory presentation. Native model geometry and custom item renderers are retained; a flat sprite does not become a full 3D mesh. Fitting uses the model's declared display-transform scale; unusual custom models can be adjusted with `setModelScale`.
- `OreBlockPreview(Component label, BlockState state, int height)` renders a specific block state rather than its inventory representation, including properties such as stair/slab orientation or a furnace's lit state. It uses the single-block renderer with full-bright lighting and empty model data. This is not a miniature world: world-dependent neighbor/model data, fluids and arbitrary block-entity NBT/animations are not simulated.
- `OrePlayerPreview(Component label, int height)` renders a separate, unregistered `RemotePlayer` using the current player's profile/skin. It requires a loaded client world/player; otherwise it shows a placeholder. It never uses the actual player as the animation target.

All previews have native widget position/size, labels/narration and optional inset backgrounds. They do not take keyboard focus or consume clicks. They can have a normal native tooltip assigned with `setTooltip`.

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

Player previews rebuild their private entity when the client level changes and release it when no world/player is available. The widgets are client/render-thread objects, not thread-safe background render services. Model draws are clipped against both widget bounds and any enclosing gallery/popup scissor. Buffered rendering is flushed and the pose, scissor, shader color and depth-test state are restored; lighting returns to Minecraft's standard GUI item lighting.

## Gallery

The **3D previews** sidebar page contains item selection with floating/rotation/movement controls; block-state selection and zoom; and a player initialized from current equipment, with optional accessory counts and a **Refresh equipment from my player** button. The loadout selector offers current equipment or sword/bow/apple/crossbow examples; examples replace only preview hands and add a shield, retaining copied armor and accessories. Swing, off-hand swing, item use, shield, walking, crouching, mouse-follow, animation pause and transform reset controls remain available. Selecting a preview item never changes the player's inventory or spawns anything in the world.
