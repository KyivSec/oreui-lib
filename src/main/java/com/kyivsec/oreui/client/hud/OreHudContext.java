package com.kyivsec.oreui.client.hud;

import java.util.Objects;

/** Immutable visibility/input context, independent of Minecraft for deterministic policy tests. */
public record OreHudContext(boolean worldAvailable, boolean screenOpen, boolean hideGui,
                            boolean spectator, boolean windowActive, boolean overlayOpen, OreHudScreen screen) {
    public OreHudContext {
        Objects.requireNonNull(screen);
        if (screenOpen != (screen != OreHudScreen.NONE)) throw new IllegalArgumentException("Screen category must match screenOpen");
    }

    /** Compatibility constructor: an unspecified open screen is classified as OTHER. */
    public OreHudContext(boolean worldAvailable, boolean screenOpen, boolean hideGui,
                         boolean spectator, boolean windowActive, boolean overlayOpen) {
        this(worldAvailable, screenOpen, hideGui, spectator, windowActive, overlayOpen,
                screenOpen ? OreHudScreen.OTHER : OreHudScreen.NONE);
    }

    public OreHudContext(boolean worldAvailable, OreHudScreen screen, boolean hideGui,
                         boolean spectator, boolean windowActive, boolean overlayOpen) {
        this(worldAvailable, screen != OreHudScreen.NONE, hideGui, spectator, windowActive, overlayOpen, screen);
    }
}
