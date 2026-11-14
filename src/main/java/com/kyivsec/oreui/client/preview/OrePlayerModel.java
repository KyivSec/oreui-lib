package com.kyivsec.oreui.client.preview;

import java.util.Objects;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** A preview-private native model and its resettable root, including any caller-added parts. */
public record OrePlayerModel(ModelPart root, PlayerModel<LivingEntity> model) {
    public OrePlayerModel { Objects.requireNonNull(root); Objects.requireNonNull(model); }

    @FunctionalInterface
    public interface Factory {
        OrePlayerModel create(EntityModelSet models, OrePlayerAppearance.Model variant);
    }

    public static OrePlayerModel bake(EntityModelSet models, OrePlayerAppearance.Model variant) {
        boolean slim = variant == OrePlayerAppearance.Model.SLIM;
        ModelPart root = models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
        return new OrePlayerModel(root, new PlayerModel<>(root, slim));
    }
}
