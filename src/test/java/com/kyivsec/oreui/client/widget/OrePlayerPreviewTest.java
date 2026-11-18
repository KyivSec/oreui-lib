package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.preview.OrePlayerAppearance;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePlayerPreviewTest {
    private static OrePlayerAppearance appearance(OrePlayerAppearance.Model model) {
        return new OrePlayerAppearance(ResourceLocation.fromNamespaceAndPath("example", "skin"), model);
    }

    @Test
    void oldConstructorStillUsesTheEntityPathWhileAppearanceOptsIntoStandaloneRendering() {
        var preview = new OrePlayerPreview(Component.literal("Player"), 160);
        assertFalse(preview.isStandalone()); assertNull(preview.getAppearance());
        var appearance = appearance(OrePlayerAppearance.Model.CLASSIC);
        preview.setAppearance(appearance);
        assertTrue(preview.isStandalone()); assertTrue(preview.hasModel());
        assertSame(appearance, preview.getAppearance());
        preview.releaseRenderResources();
        assertTrue(preview.hasModel()); assertSame(appearance, preview.getAppearance());
        preview.useEntityRendering();
        assertFalse(preview.isStandalone()); assertNull(preview.getAppearance());
        assertThrows(NullPointerException.class, () -> preview.setAppearance(null));
    }

    @Test
    void standaloneClockWalkingAndSwingDoNotNeedMinecraftWorldOrPlayer() {
        AtomicLong clock = new AtomicLong();
        var preview = new OrePlayerPreview(Component.literal("Player"), 160, appearance(OrePlayerAppearance.Model.SLIM), clock::get);
        preview.setWidth(2); // Sample render interpolation without requiring GL.
        preview.setWalking(true); preview.setCrouching(true);
        preview.tick(); preview.playSwing(InteractionHand.OFF_HAND);
        clock.set(75_000_000);
        preview.renderWidget(null, 0, 0, 0.42F);
        var pose = preview.standalonePose(0.5F, 20, 10);
        assertEquals(0.075F, pose.seconds(), 1e-6);
        assertEquals(0.25F, pose.swingProgress(), 1e-6);
        assertEquals(HumanoidArm.LEFT, pose.swingArm());
        assertTrue(pose.walking()); assertTrue(pose.crouching());
        preview.setAnimationRunning(false);
        clock.set(5_075_000_000L);
        preview.tick(); preview.renderWidget(null, 0, 0, 1);
        assertEquals(pose.seconds(), preview.standalonePose(0.5F, 0, 0).seconds());
        assertEquals(pose.swingProgress(), preview.standalonePose(0.5F, 0, 0).swingProgress());
        preview.setAnimationRunning(true);
        clock.set(5_100_000_000L);
        preview.renderWidget(null, 0, 0, 0);
        assertEquals(0.1F, preview.standalonePose(0, 0, 0).seconds(), 1e-6);
        preview.setRotation(10, 35, 20); preview.setModelScale(1.5F); preview.setModelOffset(2, 3, 4);
        assertEquals(35, preview.getYaw()); assertEquals(1.5F, preview.getModelScale());
    }

    @Test
    void layersCanBeDetachedAndInvalidExtensionHooksAreRejected() {
        var preview = new OrePlayerPreview(Component.empty(), 100, appearance(OrePlayerAppearance.Model.CLASSIC));
        java.util.function.Consumer<com.kyivsec.oreui.client.preview.OrePlayerRenderContext> layer = context -> {};
        preview.addStandaloneLayer(layer);
        assertTrue(preview.removeStandaloneLayer(layer)); assertFalse(preview.removeStandaloneLayer(layer));
        assertThrows(NullPointerException.class, () -> preview.addStandaloneLayer(null));
        assertThrows(NullPointerException.class, () -> preview.setStandalonePoseModifier(null));
        assertThrows(NullPointerException.class, () -> preview.setStandaloneModelFactory(null));
    }

    @Test
    void swingCompletionLoopingAndHiddenHostsUseOnlyUiPoseSteps() {
        AtomicLong clock = new AtomicLong();
        var preview = new OrePlayerPreview(Component.empty(), 100, appearance(OrePlayerAppearance.Model.CLASSIC), clock::get);
        preview.tick(); preview.playSwing(InteractionHand.MAIN_HAND);
        for (int tick = 1; tick <= 6; tick++) { clock.set(tick * 50_000_000L); preview.tick(); }
        assertEquals(0, preview.standalonePose(0, 0, 0).swingProgress());
        preview.setSwingLoop(true);
        for (int tick = 7; tick <= 13; tick++) { clock.set(tick * 50_000_000L); preview.tick(); }
        float progress = preview.standalonePose(0, 0, 0).swingProgress();
        assertTrue(progress > 0);
        preview.setAnimationHostVisible(false);
        clock.set(20_000_000_000L); preview.tick();
        assertEquals(progress, preview.standalonePose(0, 0, 0).swingProgress());
        preview.setAppearance(appearance(OrePlayerAppearance.Model.SLIM));
        assertTrue(preview.hasModel());
    }

    @Test void compatibleAppearanceModeIsOptInAndChangingItPreservesTheDraftAndCamera() {
        var skin = appearance(OrePlayerAppearance.Model.CLASSIC);
        var preview = new OrePlayerPreview(Component.literal("Player"), 160, skin);
        assertEquals(OrePlayerPreview.AppearanceRendering.STANDALONE, preview.getAppearanceRendering());
        preview.setRotation(12, 42, 0);
        preview.focusView(0.75F, 3);
        preview.setAppearanceRendering(OrePlayerPreview.AppearanceRendering.PLAYER_RENDERER);
        assertFalse(preview.isStandalone());
        assertSame(skin, preview.getAppearance());
        assertTrue(preview.hasModel()); // The no-world fallback is always available for a supplied appearance.
        var changed = appearance(OrePlayerAppearance.Model.SLIM);
        preview.setAppearance(changed);
        assertEquals(OrePlayerPreview.AppearanceRendering.PLAYER_RENDERER, preview.getAppearanceRendering());
        assertSame(changed, preview.getAppearance());
        assertEquals(42, preview.getYaw());
        assertEquals(12, preview.getPitch());
        assertEquals(3, preview.getModelScale());
        assertEquals(0.75F, preview.getFocusY());
        preview.releaseRenderResources();
        assertSame(changed, preview.getAppearance());
        preview.setAppearanceRendering(OrePlayerPreview.AppearanceRendering.STANDALONE);
        assertTrue(preview.isStandalone());
        assertThrows(NullPointerException.class, () -> preview.setAppearanceRendering(null));
    }

    @Test void compatibilityModeFallsBackToStandaloneOnlyWhenTheWorldIsUnavailable() {
        var compatible = OrePlayerPreview.AppearanceRendering.PLAYER_RENDERER;
        var standalone = OrePlayerPreview.AppearanceRendering.STANDALONE;
        assertTrue(compatible.usesStandalone(false));
        assertFalse(compatible.usesStandalone(true));
        assertTrue(standalone.usesStandalone(false));
        assertTrue(standalone.usesStandalone(true));
    }

    @Test void rendererSkinUsesTheDraftTextureAndCorrectArmVariantWithoutBorrowingCapes() {
        for (var variant : OrePlayerAppearance.Model.values()) {
            var appearance = appearance(variant);
            var skin = OrePlayerPreview.appearanceSkin(appearance);
            assertEquals(appearance.skin(), skin.texture());
            assertEquals(variant == OrePlayerAppearance.Model.SLIM ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, skin.model());
            assertNull(skin.capeTexture()); assertNull(skin.elytraTexture());
            assertNull(skin.textureUrl()); assertFalse(skin.secure());
        }
    }

    @Test void previewProfilesDoNotShareTheRealPlayersUuidOrProfileProperties() {
        var source = new GameProfile(UUID.randomUUID(), "Example");
        source.getProperties().put("textures", new Property("textures", "source-texture-property"));
        UUID firstId = UUID.randomUUID(), secondId = UUID.randomUUID();
        var first = OrePlayerPreview.previewProfile(firstId, source);
        var second = OrePlayerPreview.previewProfile(secondId, source);
        assertEquals(source.getName(), first.getName());
        assertEquals(firstId, first.getId()); assertEquals(secondId, second.getId());
        assertNotEquals(source.getId(), first.getId()); assertNotEquals(first.getId(), second.getId());
        assertNotSame(source.getProperties(), first.getProperties());
        assertTrue(first.getProperties().isEmpty());
        assertEquals(1, source.getProperties().size());
    }
}
