package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreIcons;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OreNativeIcons;
import com.kyivsec.oreui.client.style.OreTooltip;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import com.kyivsec.oreui.client.widget.OreAccordion;
import com.kyivsec.oreui.client.widget.OreImageCard;
import com.kyivsec.oreui.client.widget.OreNavBar;
import com.kyivsec.oreui.client.widget.OreScrollbar;
import com.kyivsec.oreui.client.widget.OreSteppedSlider;
import com.kyivsec.oreui.client.widget.OreTable;
import com.kyivsec.oreui.client.widget.OreTextField;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreCard;
import com.kyivsec.oreui.client.widget.OreColorPicker;
import com.kyivsec.oreui.client.widget.OreColorEditor;
import com.kyivsec.oreui.client.widget.OreColumn;
import com.kyivsec.oreui.client.widget.OreDropdown;
import com.kyivsec.oreui.client.widget.OreImage;
import com.kyivsec.oreui.client.widget.OreItemPreview;
import com.kyivsec.oreui.client.widget.OreBlockPreview;
import com.kyivsec.oreui.client.widget.OrePlayerPreview;
import com.kyivsec.oreui.client.widget.OreModelPreview;
import com.kyivsec.oreui.client.widget.OreKeybindButton;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OreNavigationBar;
import com.kyivsec.oreui.client.widget.OreNumberField;
import com.kyivsec.oreui.client.widget.OrePagination;
import com.kyivsec.oreui.client.widget.OreProgressIndicator;
import com.kyivsec.oreui.client.widget.OreSection;
import com.kyivsec.oreui.client.widget.OreSlider;
import com.kyivsec.oreui.client.widget.OreToast;
import com.kyivsec.oreui.client.widget.OreToggle;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class OreUiTestScreen extends OreScreen {
    private enum Page {
        BUTTONS, SELECTION, TEXT, COLORS, FEEDBACK, CARDS, STRUCTURES, MEDIA, MODELS, NAVIGATION;

        private Component label() {
            return message("page." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static final class GalleryPage {
        private final OreColumn column = new OreColumn();
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final List<OreSection> sections = new ArrayList<>();
        private OreSection currentSection;
        private int scroll;
    }

    private static final ResourceLocation ADD_ICON = OreIcons.ADD.texture();
    private static final ResourceLocation SEARCH_ICON = OreIcons.SEARCH.texture();
    private static final Component SUBTITLE = message("subtitle");
    private final Map<Page, GalleryPage> pages = new EnumMap<>(Page.class);
    private final List<OreToggle> radios = new ArrayList<>();
    private final List<OreSlider> progressSliders = new ArrayList<>();
    private Page page = Page.BUTTONS;
    private TestScreenLayout layout;
    private OreNavigationBar navigation;
    private OreNavBar topBar;
    private Component status = message("ready");
    private int clicks;
    private int pagination = 1;
    private double progress = 0.5;
    private int modelOffsetStep;

    public OreUiTestScreen() {
        super(message("title"));
    }

    private static Component message(String key, Object... arguments) {
        return Component.translatable("oreui.test." + key, arguments);
    }

    @Override
    protected void init() {
        for (Page value : Page.values()) {
            pages.put(value, new GalleryPage());
        }
        navigation = new OreNavigationBar(Arrays.stream(Page.values()).map(Page::label).toList(),
                LinearLayout.Orientation.VERTICAL, index -> selectPage(Page.values()[index]));
        navigation.setHeader(font, message("navigation.header"));
        navigation.setGroupHeading(Page.FEEDBACK.ordinal(), font, message("navigation.other"));
        OreIcons[] pageIcons = {OreIcons.ADD, OreIcons.ADJUST_HORIZONTAL3, OreIcons.LETTER, OreIcons.SPARKLES3,
                OreIcons.INFO_CIRCLE, OreIcons.LAYOUT_HORIZONTAL2, OreIcons.BULLETPOINTED_LIST3,
                OreIcons.LANDSCAPE, OreIcons.PERSON, OreIcons.BULLETPOINTED_LIST3};
        for (int i = 0; i < pageIcons.length; i++) navigation.setIcon(i, pageIcons[i].texture());
        addRenderableOnly(navigation);
        navigation.visitWidgets(this::addRenderableWidget);
        topBar = new OreNavBar(title, message("navbar.pages", Page.values().length), this::onClose, () -> selectPage(Page.NAVIGATION));
        addRenderableOnly(topBar);
        topBar.visitWidgets(this::addRenderableWidget);
        buildButtons();
        buildSelection();
        buildText();
        buildColors();
        buildFeedback();
        buildCards();
        buildStructures();
        buildMedia();
        buildModels();
        buildNavigation();
        showPage();
        repositionElements();
    }

    private <T extends LayoutElement> T add(Page owner, T element) {
        GalleryPage target = pages.get(owner);
        if (target.currentSection == null) {
            target.column.addChild(element);
        } else {
            target.currentSection.add(element);
        }
        element.visitWidgets(widget -> {
            target.widgets.add(widget);
            addContentWidget(widget);
        });
        return element;
    }

    private OreLabel label(Page owner, Component text) {
        return add(owner, new OreLabel(font, text, 200));
    }

    private void section(Page owner, String title) {
        GalleryPage target = pages.get(owner);
        OreSection section = target.column.addChild(new OreSection(font, message("section." + title)));
        target.sections.add(section);
        target.currentSection = section;
        section.visitWidgets(widget -> {
            target.widgets.add(widget);
            addContentWidget(widget);
        });
    }

    private void heading(Page owner, String description) {
        label(owner, owner.label());
        label(owner, message(description));
    }

    private OreButton button(Page owner, String key, OrePalette palette, Runnable action) {
        return add(owner, new OreButton(0, 0, 100, 28, message(key), palette, ignored -> action.run()));
    }

    private void buildButtons() {
        heading(Page.BUTTONS, "buttons.description");
        section(Page.BUTTONS, "activation");
        OreButton counter = add(Page.BUTTONS, new OreButton(0, 0, 100, 28, message("clicks", clicks), OrePalette.COLORED,
                button -> {
                    button.setMessage(message("clicks", ++clicks));
                    status = message("activated", clicks);
                }));
        counter.setTooltip(Tooltip.create(message("button.tooltip")));
        section(Page.BUTTONS, "palettes");
        for (OrePalette palette : OrePalette.values()) {
            Component name = message("palette." + palette.name().toLowerCase(Locale.ROOT));
            add(Page.BUTTONS, new OreButton(0, 0, 100, 28, name, palette, button -> status = message("palette.selected", name)));
        }
        section(Page.BUTTONS, "content");
        OreButton icon = button(Page.BUTTONS, "icon.button", OrePalette.COLORED, () -> status = message("icon.clicked"));
        icon.setIcon(ADD_ICON, true);
        icon.setTooltip(Tooltip.create(message("icon.button")));
        OreButton textIcon = button(Page.BUTTONS, "text.icon", OrePalette.REALMS, () -> status = message("icon.clicked"));
        textIcon.setIcon(ADD_ICON, false);
        OreButton vertical = button(Page.BUTTONS, "vertical.button", OrePalette.COLORED, () -> status = message("icon.clicked"));
        vertical.setIcon(SEARCH_ICON, false);
        vertical.setVerticalContent(true);
        vertical.setHeight(52);
        section(Page.BUTTONS, "states");
        OreButton seamless = button(Page.BUTTONS, "seamless", OrePalette.GENERAL, () -> status = message("seamless.clicked"));
        seamless.setAppearance(OreButton.Appearance.SEAMLESS);
        OreButton selectable = add(Page.BUTTONS, new OreButton(0, 0, 100, 28, message("selectable"), OrePalette.COLORED,
                button -> {
                    OreButton selected = (OreButton) button;
                    selected.setSelected(!selected.isSelected());
                    status = message(selected.isSelected() ? "selected" : "unselected");
                }));
        selectable.setIcon(ADD_ICON, true);
        selectable.setAppearance(OreButton.Appearance.SELECTABLE_ICON);
        selectable.setHeight(26);
        selectable.setTooltip(Tooltip.create(message("selectable")));
        section(Page.BUTTONS, "disabled.buttons");
        List<OreButton> disabled = new ArrayList<>();
        for (OrePalette palette : OrePalette.values()) {
            Component name = message("palette." + palette.name().toLowerCase(Locale.ROOT));
            OreButton example = add(Page.BUTTONS, new OreButton(0, 0, 100, 28, message("disabled.palette", name),
                    palette, button -> status = message("disabled.activated")));
            example.active = false;
            disabled.add(example);
        }
        add(Page.BUTTONS, new OreToggle(message("disabled.preview"), OreToggle.Kind.SWITCH, false,
                value -> disabled.forEach(button -> button.active = value)));
    }

    private void buildSelection() {
        heading(Page.SELECTION, "selection.description");
        section(Page.SELECTION, "switches");
        for (OrePalette palette : OrePalette.values()) {
            Component name = message("palette." + palette.name().toLowerCase(Locale.ROOT));
            OreToggle example = add(Page.SELECTION, new OreToggle(name, OreToggle.Kind.SWITCH, true,
                    value -> status = message(value ? "selected" : "unselected")));
            example.setSwitchPalette(palette);
        }
        section(Page.SELECTION, "switch.symbols");
        for (OrePalette palette : OrePalette.values()) {
            Component name = message("palette." + palette.name().toLowerCase(Locale.ROOT));
            OreToggle example = add(Page.SELECTION, new OreToggle(name, OreToggle.Kind.SWITCH, false,
                    value -> status = message(value ? "selected" : "unselected")));
            example.setSwitchPalette(palette);
            example.setSwitchSymbols(true);
        }
        section(Page.SELECTION, "switch.disabled");
        for (boolean checked : List.of(true, false)) {
            OreToggle example = add(Page.SELECTION, new OreToggle(message("disabled"), OreToggle.Kind.SWITCH, checked, value -> {}));
            example.setSwitchSymbols(true);
            example.active = false;
        }
        section(Page.SELECTION, "choices");
        for (boolean selected : List.of(false, true)) {
            OreButton selection = add(Page.SELECTION, new OreButton(0, 0, 100, 28, message("selection.button"), OrePalette.GENERAL,
                    button -> {
                        OreButton choice = (OreButton) button;
                        choice.setSelected(!choice.isSelected());
                        status = message(choice.isSelected() ? "selected" : "unselected");
                    }));
            selection.setAppearance(OreButton.Appearance.SELECTION);
            selection.setSelected(selected);
            selection.setIcon(ADD_ICON, false);
            selection.setBadge(Component.literal("1"));
        }
        add(Page.SELECTION, new OreToggle(message("checkbox"), OreToggle.Kind.CHECKBOX, false,
                value -> status = message(value ? "selected" : "unselected")));
        for (boolean checked : List.of(false, true)) {
            OreToggle disabled = add(Page.SELECTION, new OreToggle(message("disabled"), OreToggle.Kind.CHECKBOX, checked, value -> {}));
            disabled.active = false;
        }
        for (OrePalette palette : List.of(OrePalette.COLORED, OrePalette.DESTRUCTIVE, OrePalette.WARNING, OrePalette.REALMS)) {
            Component name = message("palette." + palette.name().toLowerCase(Locale.ROOT));
            LinearLayout examples = new LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL).spacing(4);
            for (int state = 0; state < 3; state++) {
                OreToggle checkbox = examples.addChild(new OreToggle(name, OreToggle.Kind.CHECKBOX, state == 0,
                        value -> status = message(value ? "selected" : "unselected")));
                checkbox.setCheckboxPalette(palette);
                checkbox.setIndeterminate(state == 1);
                checkbox.setWidth(23);
                checkbox.setHeight(23);
                checkbox.setTooltip(Tooltip.create(name));
            }
            examples.arrangeElements();
            add(Page.SELECTION, examples);
        }
        for (int i = 0; i < 3; i++) {
            int index = i;
            radios.add(add(Page.SELECTION, new OreToggle(message("radio", i + 1), OreToggle.Kind.RADIO, i == 0, value -> {
                if (value) {
                    for (int other = 0; other < radios.size(); other++) {
                        if (other != index) {
                            radios.get(other).setChecked(false);
                        }
                    }
                    status = message("radio.selected", index + 1);
                }
            })));
        }
        section(Page.SELECTION, "values");
        OreSlider slider = add(Page.SELECTION, new OreSlider(0, 0, 100, 34, message("slider.label"), progress, this::updateProgress));
        progressSliders.add(slider);
        OreSteppedSlider incremented = add(Page.SELECTION, new OreSteppedSlider(message("slider.incremented"), 10, 0.3, false,
                value -> status = message("slider.step", Math.round(value * 10))));
        OreSteppedSlider segmented = add(Page.SELECTION, new OreSteppedSlider(message("slider.segmented"), 10, 0.3, true,
                value -> status = message("slider.step", Math.round(value * 10))));
        OreSteppedSlider disabledSteps = add(Page.SELECTION, new OreSteppedSlider(message("slider.segmented.disabled"), 10, 0.3, true, value -> {}));
        disabledSteps.active = false;
        OreDropdown dropdown = add(Page.SELECTION, new OreDropdown(message("dropdown"),
                List.of(message("choice.0"), message("choice.1"), message("choice.2")), 0,
                index -> status = message("dropdown.selected", message("choice." + index))));
        add(Page.SELECTION, new OreToggle(message("enabled"), OreToggle.Kind.SWITCH, true, enabled -> {
            slider.active = enabled;
            incremented.active = enabled;
            segmented.active = enabled;
            dropdown.active = enabled;
            status = message(enabled ? "enabled" : "disabled.controls");
        }));
    }

    private void buildText() {
        heading(Page.TEXT, "text.description");
        section(Page.TEXT, "editing");
        OreLabel preview = label(Page.TEXT, message("preview", ""));
        EditBox text = add(Page.TEXT, new OreTextField(font, 0, 0, 100, message("text.label"), message("text.description.field")));
        text.setMaxLength(128);
        text.setTextShadow(false);
        text.setHint(message("text.hint"));
        text.setResponder(value -> {
            preview.setMessage(message("preview", value));
            relayout();
        });
        OreTextField disabledText = add(Page.TEXT, new OreTextField(font, 0, 0, 100, message("text.disabled"), message("text.description.field")));
        disabledText.setValue(message("text.disabled").getString());
        disabledText.active = false;
        section(Page.TEXT, "search");
        EditBox search = add(Page.TEXT, new OreTextField(font, 0, 0, 100, message("search.label"), message("search.description.field")).search());
        search.setTextShadow(false);
        search.setMaxLength(64);
        search.setHint(message("search.hint"));
        List<String> options = List.of(message("choice.0").getString(), message("choice.1").getString(), message("choice.2").getString());
        OreLabel results = label(Page.TEXT, message("search.results", String.join(", ", options)));
        search.setResponder(value -> {
            String matches = String.join(", ", options.stream().filter(option -> option.toLowerCase(Locale.ROOT)
                    .contains(value.toLowerCase(Locale.ROOT))).toList());
            results.setMessage(message("search.results", matches.isEmpty() ? message("search.empty") : matches));
            relayout();
        });
        section(Page.TEXT, "binding");
        OreNumberField number = add(Page.TEXT, new OreNumberField(font, message("number.label"), 42,
                value -> status = message("number.committed", value)));
        number.setDescription(message("number.description.field"));
        add(Page.TEXT, new OreKeybindButton(message("keybind"), InputConstants.Type.KEYSYM.getOrCreate(75),
                key -> status = message("keybind.changed", key.getDisplayName())));
    }

    private void buildColors() {
        heading(Page.COLORS, "colors.description");
        section(Page.COLORS, "primary");
        add(Page.COLORS, new OreColorEditor(font, message("color.primary"), 0xFF387E24,
                color -> status = message("color.changed", OreColorPicker.format(color))));
        section(Page.COLORS, "opacity");
        add(Page.COLORS, new OreColorEditor(font, message("color.secondary"), 0x807345E5,
                color -> status = message("color.changed", OreColorPicker.format(color))));
        label(Page.COLORS, message("color.transparent")).decorate(OreLabel.Decoration.BANNER, 0x807345E5).colorSwatch();
        label(Page.COLORS, message("color.resource.note"));
    }

    private void buildFeedback() {
        heading(Page.FEEDBACK, "feedback.description");
        section(Page.FEEDBACK, "status");
        label(Page.FEEDBACK, message("label.example"));
        label(Page.FEEDBACK, message("indicator")).decorate(OreLabel.Decoration.INDICATOR, 0xFF9DDF77);
        label(Page.FEEDBACK, message("tag")).decorate(OreLabel.Decoration.TAG, 0xFF9DDF77);
        label(Page.FEEDBACK, Component.literal("1")).decorate(OreLabel.Decoration.BADGE, 0xFFAD99E7);
        label(Page.FEEDBACK, message("label.example")).decorate(OreLabel.Decoration.DIVIDER, 0xFFD2D4D6);
        section(Page.FEEDBACK, "progress");
        progressSliders.add(add(Page.FEEDBACK, new OreSlider(0, 0, 100, 34, message("slider.label"), progress, this::updateProgress)));
        add(Page.FEEDBACK, new OreProgressIndicator(message("progress"), OreProgressIndicator.Kind.BAR, () -> progress));
        add(Page.FEEDBACK, new OreProgressIndicator(message("progress.outlined"), OreProgressIndicator.Kind.OUTLINED_BAR, () -> progress));
        OreProgressIndicator pixelSpinner = add(Page.FEEDBACK, new OreProgressIndicator(message("spinner.pixel"),
                OreProgressIndicator.Kind.PIXEL_SPINNER, () -> progress));
        add(Page.FEEDBACK, new OreProgressIndicator(message("progress.ring"), OreProgressIndicator.Kind.PIXEL_RING, () -> progress));
        OreProgressIndicator spinner = add(Page.FEEDBACK, new OreProgressIndicator(message("spinner"),
                OreProgressIndicator.Kind.SPINNER, () -> 0));
        add(Page.FEEDBACK, new OreToggle(message("motion"), OreToggle.Kind.SWITCH, true, running -> {
            spinner.setRunning(running);
            pixelSpinner.setRunning(running);
        }));
        section(Page.FEEDBACK, "notifications");
        button(Page.FEEDBACK, "toast", OrePalette.COLORED,
                () -> minecraft.getToasts().addToast(new OreToast(message("toast.title"), message("toast.body"))));
        OreButton tooltip = button(Page.FEEDBACK, "tooltip", OrePalette.GENERAL, () -> status = message("tooltip.clicked"));
        tooltip.setTooltip(Tooltip.create(message("tooltip.body")));
        for (OreTooltip.Side side : OreTooltip.Side.values()) {
            OreLabel example = add(Page.FEEDBACK, new OreLabel(font, message("tooltip.side", side.name()), 200) {
                @Override
                public void setWidth(int width) {
                    super.setWidth(width);
                    setHeight(30);
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    OreTooltip.render(graphics, font, List.of(getMessage().getVisualOrderText()), getX() + 8, getY() + 8,
                            Math.max(12, getWidth() - 16), side);
                }
            });
            example.setHeight(30);
        }
    }

    private void buildCards() {
        heading(Page.CARDS, "cards.description");
        section(Page.CARDS, "banners");
        label(Page.CARDS, message("banner.info")).decorate(OreLabel.Decoration.BANNER, 0xFF2E6BE5);
        label(Page.CARDS, message("banner.warning")).decorate(OreLabel.Decoration.BANNER, 0xFFE1B922);
        section(Page.CARDS, "cards");
        add(Page.CARDS, new OreCard(font, message("card.title"), message("card.body"), 0xFF50913D,
                button -> status = message("card.clicked")));
        add(Page.CARDS, new OreCard(font, message("card.second.title"), message("card.second.body"), 0xFF7345E5,
                button -> openPopup()));
        section(Page.CARDS, "image.cards");
        ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/block/stone.png");
        OreImageCard card = add(Page.CARDS, new OreImageCard(font, message("card.image.title"), message("card.image.description"),
                texture, 16, 16, () -> status = message("card.clicked"), null));
        card.setCount(Component.literal("1/8"));
        card.setTopTag(message("card.tag.info"));
        card.setBottomInfoTag(message("card.tag.info"));
        card.setBottomTag(message("card.tag.notice"));
        OreImageCard actionCard = add(Page.CARDS, new OreImageCard(font, message("card.image.title"), message("card.image.description"),
                texture, 16, 16, this::openModal, () -> status = message("card.action.clicked")));
        actionCard.setTopTag(message("card.tag.info"));
        actionCard.setBottomInfoTag(message("card.tag.info"));
        actionCard.setBottomTag(message("card.tag.notice"));
        label(Page.CARDS, message("card.image.note"));
    }

    private void buildStructures() {
        heading(Page.STRUCTURES, "structures.description");
        section(Page.STRUCTURES, "accordions");
        add(Page.STRUCTURES, new OreAccordion(font, message("accordion.closed"), message("accordion.body"), false, expanded -> relayout()));
        add(Page.STRUCTURES, new OreAccordion(font, message("accordion.open"), message("accordion.body"), true, expanded -> relayout()));
        section(Page.STRUCTURES, "tables");
        OreTable table = add(Page.STRUCTURES, new OreTable(message("table.title"),
                List.of(message("table.name"), message("table.count"), message("table.state")), List.of(
                new OreTable.Row(Component.literal("A"), List.of(Component.literal("Stone"), Component.literal("64"), Component.literal("Ready"))),
                new OreTable.Row(Component.literal("B"), List.of(Component.literal("Apple"), Component.literal("12"), Component.literal("Ready"))),
                new OreTable.Row(Component.literal("C"), List.of(Component.literal("Sword"), Component.literal("1"), Component.literal("Used"))))));
        table.setComparator(1, java.util.Comparator.comparingInt(component -> Integer.parseInt(component.getString())));
        label(Page.STRUCTURES, message("table.note"));
        section(Page.STRUCTURES, "scrollbars");
        OreLabel scrollValue = label(Page.STRUCTURES, message("scrollbar.value", 0));
        OreScrollbar horizontal = add(Page.STRUCTURES, new OreScrollbar(message("scrollbar.horizontal"), OreScrollbar.Orientation.HORIZONTAL,
                value -> scrollValue.setMessage(message("scrollbar.value", value))));
        horizontal.setMetrics(500, 100, 0);
        label(Page.STRUCTURES, message("scrollbar.note"));
    }

    private void buildMedia() {
        heading(Page.MEDIA, "media.description");
        section(Page.MEDIA, "icons");
        add(Page.MEDIA, new OreImage(message("icon.add"), ADD_ICON, 16, 16, 48));
        label(Page.MEDIA, message("icon.add"));
        add(Page.MEDIA, new OreImage(message("icon.search"), SEARCH_ICON, 16, 16, 32));
        label(Page.MEDIA, message("icon.search"));
        OreNativeIcons first = OreNativeIcons.CHECKMARK;
        OreImage nativePreview = add(Page.MEDIA, new OreImage(message("icon.native"), first.texture(), 15, 15, 20));
        OreImage preview = add(Page.MEDIA, new OreImage(message("icon.enlarged"), first.texture(), 15, 15, 48));
        OreLabel dimensions = label(Page.MEDIA, nativeIconDescription(first));
        OreButton iconButton = button(Page.MEDIA, "icon.sample", OrePalette.GENERAL, () -> status = message("icon.clicked"));
        iconButton.setIcon(first.texture(), false);
        add(Page.MEDIA, new OreDropdown(message("icon.browser"), Arrays.stream(OreNativeIcons.values()).map(OreNativeIcons::label).toList(), 0,
                index -> {
                    OreNativeIcons selected = OreNativeIcons.values()[index];
                    nativePreview.setTexture(selected.texture());
                    preview.setTexture(selected.texture());
                    iconButton.setIcon(selected.texture(), false);
                    dimensions.setMessage(nativeIconDescription(selected));
                    relayout();
                }));
        section(Page.MEDIA, "textures");
        add(Page.MEDIA, new OreProgressIndicator(message("spinner.pixel"), OreProgressIndicator.Kind.PIXEL_SPINNER, () -> 0));
        add(Page.MEDIA, new OreProgressIndicator(message("progress.ring"), OreProgressIndicator.Kind.PIXEL_RING, () -> progress));
        label(Page.MEDIA, message("icons.pixel.note"));
        add(Page.MEDIA, new OreImage(message("texture"), ResourceLocation.withDefaultNamespace("textures/block/stone.png"), 16, 16, 96));
        label(Page.MEDIA, message("texture.note"));
    }

    private Component nativeIconDescription(OreNativeIcons icon) {
        return message("icon.dimensions", icon.label(), icon.canvasSize(), icon.canvasSize(), icon.ink().width(), icon.ink().height());
    }

    private void buildModels() {
        heading(Page.MODELS, "models.description");
        section(Page.MODELS, "models.items");
        ItemStack[] items = {new ItemStack(Items.GRASS_BLOCK), new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.APPLE), new ItemStack(Items.CHEST)};
        OreItemPreview item = add(Page.MODELS, new OreItemPreview(message("models.item"), items[0], 100));
        item.setFloating(true);
        add(Page.MODELS, new OreDropdown(message("models.item"),
                List.of(message("models.item.0"), message("models.item.1"), message("models.item.2"), message("models.item.3")),
                0, index -> item.setItem(items[index])));
        add(Page.MODELS, new OreToggle(message("models.floating"), OreToggle.Kind.SWITCH, true, item::setFloating));
        OreSlider itemYaw = add(Page.MODELS, new OreSlider(0, 0, 100, 34, message("models.rotation"), 35.0 / 360,
                value -> item.setRotation(item.getPitch(), (float) value * 360, item.getRoll())));
        button(Page.MODELS, "models.move", OrePalette.GENERAL, () -> {
            modelOffsetStep = (modelOffsetStep + 1) % 3;
            item.setModelOffset(modelOffsetStep == 1 ? 24 : modelOffsetStep == 2 ? -24 : 0, 0, 0);
        });
        label(Page.MODELS, message("models.item.note"));

        section(Page.MODELS, "models.blocks");
        BlockState[] blocks = {Blocks.OAK_STAIRS.defaultBlockState(),
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true), Blocks.GLASS.defaultBlockState()};
        OreBlockPreview block = add(Page.MODELS, new OreBlockPreview(message("models.block"), blocks[0], 100));
        add(Page.MODELS, new OreDropdown(message("models.block"),
                List.of(message("models.block.0"), message("models.block.1"), message("models.block.2"), message("models.block.3")),
                0, index -> block.setBlockState(blocks[index])));
        OreSlider blockZoom = add(Page.MODELS, new OreSlider(0, 0, 100, 34, message("models.zoom"), 1.0 / 3,
                value -> block.setModelScale(0.5F + (float) value * 1.5F)));

        section(Page.MODELS, "models.player");
        OrePlayerPreview player = add(Page.MODELS, new OrePlayerPreview(message("models.player"), 160));
        ItemStack[] held = {new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.BOW),
                new ItemStack(Items.APPLE), new ItemStack(Items.CROSSBOW)};
        player.copyEquipmentFromCurrentPlayer();
        player.setSwingLoop(true);
        player.setUseItemLoop(true);
        player.setFollowMouse(true);
        OreLabel equipmentStatus = label(Page.MODELS, equipmentSnapshotStatus(player));
        Runnable copyEquipment = () -> {
            player.copyEquipmentFromCurrentPlayer();
            equipmentStatus.setMessage(equipmentSnapshotStatus(player));
            relayout();
        };
        OreDropdown loadout = add(Page.MODELS, new OreDropdown(message("models.equipment"),
                List.of(message("models.held.current"), message("models.held.0"), message("models.held.1"),
                        message("models.held.2"), message("models.held.3")), 0, index -> {
                    if (index == 0) copyEquipment.run();
                    else {
                        player.setMainHand(held[index - 1]);
                        player.setOffHand(new ItemStack(Items.SHIELD));
                    }
                }));
        button(Page.MODELS, "models.copy.equipment", OrePalette.GENERAL, () -> {
            if (loadout.getSelectedIndex() == 0) copyEquipment.run();
            else loadout.select(0);
        });
        button(Page.MODELS, "models.swing", OrePalette.COLORED, () -> player.playSwing(InteractionHand.MAIN_HAND));
        button(Page.MODELS, "models.swing.offhand", OrePalette.GENERAL, () -> player.playSwing(InteractionHand.OFF_HAND));
        button(Page.MODELS, "models.use", OrePalette.GENERAL, () -> player.playUseItem(InteractionHand.MAIN_HAND));
        button(Page.MODELS, "models.shield", OrePalette.GENERAL, () -> player.playUseItem(InteractionHand.OFF_HAND));
        button(Page.MODELS, "models.stop.use", OrePalette.GENERAL, player::stopUsingItem);
        add(Page.MODELS, new OreToggle(message("models.loop.swing"), OreToggle.Kind.SWITCH, true, player::setSwingLoop));
        add(Page.MODELS, new OreToggle(message("models.loop.use"), OreToggle.Kind.SWITCH, true, player::setUseItemLoop));
        add(Page.MODELS, new OreToggle(message("models.walk"), OreToggle.Kind.SWITCH, false, player::setWalking));
        add(Page.MODELS, new OreToggle(message("models.crouch"), OreToggle.Kind.SWITCH, false, player::setCrouching));
        add(Page.MODELS, new OreToggle(message("models.look"), OreToggle.Kind.SWITCH, true, player::setFollowMouse));
        OreSlider playerYaw = add(Page.MODELS, new OreSlider(0, 0, 100, 34, message("models.rotation"), 155.0 / 360,
                value -> player.setRotation(player.getPitch(), (float) value * 360 - 180, player.getRoll())));
        label(Page.MODELS, message("models.player.note"));

        section(Page.MODELS, "models.controls");
        List<OreModelPreview> previews = List.of(item, block, player);
        add(Page.MODELS, new OreToggle(message("models.animate"), OreToggle.Kind.SWITCH, true,
                running -> previews.forEach(preview -> preview.setAnimationRunning(running))));
        button(Page.MODELS, "models.reset", OrePalette.GENERAL, () -> {
            previews.forEach(preview -> {
                preview.setModelOffset(0, 0, 0);
                preview.setModelScale(1);
            });
            item.setRotation(20, 35, 0);
            block.setRotation(25, -35, 0);
            player.setRotation(-10, -25, 0);
            itemYaw.setProgress(35.0 / 360);
            blockZoom.setProgress(1.0 / 3);
            playerYaw.setProgress(155.0 / 360);
            modelOffsetStep = 0;
        });
    }

    private Component equipmentSnapshotStatus(OrePlayerPreview player) {
        if (minecraft.player == null) return message("models.gear.unavailable");
        return player.isCuriosSnapshotAvailable()
                ? message("models.gear.curios", player.getCopiedCuriosCount(), player.getCopiedCosmeticCuriosCount())
                : message("models.gear.vanilla");
    }

    private void buildNavigation() {
        heading(Page.NAVIGATION, "navigation.description");
        section(Page.NAVIGATION, "tabs");
        OreLabel tabContent = new OreLabel(font, message("tab.content", 1), 200);
        add(Page.NAVIGATION, new OreNavigationBar(List.of(message("tab.first"), message("tab.second"), message("tab.third")),
                LinearLayout.Orientation.HORIZONTAL, index -> {
                    tabContent.setMessage(message("tab.content", index + 1));
                    relayout();
                }));
        add(Page.NAVIGATION, tabContent);
        section(Page.NAVIGATION, "pagination");
        OreLabel pageNumber = label(Page.NAVIGATION, message("pagination.value", pagination));
        add(Page.NAVIGATION, new OrePagination(8, pagination, selected -> {
            pagination = selected;
            pageNumber.setMessage(message("pagination.value", pagination));
        }));
        section(Page.NAVIGATION, "layers");
        button(Page.NAVIGATION, "modal", OrePalette.COLORED, this::openModal);
        for (OreMenuPresets.Kind kind : OreMenuPresets.Kind.values()) {
            add(Page.NAVIGATION, new OreButton(0, 0, 100, 28,
                    message("presets." + kind.name().toLowerCase(Locale.ROOT)), OrePalette.GENERAL,
                    trigger -> OreMenuDemo.open(kind, trigger::getRectangle)));
        }
        button(Page.NAVIGATION, "popup", OrePalette.COLORED, this::openPopup);
        button(Page.NAVIGATION, "confirm", OrePalette.DESTRUCTIVE, () -> minecraft.pushGuiLayer(new OrePopupScreen(message("confirm"), popup -> {
            popup.add(new OreLabel(popup.font(), message("confirm.body"), 100));
            popup.addAction(message("confirm.accept"), OrePalette.COLORED, () -> status = message("confirm.done"));
            popup.addAction(Component.translatable("gui.cancel"), () -> status = message("confirm.cancelled"));
        })));
        button(Page.NAVIGATION, "menu", OrePalette.GENERAL, () -> minecraft.pushGuiLayer(new OrePopupScreen(message("menu"), popup -> {
            popup.addAction(message("menu.action"), () -> status = message("menu.done"));
            OreButton nested = popup.add(new OreButton(0, 0, 100, 28, message("menu.nested"), OrePalette.GENERAL, button -> openPopup()));
            nested.setAppearance(OreButton.Appearance.MENU);
        }).menu()));
    }

    private void openModal() {
        minecraft.pushGuiLayer(new OreModalScreen(message("modal"), modal -> {
            modal.add(new OreLabel(modal.font(), message("modal.body"), 176));
            OreTextField name = modal.add(new OreTextField(modal.font(), 0, 0, 176, message("text.label"), message("modal.field.description")));
            name.setMaxLength(64);
            OreButton accept = modal.addAction(message("modal.accept"), OrePalette.COLORED,
                    () -> status = message("modal.done", name.getValue()));
            accept.active = false;
            modal.addConfirmation(message("modal.checkbox"), false, checked -> accept.active = checked);
            modal.addAction(Component.translatable("gui.cancel"), () -> status = message("confirm.cancelled"));
        }));
    }

    private void openPopup() {
        minecraft.pushGuiLayer(new OrePopupScreen(message("popup"), popup -> {
            popup.add(new OreLabel(popup.font(), message("popup.body"), 100));
            popup.addAction(Component.translatable("gui.done"), OrePalette.COLORED, () -> status = message("popup.closed"));
        }));
    }

    private void updateProgress(double value) {
        if (value != progress) {
            progress = value;
            progressSliders.forEach(slider -> slider.setProgress(value));
            status = message("slider.changed", Math.round(value * 100));
        }
    }

    private void selectPage(Page next) {
        if (next != page) {
            pages.get(page).scroll = scrollOffset;
            cancelInteraction();
            clearFocus();
            page = next;
            scrollOffset = pages.get(page).scroll;
            showPage();
            relayout();
        }
    }

    private void showPage() {
        pages.forEach((owner, gallery) -> gallery.widgets.forEach(widget -> widget.visible = owner == page));
        navigation.select(page.ordinal());
    }

    private void relayout() {
        if (layout != null) {
            GalleryPage active = pages.get(page);
            active.column.setWidth(Math.max(32, layout.width() - 32));
            contentHeight = active.column.getHeight() + 32;
            clampScroll();
            arrangeContent();
        }
    }

    @Override
    protected void repositionElements() {
        cancelInteraction();
        layout = TestScreenLayout.of(width, height);
        viewport = new ScreenRectangle(layout.x(), layout.top(), layout.width(), layout.height());
        navigation.setWidth(layout.navigationWidth());
        navigation.fitHeight(layout.height());
        navigation.setPosition(layout.navigationX(), layout.top());
        topBar.setWidth(width);
        topBar.setPosition(0, 0);
        relayout();
        revealFocused();
    }

    @Override
    protected void arrangeContent() {
        pages.get(page).column.setPosition(viewport.left() + 16, viewport.top() + 16 - scrollOffset);
    }

    @Override
    protected void renderContentBackground(GuiGraphics graphics) {
        OreDrawing.panel(graphics, viewport.left(), viewport.top() - scrollOffset, viewport.width(), Math.max(viewport.height(), contentHeight));
        for (OreSection section : pages.get(page).sections) {
            section.render(graphics, 0, 0, 0);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OreDrawing.BACKGROUND);
        OreDrawing.scrollingText(graphics, font, SUBTITLE, 12, 23, width - 12, 38, OreDrawing.MUTED);
        graphics.fill(0, height - 24, width, height, 0xFF151516);
        graphics.fill(0, height - 24, width, height - 23, 0xFF515453);
        OreDrawing.scrollingText(graphics, font, status, 12, height - 22, width - 12, height - 3, OreDrawing.MUTED);
    }
}
