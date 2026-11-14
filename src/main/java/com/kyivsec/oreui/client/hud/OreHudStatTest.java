package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.OreUi;
import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.widget.OreStatBar;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Live, read-only player statistics in cell (0,0) of a screen-sized 5x5 grid. */
public final class OreHudStatTest {
    public static final int HEALTH_COLOR = 0xFFC43C3C;
    public static final int HUNGER_COLOR = 0xFFC5A477;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OreUi.ID, "stat_test_hud");
    public static final OreHudLayer LAYER = new OreHudLayer(ID, OreHudStatTest::build)
            .enabled(false).fillScreen().panel(false);

    private OreHudStatTest() {}

    private static OreGrid build() {
        return content(() -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getHealth();
        }, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getMaxHealth();
        }, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getFoodData().getFoodLevel();
        });
    }

    static OreGrid content(DoubleSupplier health, DoubleSupplier maximumHealth, DoubleSupplier hunger) {
        OreGrid screen = new OreGrid(5, 5);
        for (int row = 0; row < 5; row++) screen.row(row, OreGrid.Track.flex(1));
        OreGrid bars = new OreGrid(2, 1).padding(4).gap(4)
                .row(0, OreGrid.Track.flex(1, 0, 20)).row(1, OreGrid.Track.flex(1, 0, 20));
        var stretch = OreGrid.Cell.DEFAULT.align(OreGrid.Align.STRETCH, OreGrid.Align.STRETCH);
        bars.add(new OreStatBar(Component.translatable("oreui.hud.test.health"), HEALTH_COLOR, health, maximumHealth), 0, 0, stretch);
        bars.add(new OreStatBar(Component.translatable("oreui.hud.test.hunger"), HUNGER_COLOR, hunger, () -> 20), 1, 0, stretch);
        screen.add(bars, 0, 0, stretch);
        return screen;
    }

    public static void toggle() { LAYER.enabled(!LAYER.isEnabled()); }
}
