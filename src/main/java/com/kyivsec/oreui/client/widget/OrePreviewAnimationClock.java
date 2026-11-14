package com.kyivsec.oreui.client.widget;

/** Fixed-step animation timing independent of the world's paused/frozen partial tick. */
final class OrePreviewAnimationClock {
    static final long TICK_NANOS = 50_000_000L;
    static final long MAX_GAP_NANOS = 250_000_000L;

    record Step(int ticks, float partialTick) {}

    private long lastNanos;
    private long remainderNanos;
    private boolean initialized;
    private boolean wasActive;

    Step advance(long nowNanos, boolean active) {
        if (!initialized) {
            initialized = true;
            lastNanos = nowNanos;
            wasActive = active;
            return new Step(0, 0);
        }
        long elapsed = nowNanos - lastNanos;
        lastNanos = nowNanos;
        int ticks = 0;
        // Ignore long stalls/background gaps rather than bursting through old animations on return.
        if (active && wasActive && elapsed > 0 && elapsed <= MAX_GAP_NANOS) {
            remainderNanos += elapsed;
            ticks = (int) (remainderNanos / TICK_NANOS);
            remainderNanos %= TICK_NANOS;
        }
        wasActive = active;
        return new Step(ticks, remainderNanos / (float) TICK_NANOS);
    }
}
