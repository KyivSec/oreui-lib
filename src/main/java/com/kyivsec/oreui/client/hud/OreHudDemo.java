package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.OreUi;
import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreDropdown;
import com.kyivsec.oreui.client.widget.OreItemPreview;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OreProgressIndicator;
import com.kyivsec.oreui.client.widget.OreSlider;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Explicitly enabled local demo, not a HUD forced on consuming mods. */
public final class OreHudDemo {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OreUi.ID, "demo_hud");
    public static final OreHudLayer LAYER = new OreHudLayer(ID, OreHudDemo::build).enabled(false).interactive(true).size(228, 230);
    private OreHudDemo() {}

    private static OreGrid build() {
        var font = Minecraft.getInstance().font;
        double[] progress = {0.5}; int[] clicks = {0};
        OreGrid grid = new OreGrid(6, 2).gap(4).padding(8);
        grid.add(new OreLabel(font, Component.translatable("oreui.hud.demo.title"), 200), 0, 0, 1, 2, OreGrid.Cell.DEFAULT);
        grid.add(new OreLabel(font, Component.translatable("oreui.hud.demo.hint"), 200), 1, 0, 1, 2, OreGrid.Cell.DEFAULT);
        grid.add(new OreProgressIndicator(Component.translatable("oreui.hud.demo.progress"), OreProgressIndicator.Kind.BAR, () -> progress[0]),
                2, 0, 1, 2, OreGrid.Cell.DEFAULT.padding(2));
        grid.add(new OreSlider(0, 0, 200, 34, Component.translatable("oreui.hud.demo.progress"), progress[0], value -> progress[0] = value),
                3, 0, 1, 2, OreGrid.Cell.DEFAULT.padding(2));
        grid.add(new OreButton(0, 0, 100, 28, Component.translatable("oreui.hud.demo.clicks", 0), OrePalette.COLORED,
                button -> button.setMessage(Component.translatable("oreui.hud.demo.clicks", ++clicks[0]))), 4, 0, OreGrid.Cell.DEFAULT.padding(2));
        grid.add(new OreDropdown(Component.translatable("oreui.hud.demo.material"),
                List.of(Component.translatable("oreui.test.choice.0"), Component.translatable("oreui.test.choice.1"), Component.translatable("oreui.test.choice.2")),
                0, index -> {}), 4, 1, OreGrid.Cell.DEFAULT.padding(2));
        OreItemPreview preview = new OreItemPreview(Component.translatable("oreui.hud.demo.model"), new ItemStack(Items.GRASS_BLOCK), 60);
        preview.setFloating(true);
        grid.add(preview, 5, 0, 1, 2, OreGrid.Cell.DEFAULT);
        return grid;
    }

    public static void toggle() { LAYER.enabled(!LAYER.isEnabled()); }
    public static void edit() { LAYER.enabled(true); OreHudManager.startInteraction(ID); }
}
