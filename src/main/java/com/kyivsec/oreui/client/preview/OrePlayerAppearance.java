package com.kyivsec.oreui.client.preview;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Immutable, shareable standalone appearance. Texture IDs are borrowed, never released by a preview. */
public record OrePlayerAppearance(ResourceLocation skin, Model model, Set<PlayerModelPart> parts) {
    public enum Model { CLASSIC, SLIM }

    public OrePlayerAppearance {
        Objects.requireNonNull(skin); Objects.requireNonNull(model);
        parts = Set.copyOf(parts);
    }

    public OrePlayerAppearance(ResourceLocation skin, Model model) {
        this(skin, model, EnumSet.allOf(PlayerModelPart.class));
    }
}
