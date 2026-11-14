package com.kyivsec.oreui.client.preview;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreStandalonePlayerRendererTest {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("example", "skin");

    private static OrePlayerModel model(OrePlayerAppearance.Model variant) {
        boolean slim = variant == OrePlayerAppearance.Model.SLIM;
        var mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        mesh.getRoot().addOrReplaceChild("custom", CubeListBuilder.create(), PartPose.offset(1, 2, 3));
        var root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        return new OrePlayerModel(root, new PlayerModel<>(root, slim));
    }

    @Test
    void cachesArePerPreviewAndVariantAndRebakeAfterResourceReloadOrFactoryReplacement() {
        AtomicInteger builds = new AtomicInteger();
        var renderer = new OreStandalonePlayerRenderer();
        renderer.setFactory((models, variant) -> { builds.incrementAndGet(); return model(variant); });
        var classic = renderer.model(null, OrePlayerAppearance.Model.CLASSIC, 1);
        assertSame(classic, renderer.model(null, OrePlayerAppearance.Model.CLASSIC, 1));
        var slim = renderer.model(null, OrePlayerAppearance.Model.SLIM, 1);
        assertNotSame(classic, slim);
        assertEquals(2, builds.get());
        assertNotEquals(classic.model().leftArm.y, slim.model().leftArm.y);
        assertNotSame(classic, renderer.model(null, OrePlayerAppearance.Model.CLASSIC, 2));
        assertEquals(3, builds.get());
        renderer.setFactory((models, variant) -> model(variant));
        assertNotSame(slim, renderer.model(null, OrePlayerAppearance.Model.SLIM, 2));
        renderer.clear();
        assertNotSame(classic, renderer.model(null, OrePlayerAppearance.Model.CLASSIC, 2));
    }

    @Test
    void posesWalkCrouchLookAndSwingWithoutAnEntityAndResetAllPartsEveryFrame() {
        var holder = model(OrePlayerAppearance.Model.SLIM);
        var appearance = new OrePlayerAppearance(SKIN, OrePlayerAppearance.Model.SLIM, Set.of(PlayerModelPart.HAT));
        var pose = new OrePlayerPose(0.25F, true, true, 30, -20, HumanoidArm.LEFT, 0.5F);
        OreStandalonePlayerRenderer.applyPose(holder, appearance, pose);
        var model = holder.model();
        assertTrue(model.crouching);
        assertEquals(0.5F, model.body.xRot);
        assertEquals(Math.toRadians(30), model.head.yRot, 1e-6);
        assertEquals(Math.toRadians(-20), model.head.xRot, 1e-6);
        assertEquals(model.head.xRot, model.hat.xRot);
        assertEquals(model.leftArm.xRot, model.leftSleeve.xRot);
        assertTrue(model.hat.visible); assertFalse(model.jacket.visible);
        assertFalse(model.leftPants.visible); assertFalse(model.leftSleeve.visible);
        var custom = holder.root().getChild("custom");
        custom.x = 100; custom.visible = false; custom.skipDraw = true;
        model.young = true; model.riding = true;
        OreStandalonePlayerRenderer.applyPose(holder, new OrePlayerAppearance(SKIN, OrePlayerAppearance.Model.SLIM),
                new OrePlayerPose(0, false, false, 0, 0, HumanoidArm.RIGHT, 0));
        assertEquals(1, custom.x); assertTrue(custom.visible); assertFalse(custom.skipDraw);
        assertEquals(0, model.body.xRot); assertEquals(0, model.head.yRot);
        assertEquals(0, model.leftLeg.xRot); assertEquals(0, model.leftLeg.z);
        assertEquals(2.5F, model.leftArm.y);
        assertTrue(model.jacket.visible); assertFalse(model.crouching);
        assertFalse(model.young); assertFalse(model.riding);
    }

    @Test
    void modifyingOneModelsPoseCannotChangeAnotherPreviewsParts() {
        var first = model(OrePlayerAppearance.Model.CLASSIC);
        var second = model(OrePlayerAppearance.Model.CLASSIC);
        first.root().getChild("custom").x = 100;
        first.model().head.xRot = 1;
        assertEquals(1, second.root().getChild("custom").x);
        assertEquals(0, second.model().head.xRot);
    }
}
