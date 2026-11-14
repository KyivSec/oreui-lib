# Custom UI fonts

Fonts are opt-in and client-only. No font files are bundled. Existing constructors and the entire test gallery use Minecraft's default font; the library does not replace `minecraft:default`, chat, HUD or other mods' text. Supply custom font resources through your application or a resource pack.

## Modal and popup fonts

```java
ResourceLocation bodyFont = ResourceLocation.fromNamespaceAndPath("example", "body");
minecraft.pushGuiLayer(new OreModalScreen(title, bodyFont, modal -> {
    modal.add(new OreLabel(modal.font(), body, 176));
    modal.add(new OreTextField(modal.font(), 0, 0, 176, label, description));
    modal.addConfirmation(confirmation, false, checked -> {});
    modal.addAction(Component.translatable("gui.done"), OrePalette.COLORED, this::apply);
}));
```

`OreModalScreen` and `OrePopupScreen` accept `(Component title, ResourceLocation fontId, Consumer buildContent)`. Use `modal.font()` / `popup.font()` when constructing labels, text fields or other controls that take a `Font`. The title, added buttons, actions and confirmation row inherit the selected font. Set an added button's font after `add` if it should override that choice. Existing two-argument constructors remain unchanged.

## Reusable font views

```java
Font custom = OreFonts.font(minecraft.font, bodyFont);
OreLabel label = new OreLabel(custom, Component.literal("Custom typography"), 180);
OreTextField field = new OreTextField(custom, 20, 50, 180, caption, description);
OreButton button = new OreButton(20, 100, 180, 28, actionLabel, OrePalette.GENERAL, clicked -> apply());
button.setFont(custom);
```

`OreFonts.font(base, fontId)` returns a native `Font`-compatible view. Drawing, width measurement, wrapping, clipping, cursor placement and selection use the same font metrics. It delegates glyph ownership and resource reloads to the base font; it does not create a second font manager or renderer. Use it on the client/render thread. Reuse the view rather than constructing one each frame.

The font view also accepts your own namespaced font resource IDs. Explicit non-default font IDs on text components remain respected. Selecting `Style.DEFAULT_FONT` returns the supplied base renderer unchanged.

`OreButton.setFont(Font)` also applies to `OreToggle`; passing `null` restores the default renderer. Controls with a `Font` constructor use the supplied renderer for their text. Other widgets can use native `Component` font styles, for example `component.copy().withStyle(style -> style.withFont(bodyFont))`.

Bold, italic, colors and other component styles retain Minecraft's normal behavior. No font-family-specific face mapping is applied. To use a distinct bold/italic font file, define its own resource ID and select it explicitly without also requesting synthetic bold/italic.

## Adding a font

Supply a resource-pack definition such as `assets/example/font/body.json` and its font file:

```json
{
  "providers": [
    { "type": "ttf", "file": "example:body.ttf", "size": 11, "oversample": 2, "shift": [0, 0] },
    { "type": "reference", "id": "minecraft:default" }
  ]
}
```

Store the TTF at `assets/example/font/body.ttf`. Select `ResourceLocation.fromNamespaceAndPath("example", "body")` in the font API. Keep all redistribution notices required by that font's license.

Actual glyph coverage depends on the supplied font and installed resource packs. The native nine-pixel line grid and existing widget text scales remain unchanged; check long/accented text at your target GUI scales.
