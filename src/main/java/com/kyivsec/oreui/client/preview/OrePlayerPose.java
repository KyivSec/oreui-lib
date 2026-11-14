package com.kyivsec.oreui.client.preview;

import net.minecraft.world.entity.HumanoidArm;

/** Frame-local, entity-free pose inputs. Time and swing interpolation come from the preview's UI clock. */
public record OrePlayerPose(float seconds, boolean walking, boolean crouching, float lookYaw, float lookPitch,
                            HumanoidArm swingArm, float swingProgress) {}
