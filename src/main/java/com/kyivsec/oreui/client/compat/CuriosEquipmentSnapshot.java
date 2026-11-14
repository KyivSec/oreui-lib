package com.kyivsec.oreui.client.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

/** Loaded only after the optional Curios presence check. Never installs data on the source. */
final class CuriosEquipmentSnapshot {
    private CuriosEquipmentSnapshot() {
    }

    static PlayerEquipmentCompat.Snapshot capture(LivingEntity source) {
        var inventory = CuriosApi.getCuriosInventory(source);
        List<SlotSnapshot> slots = new ArrayList<>();
        int equippedCount = 0;
        int cosmeticCount = 0;
        if (inventory.isPresent()) {
            for (var entry : inventory.get().getCurios().entrySet()) {
                ICurioStacksHandler handler = entry.getValue();
                var stacks = handler.getStacks();
                var cosmetics = handler.getCosmeticStacks();
                var renders = handler.getRenders();
                var activeStates = handler.getActiveStates();
                List<ItemStack> equipped = new ArrayList<>();
                List<ItemStack> cosmetic = new ArrayList<>();
                List<Boolean> render = new ArrayList<>();
                List<Boolean> active = new ArrayList<>();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack item = stacks.getStackInSlot(i).copy();
                    ItemStack cosmeticItem = i < cosmetics.getSlots() ? cosmetics.getStackInSlot(i).copy() : ItemStack.EMPTY;
                    equipped.add(item);
                    cosmetic.add(cosmeticItem);
                    render.add(i < renders.size() && renders.get(i));
                    active.add(i >= activeStates.size() || activeStates.get(i));
                    if (!item.isEmpty()) equippedCount++;
                    if (!cosmeticItem.isEmpty()) cosmeticCount++;
                }
                slots.add(new SlotSnapshot(entry.getKey(), List.copyOf(equipped), List.copyOf(cosmetic),
                        List.copyOf(render), List.copyOf(active), handler.isVisible(), handler.hasCosmetic(),
                        handler.canToggleRendering(), handler.getDropRule()));
            }
        }
        List<SlotSnapshot> snapshot = List.copyOf(slots);
        return new PlayerEquipmentCompat.Snapshot(inventory.isPresent(), equippedCount, cosmeticCount,
                target -> install(target, snapshot));
    }

    private static void install(LivingEntity target, List<SlotSnapshot> snapshot) {
        CuriosApi.getCuriosInventory(target).ifPresent(inventory -> {
            Map<String, ICurioStacksHandler> handlers = new LinkedHashMap<>();
            for (SlotSnapshot slot : snapshot) {
                // Use the captured final size, not slot modifiers. Fresh handlers have no pending resize.
                CurioStacksHandler handler = new CurioStacksHandler(inventory, slot.identifier(), slot.equipped().size(),
                        slot.visible(), slot.hasCosmetic(), slot.canToggleRendering(), slot.dropRule());
                var stacks = handler.getStacks();
                var cosmetics = handler.getCosmeticStacks();
                var renders = handler.getRenders();
                var activeStates = handler.getActiveStates();
                for (int i = 0; i < slot.equipped().size(); i++) {
                    // Direct setters on fresh Curios 9.5 handlers do not validate or fire equip hooks.
                    // Copy again so renderers cannot mutate the stored snapshot or another preview entity.
                    stacks.setStackInSlot(i, slot.equipped().get(i).copy());
                    cosmetics.setStackInSlot(i, slot.cosmetic().get(i).copy());
                    renders.set(i, slot.render().get(i));
                    activeStates.set(i, slot.active().get(i));
                }
                handlers.put(slot.identifier(), handler);
            }
            // No readTag/reset, modifier reconstruction, inventory ticking or gameplay callbacks.
            inventory.setCurios(handlers);
        });
    }

    private record SlotSnapshot(String identifier, List<ItemStack> equipped, List<ItemStack> cosmetic,
                                List<Boolean> render, List<Boolean> active, boolean visible, boolean hasCosmetic,
                                boolean canToggleRendering, ICurio.DropRule dropRule) {
    }
}
