package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.compat.PlayerEquipmentCompat;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class OrePlayerPreview extends OreModelPreview {
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private PlayerEquipmentCompat.Snapshot accessories = PlayerEquipmentCompat.empty();
    private HumanoidArm mainArm = HumanoidArm.RIGHT;
    private PreviewPlayer player;
    private boolean walking;
    private boolean crouching;
    private boolean followMouse;
    private boolean swingLoop;
    private boolean useItemLoop;
    private int swingCooldown;
    private InteractionHand swingHand = InteractionHand.MAIN_HAND;
    private boolean pendingSwing;
    private InteractionHand usingHand;

    public OrePlayerPreview(Component label, int height) {
        super(label, height);
        setRotation(-10, -25, 0);
    }

    public void setMainHand(ItemStack item) {
        setEquipment(EquipmentSlot.MAINHAND, item);
    }

    public void setOffHand(ItemStack item) {
        setEquipment(EquipmentSlot.OFFHAND, item);
    }

    /** Captures all six vanilla slots and optional Curios slots; never retains source inventories. */
    public void copyEquipmentFrom(Player source) {
        Objects.requireNonNull(source);
        Map<EquipmentSlot, ItemStack> copied = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.BODY) copied.put(slot, source.getItemBySlot(slot).copy());
        }
        PlayerEquipmentCompat.Snapshot copiedAccessories = PlayerEquipmentCompat.capture(source);
        stopUsingItem();
        pendingSwing = false;
        equipment.clear();
        equipment.putAll(copied);
        mainArm = source.getMainArm();
        accessories = copiedAccessories;
        if (player != null) {
            player.swinging = false;
            player.attackAnim = player.oAttackAnim = 0;
            applyEquipment();
        }
    }

    /** Returns false, leaving the existing snapshot untouched, when no client player is available. */
    public boolean copyEquipmentFromCurrentPlayer() {
        Player source = Minecraft.getInstance().player;
        if (source == null) return false;
        copyEquipmentFrom(source);
        return true;
    }

    public boolean isCuriosSnapshotAvailable() {
        return accessories.available();
    }

    /** Counts occupied regular slots, including hidden ones, rather than stack item quantities. */
    public int getCopiedCuriosCount() {
        return accessories.equippedCount();
    }

    public int getCopiedCosmeticCuriosCount() {
        return accessories.cosmeticCount();
    }

    private void applyEquipment() {
        equipment.forEach((slot, item) -> player.equip(slot, item.copy()));
        player.setMainArm(mainArm);
        accessories.applyTo(player);
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        Objects.requireNonNull(slot);
        Objects.requireNonNull(item);
        if (slot == EquipmentSlot.BODY) throw new IllegalArgumentException("Players do not have an animal body equipment slot");
        stopUsingItem();
        ItemStack copy = item.copy();
        equipment.put(slot, copy);
        if (player != null) player.equip(slot, copy.copy());
    }

    public void playSwing(InteractionHand hand) {
        swingHand = Objects.requireNonNull(hand);
        pendingSwing = true;
        stopUsingItem();
        if (player != null) startSwing();
    }

    public void setSwingLoop(boolean loop) {
        swingLoop = loop;
    }

    public void setWalking(boolean walking) {
        this.walking = walking;
    }

    public void setCrouching(boolean crouching) {
        this.crouching = crouching;
        if (player != null) player.crouch(crouching);
    }

    public void setFollowMouse(boolean followMouse) {
        this.followMouse = followMouse;
    }

    public void playUseItem(InteractionHand hand) {
        usingHand = Objects.requireNonNull(hand);
        pendingSwing = false;
        if (player != null) player.beginUse(hand);
    }

    public void setUseItemLoop(boolean loop) {
        useItemLoop = loop;
    }

    public void stopUsingItem() {
        usingHand = null;
        if (player != null) player.endUse();
    }

    private boolean preparePlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            player = null;
            return false;
        }
        if (player == null || player.level() != minecraft.level) {
            player = new PreviewPlayer(minecraft.level, minecraft.player.getGameProfile());
            applyEquipment();
            player.crouch(crouching);
            if (usingHand != null) player.beginUse(usingHand);
            if (pendingSwing) startSwing();
        }
        return true;
    }

    private void startSwing() {
        player.animateSwing(swingHand);
        pendingSwing = false;
        swingCooldown = 12;
    }

    @Override
    protected void tickModel() {
        if (!preparePlayer()) return;
        if (swingCooldown > 0) swingCooldown--;
        if (swingLoop && swingCooldown == 0 && usingHand == null) startSwing();
        player.tickPreview(walking);
        if (usingHand != null && !player.isUsingItem()) {
            if (useItemLoop) player.beginUse(usingHand);
            else usingHand = null;
        }
    }

    @Override
    protected boolean hasModel() {
        return preparePlayer();
    }

    @Override
    protected Component emptyMessage() {
        return Component.translatable("oreui.preview.player.unavailable");
    }

    @Override
    protected float pixelsPerUnit() {
        return Math.max(1, Math.min((width - 16) / 1.6F, (height - 16) / 2.2F));
    }

    @Override
    protected void renderModel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float lookYaw = followMouse && isHovered() ? Mth.clamp((getX() + width / 2.0F - mouseX) * 0.5F, -35, 35) : 0;
        float lookPitch = followMouse && isHovered() ? Mth.clamp((mouseY - getY() - height / 2.0F) * 0.5F, -25, 25) : 0;
        player.yHeadRot = lookYaw;
        player.setXRot(lookPitch);
        graphics.pose().translate(0, -player.getBbHeight() / 2.0F, 0);
        Lighting.setupForEntityInInventory();
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player).render(player, 0, partialTick,
                graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT);
    }

    private static final class PreviewPlayer extends RemotePlayer {
        private final PlayerTeam previewTeam;

        private PreviewPlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
            previewTeam = new Scoreboard().addPlayerTeam("oreui_preview");
            previewTeam.setNameTagVisibility(Team.Visibility.NEVER);
            setSilent(true);
            setOnGround(true);
        }

        @Override
        public PlayerTeam getTeam() {
            return previewTeam;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public boolean isModelPartShown(PlayerModelPart part) {
            return true;
        }

        private void equip(EquipmentSlot slot, ItemStack item) {
            switch (slot) {
                case MAINHAND -> getInventory().items.set(getInventory().selected, item);
                case OFFHAND -> getInventory().offhand.set(0, item);
                case FEET, LEGS, CHEST, HEAD -> getInventory().armor.set(slot.getIndex(), item);
                default -> throw new IllegalArgumentException("Unsupported player equipment slot: " + slot);
            }
        }

        private void crouch(boolean crouching) {
            setShiftKeyDown(crouching);
            setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        }

        private void animateSwing(InteractionHand hand) {
            endUse();
            swingTime = -1;
            swinging = true;
            swingingArm = hand;
        }

        private void beginUse(InteractionHand hand) {
            endUse();
            ItemStack item = getItemInHand(hand);
            int duration = item.isEmpty() ? 0 : item.getUseDuration(this);
            if (duration <= 0) return;
            swinging = false;
            attackAnim = oAttackAnim = 0;
            setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
            setLivingEntityFlag(1, true);
            useItem = item;
            useItemRemaining = duration;
        }

        private void endUse() {
            setLivingEntityFlag(1, false);
            setLivingEntityFlag(2, false);
            useItem = ItemStack.EMPTY;
            useItemRemaining = 0;
        }

        private void tickPreview(boolean walking) {
            tickCount++;
            oAttackAnim = attackAnim;
            xRotO = getXRot();
            yRotO = getYRot();
            yBodyRotO = yBodyRot;
            yHeadRotO = yHeadRot;
            updateSwingTime();
            walkAnimation.update(walking ? 0.6F : 0, 0.4F);
            if (isUsingItem() && --useItemRemaining <= 0) endUse();
        }
    }
}
