package com.kyivsec.oreui.client.compat;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/** Internal bridge; no optional-mod types appear in its public signatures. */
public final class PlayerEquipmentCompat {
    private static final Snapshot EMPTY = new Snapshot(false, 0, 0, target -> {});

    private PlayerEquipmentCompat() {
    }

    public static Snapshot empty() {
        return EMPTY;
    }

    public static Snapshot capture(LivingEntity source) {
        return ModList.get().isLoaded("curios") ? CuriosEquipmentSnapshot.capture(source) : EMPTY;
    }

    public record Snapshot(boolean available, int equippedCount, int cosmeticCount, Consumer<LivingEntity> installer) {
        public void applyTo(LivingEntity target) {
            installer.accept(target);
        }
    }
}
