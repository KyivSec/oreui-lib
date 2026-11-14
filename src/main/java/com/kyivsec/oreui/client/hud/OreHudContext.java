package com.kyivsec.oreui.client.hud;

/** Immutable visibility/input context, independent of Minecraft for deterministic policy tests. */
public record OreHudContext(boolean worldAvailable, boolean screenOpen, boolean hideGui,
                            boolean spectator, boolean windowActive, boolean overlayOpen) {}
