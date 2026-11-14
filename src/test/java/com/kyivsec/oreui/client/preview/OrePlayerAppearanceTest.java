package com.kyivsec.oreui.client.preview;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePlayerAppearanceTest {
    @Test
    void appearancesBorrowTextureIdsAndDefensivelyCopyPartVisibility() {
        var skin = ResourceLocation.fromNamespaceAndPath("example", "textures/skin.png");
        var parts = EnumSet.of(PlayerModelPart.HAT);
        var appearance = new OrePlayerAppearance(skin, OrePlayerAppearance.Model.SLIM, parts);
        parts.clear();
        assertSame(skin, appearance.skin());
        assertEquals(Set.of(PlayerModelPart.HAT), appearance.parts());
        assertThrows(UnsupportedOperationException.class, () -> appearance.parts().clear());
        assertEquals(EnumSet.allOf(PlayerModelPart.class), new OrePlayerAppearance(skin, OrePlayerAppearance.Model.CLASSIC).parts());
        assertThrows(NullPointerException.class, () -> new OrePlayerAppearance(null, OrePlayerAppearance.Model.CLASSIC));
        assertThrows(NullPointerException.class, () -> new OrePlayerAppearance(skin, null));
    }
}
