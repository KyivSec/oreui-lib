package com.kyivsec.oreui.client.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePreviewAnimationClockTest {
    @Test
    void advancesSmoothlyAt60AndHigherFpsWithoutAnyWorldPartialTick() {
        for (int fps : new int[] {60, 120, 144, 240}) {
            OrePreviewAnimationClock clock = new OrePreviewAnimationClock();
            clock.advance(0, true);
            int ticks = 0;
            float previous = 0;
            for (int frame = 1; frame <= fps; frame++) {
                var step = clock.advance(frame * 1_000_000_000L / fps, true);
                ticks += step.ticks();
                float position = ticks + step.partialTick();
                assertTrue(position > previous, "Every frame must advance at " + fps + " FPS");
                assertEquals(frame * 20.0 / fps, position, 1e-5);
                assertTrue(step.partialTick() >= 0 && step.partialTick() < 1);
                previous = position;
            }
            assertEquals(20, ticks);
        }
    }

    @Test
    void fixedTicksAndRenderSamplesShareOneTimelineWithoutDoubleAdvancement() {
        OrePreviewAnimationClock clock = new OrePreviewAnimationClock();
        clock.advance(0, true);
        int ticks = 0;
        for (long now = 1_000_000; now <= 1_000_000_000L; now += 1_000_000) {
            var step = clock.advance(now, true);
            ticks += step.ticks();
            if (now % 50_000_000 == 0) {
                var duplicate = clock.advance(now, true);
                assertEquals(0, duplicate.ticks());
                assertEquals(step.partialTick(), duplicate.partialTick());
            }
        }
        assertEquals(20, ticks);
    }

    @Test
    void hiddenOrPausedTimeDoesNotAccumulateAndResumingKeepsTheSameFraction() {
        OrePreviewAnimationClock clock = new OrePreviewAnimationClock();
        clock.advance(0, true);
        assertEquals(0.5F, clock.advance(25_000_000, true).partialTick());
        var pause = clock.advance(25_000_000, false);
        assertEquals(0.5F, pause.partialTick());
        assertEquals(0, clock.advance(5_000_000_000L, false).ticks());
        var resume = clock.advance(6_000_000_000L, true);
        assertEquals(0, resume.ticks());
        assertEquals(0.5F, resume.partialTick());
        var next = clock.advance(6_025_000_000L, true);
        assertEquals(1, next.ticks());
        assertEquals(0, next.partialTick());
    }

    @Test
    void longBackgroundGapsDoNotBurstThroughOldAnimationTicks() {
        OrePreviewAnimationClock clock = new OrePreviewAnimationClock();
        clock.advance(0, true);
        clock.advance(25_000_000, true);
        var stalled = clock.advance(10_000_000_000L, true);
        assertEquals(0, stalled.ticks());
        assertEquals(0.5F, stalled.partialTick());
        var next = clock.advance(10_025_000_000L, true);
        assertEquals(1, next.ticks());
        assertEquals(0, next.partialTick());
    }

    @Test
    void shortDelaysHaveBoundedCatchupAndNegativeClockDeltasNeverTick() {
        OrePreviewAnimationClock clock = new OrePreviewAnimationClock();
        clock.advance(0, true);
        var delayed = clock.advance(OrePreviewAnimationClock.MAX_GAP_NANOS, true);
        assertEquals(5, delayed.ticks());
        assertEquals(0, delayed.partialTick());
        assertEquals(0, clock.advance(OrePreviewAnimationClock.MAX_GAP_NANOS - 1, true).ticks());
    }
}
