package com.kyivsec.oreui.client.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Internal native-model renderer. It never constructs an entity or calls entity-based setupAnim. */
public final class OreStandalonePlayerRenderer {
    private final Map<OrePlayerAppearance.Model, OrePlayerModel> models = new EnumMap<>(OrePlayerAppearance.Model.class);
    private OrePlayerModel.Factory factory = OrePlayerModel::bake;
    private long generation = -1;

    public void setFactory(OrePlayerModel.Factory factory) { this.factory = Objects.requireNonNull(factory); clear(); }
    public void clear() { models.clear(); generation = -1; }

    OrePlayerModel model(EntityModelSet modelSet, OrePlayerAppearance.Model variant, long generation) {
        if (this.generation != generation) { models.clear(); this.generation = generation; }
        return models.computeIfAbsent(variant, key -> Objects.requireNonNull(factory.create(modelSet, key)));
    }

    public void render(GuiGraphics graphics, EntityModelSet modelSet, OrePlayerAppearance appearance, OrePlayerPose pose,
                       BiConsumer<OrePlayerModel, OrePlayerPose> modifier, Consumer<OrePlayerRenderContext> layers) {
        OrePlayerModel model = model(modelSet, appearance.model(), OrePreviewResources.generation());
        applyPose(model, appearance, pose);
        modifier.accept(model, pose);
        syncOverlays(model);
        Lighting.setupForEntityInInventory();
        graphics.pose().pushPose();
        try {
            graphics.pose().mulPose(Axis.YP.rotationDegrees(180));
            graphics.pose().scale(-1, -1, 1);
            graphics.pose().translate(0, -0.5, 0); // Center the native model's -8..24 pixel height.
            var context = new OrePlayerRenderContext(graphics, model, appearance, pose, LightTexture.FULL_BRIGHT);
            context.renderSkinLayer(appearance.skin(), -1);
            graphics.pose().pushPose();
            try { layers.accept(context); }
            finally { graphics.pose().popPose(); }
        } finally { graphics.pose().popPose(); }
    }

    public static void applyPose(OrePlayerModel holder, OrePlayerAppearance appearance, OrePlayerPose pose) {
        holder.root().getAllParts().forEach(part -> { part.resetPose(); part.visible = true; part.skipDraw = false; });
        var model = holder.model();
        model.young = false; model.riding = false; model.attackTime = pose.swingProgress();
        model.crouching = pose.crouching(); model.swimAmount = 0;
        model.leftArmPose = model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        float age = pose.seconds() * 20;
        model.head.yRot = pose.lookYaw() * Mth.DEG_TO_RAD;
        model.head.xRot = pose.lookPitch() * Mth.DEG_TO_RAD;
        if (pose.walking()) {
            model.rightArm.xRot = Mth.cos(age * 0.6662F + Mth.PI) * 0.6F;
            model.leftArm.xRot = Mth.cos(age * 0.6662F) * 0.6F;
            model.rightLeg.xRot = Mth.cos(age * 0.6662F) * 0.84F;
            model.leftLeg.xRot = Mth.cos(age * 0.6662F + Mth.PI) * 0.84F;
        }
        model.rightArm.zRot += Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        model.leftArm.zRot -= Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        model.rightArm.xRot += Mth.sin(age * 0.067F) * 0.05F;
        model.leftArm.xRot -= Mth.sin(age * 0.067F) * 0.05F;
        if (pose.crouching()) {
            model.body.xRot = 0.5F;
            model.rightArm.xRot += 0.4F; model.leftArm.xRot += 0.4F;
            model.rightLeg.z = model.leftLeg.z = 4;
            model.rightLeg.y = model.leftLeg.y = 12.2F;
            model.head.y = 4.2F; model.body.y = 3.2F;
            model.leftArm.y += 3.2F; model.rightArm.y += 3.2F;
        }
        if (pose.swingProgress() > 0) {
            var arm = pose.swingArm() == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
            arm.xRot -= Mth.sin(Mth.sqrt(pose.swingProgress()) * Mth.PI) * 1.2F;
            arm.zRot += Mth.sin(pose.swingProgress() * Mth.PI) * -0.4F;
        }
        model.hat.visible = appearance.parts().contains(PlayerModelPart.HAT);
        model.jacket.visible = appearance.parts().contains(PlayerModelPart.JACKET);
        model.leftSleeve.visible = appearance.parts().contains(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = appearance.parts().contains(PlayerModelPart.RIGHT_SLEEVE);
        model.leftPants.visible = appearance.parts().contains(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = appearance.parts().contains(PlayerModelPart.RIGHT_PANTS_LEG);
        syncOverlays(holder);
    }

    public static void syncOverlays(OrePlayerModel holder) {
        var model = holder.model();
        model.hat.copyFrom(model.head); model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm); model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg); model.rightPants.copyFrom(model.rightLeg);
    }
}
