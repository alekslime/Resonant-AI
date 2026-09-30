package com.resonant.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pose-math invariants for the animated dots. Pure Kotlin: runs on the JVM, no device needed. */
class DotsEngineTest {

    private val letters = listOf(
        listOf(1, 2, 3, 5), listOf(1, 5), listOf(2, 3, 4), listOf(1, 3, 5),
        listOf(1, 3, 4, 5), listOf(1), listOf(1, 3, 4, 5), listOf(2, 3, 4, 5)
    )

    @Test fun everyStateStaysFiniteAndOnScreen() {
        for (state in DotsState.values()) for (level in listOf<Float?>(null, 0f, .5f, 1f)) {
            val engine = DotsEngine(false)
            var t = 0f
            while (t < 30f) {
                for (v in engine.frame(state, t, level, false)) {
                    assertTrue("$state level=$level t=$t v=$v", !v.isNaN() && !v.isInfinite() && Math.abs(v) < 3000f)
                }
                t += 1f / 60f
            }
        }
    }

    @Test fun idleSpellsResonantInBraille() {
        letters.forEachIndexed { i, expected ->
            val pose = DotsEngine(false).frame(DotsState.Idle, idleLetterFormedAt(i) + .01f, null, false)
            val on = (0 until 6).filter { pose[it * 4 + 2] > .5f }.map { it + 1 }
            assertEquals("letter #$i", expected, on)
        }
    }

    @Test fun switchingStatesNeverJumps() {
        val engine = DotsEngine(false)
        var t = 0f
        var prev = engine.frame(DotsState.Idle, t, null, false).copyOf()
        var maxJump = 0f
        for (state in listOf(
            DotsState.Listening, DotsState.Transcribing, DotsState.Thinking,
            DotsState.Speaking, DotsState.Offline, DotsState.Idle
        )) repeat(120) {
            t += 1f / 60f
            val p = engine.frame(state, t, .5f, false)
            for (k in 0 until 24) maxJump = maxOf(maxJump, Math.abs(p[k] - prev[k]))
            prev = p.copyOf()
        }
        assertTrue("max per-frame delta $maxJump", maxJump < 60f)
    }

    @Test fun splashGrowsInFromNothing() {
        val pose = DotsEngine(true).frame(DotsState.Idle, .001f, null, false)
        assertTrue((0 until 6).all { pose[it * 4 + 2] < .05f })
    }

    @Test fun reduceMotionSkipsTheGrowIn() {
        assertTrue(DotsEngine(true).frame(DotsState.Idle, .001f, null, true)[2] > .9f)
    }
}
