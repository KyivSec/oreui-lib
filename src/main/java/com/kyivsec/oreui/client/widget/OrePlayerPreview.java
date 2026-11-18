package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.compat.PlayerEquipmentCompat;
import com.kyivsec.oreui.client.mixin.EntityRenderDispatcherAccessor;
import com.kyivsec.oreui.client.preview.OreEntityPreviewRenderScope;
import com.kyivsec.oreui.client.preview.OrePlayerAppearance;
import com.kyivsec.oreui.client.preview.OrePlayerModel;
import com.kyivsec.oreui.client.preview.OrePlayerPose;
import com.kyivsec.oreui.client.preview.OrePlayerRenderContext;
import com.kyivsec.oreui.client.preview.OreStandalonePlayerRenderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.PlayerSkin;
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
import org.joml.Quaternionf;

public class OrePlayerPreview extends OreModelPreview {
    public enum AppearanceRendering {
        /** Entity-free, preview-private models and explicit standalone extensions. */
        STANDALONE,
        /** Native dispatcher/player renderer and installed mod hooks; standalone fallback without a world. */
        PLAYER_RENDERER;

        boolean usesStandalone(boolean worldAvailable) {
            return this == STANDALONE || !worldAvailable;
        }
    }

    private final UUID previewId = UUID.randomUUID();
    private AppearanceRendering appearanceRendering = AppearanceRendering.STANDALONE;
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
    private OrePlayerAppearance appearance;
    private final OreStandalonePlayerRenderer standalone = new OreStandalonePlayerRenderer();
    private final List<Consumer<OrePlayerRenderContext>> standaloneLayers = new ArrayList<>();
    private BiConsumer<OrePlayerModel, OrePlayerPose> poseModifier = (model, pose) -> {};
    private int standaloneSwingTicks = -1;

    public OrePlayerPreview(Component label, int height) {
        super(label, height);
        setRotation(-10, -25, 0);
    }

    /** Standalone native model path: no world, local player, profile lookup or fake entity. */
    public OrePlayerPreview(Component label, int height, OrePlayerAppearance appearance) {
        this(label, height);
        setAppearance(appearance);
    }

    OrePlayerPreview(Component label, int height, OrePlayerAppearance appearance, LongSupplier clock) {
        super(label, height, clock);
        setRotation(-10, -25, 0);
        setAppearance(appearance);
    }

    /** Updates the draft skin/model without changing the selected renderer or resetting an entity's animations. */
    public void setAppearance(OrePlayerAppearance appearance) {
        this.appearance = Objects.requireNonNull(appearance);
        if (player != null) player.setAppearance(appearance);
    }
    public OrePlayerAppearance getAppearance() { return appearance; }
    /** Reports the configured mode; PLAYER_RENDERER can still fall back to standalone without a world. */
    public boolean isStandalone() { return appearance != null && appearanceRendering == AppearanceRendering.STANDALONE; }

    public AppearanceRendering getAppearanceRendering() { return appearanceRendering; }

    public void setAppearanceRendering(AppearanceRendering rendering) {
        Objects.requireNonNull(rendering);
        if (appearanceRendering == rendering) return;
        appearanceRendering = rendering;
        releaseRenderResources();
    }

    /** Returns to the original local-profile/entity renderer; stored equipment snapshots are retained. */
    public void useEntityRendering() {
        appearance = null;
        if (player != null) player.setAppearance(null);
        standaloneSwingTicks = -1;
        standalone.clear();
    }

    public void setStandaloneModelFactory(OrePlayerModel.Factory factory) { standalone.setFactory(factory); }
    public void setStandalonePoseModifier(BiConsumer<OrePlayerModel, OrePlayerPose> modifier) {
        poseModifier = Objects.requireNonNull(modifier);
    }
    public void addStandaloneLayer(Consumer<OrePlayerRenderContext> layer) { standaloneLayers.add(Objects.requireNonNull(layer)); }
    public boolean removeStandaloneLayer(Consumer<OrePlayerRenderContext> layer) { return standaloneLayers.remove(layer); }

    /** Drops preview-owned model/entity caches, not appearances, borrowed textures or caller-owned handles. */
    public void releaseRenderResources() { player = null; standalone.clear(); }

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
        if (isStandalone()) startStandaloneSwing();
        else if (player != null) startSwing();
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
            player = new PreviewPlayer(minecraft.level, previewProfile(previewId, minecraft.player.getGameProfile()),
                    minecraft.player.getSkin(), appearance);
            // World-dependent resource-pack predicates see a sensible location, but no entity is spawned or ticked in the world.
            player.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
            player.xo = player.xOld = player.getX();
            player.yo = player.yOld = player.getY();
            player.zo = player.zOld = player.getZ();
            applyEquipment();
            player.crouch(crouching);
            if (usingHand != null) player.beginUse(usingHand);
            if (pendingSwing) startSwing();
        }
        return true;
    }

    static GameProfile previewProfile(UUID previewId, GameProfile source) {
        // UUID-keyed animation caches must not treat a UI draft as the actual local player.
        return new GameProfile(previewId, source.getName());
    }

    static PlayerSkin appearanceSkin(OrePlayerAppearance appearance) {
        return new PlayerSkin(appearance.skin(), null, null, null,
                appearance.model() == OrePlayerAppearance.Model.SLIM ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
    }

    private boolean renderStandalone() {
        if (appearance == null) return false;
        boolean worldAvailable = appearanceRendering != AppearanceRendering.STANDALONE && preparePlayer();
        return appearanceRendering.usesStandalone(worldAvailable);
    }

    private void startSwing() {
        player.animateSwing(swingHand);
        pendingSwing = false;
        swingCooldown = 12;
    }

    private void startStandaloneSwing() {
        standaloneSwingTicks = 0;
        pendingSwing = false;
        swingCooldown = 12;
    }

    OrePlayerPose standalonePose(float partialTick, float lookYaw, float lookPitch) {
        float swing = standaloneSwingTicks < 0 ? 0 : Math.min(1, (standaloneSwingTicks + partialTick) / 6.0F);
        HumanoidArm arm = swingHand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        return new OrePlayerPose(animationSeconds(), walking, crouching, lookYaw, lookPitch, arm, swing);
    }

    @Override
    protected void tickModel() {
        if (renderStandalone()) {
            if (swingCooldown > 0) swingCooldown--;
            if (standaloneSwingTicks >= 0 && ++standaloneSwingTicks >= 6) standaloneSwingTicks = -1;
            if (pendingSwing || swingLoop && swingCooldown == 0) startStandaloneSwing();
            return;
        }
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
        return appearance != null || preparePlayer();
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
        if (renderStandalone()) {
            standalone.render(graphics, Minecraft.getInstance().getEntityModels(), appearance,
                    standalonePose(partialTick, lookYaw, lookPitch), poseModifier, context -> {
                        for (var layer : List.copyOf(standaloneLayers)) {
                            graphics.pose().pushPose();
                            try { layer.accept(context); }
                            finally { graphics.pose().popPose(); }
                        }
                    });
            return;
        }
        player.yHeadRot = lookYaw;
        player.yHeadRotO = lookYaw;
        player.setXRot(lookPitch);
        player.xRotO = lookPitch;
        graphics.pose().translate(0, -player.getBbHeight() / 2.0F, 0);
        Lighting.setupForEntityInInventory();
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Quaternionf camera = new Quaternionf().rotationXYZ(getPitch() * Mth.DEG_TO_RAD,
                getYaw() * Mth.DEG_TO_RAD, getRoll() * Mth.DEG_TO_RAD).conjugate();
        try (var ignored = new OreEntityPreviewRenderScope((EntityRenderDispatcherAccessor) dispatcher, camera)) {
            // Enter the dispatcher as well as the player renderer: ETF/EMF establish their entity context here.
            try {
                RenderSystem.runAsFancy(() -> dispatcher.render(player, 0, 0, 0, 0, partialTick,
                        graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT));
            } finally { graphics.flush(); }
        }
    }

    private static final class PreviewPlayer extends RemotePlayer {
        private final PlayerTeam previewTeam;
        private final PlayerSkin accountSkin;
        private OrePlayerAppearance appearance;
        private PlayerSkin skin;

        private PreviewPlayer(ClientLevel level, GameProfile profile, PlayerSkin accountSkin, OrePlayerAppearance appearance) {
            super(level, profile);
            this.accountSkin = accountSkin;
            setAppearance(appearance);
            previewTeam = new Scoreboard().addPlayerTeam("oreui_preview");
            previewTeam.setNameTagVisibility(Team.Visibility.NEVER);
            setSilent(true);
            setOnGround(true);
            // Construction uses spawn rotation; the preview camera owns orientation instead.
            setYRot(0); yRotO = 0; yBodyRot = yBodyRotO = 0; yHeadRot = yHeadRotO = 0;
        }

        @Override public PlayerSkin getSkin() {
            return skin;
        }

        private void setAppearance(OrePlayerAppearance appearance) {
            this.appearance = appearance;
            skin = appearance == null ? accountSkin : appearanceSkin(appearance);
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
            return appearance == null || appearance.parts().contains(part);
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
