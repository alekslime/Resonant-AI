package com.resonant.app.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class SpringFeelTest {

    @Test
    fun the_springs_give_softly_and_never_wobble() {
        // Between "a hair of overshoot" and "no overshoot at all"; below 0.4 would read as bouncing.
        assertTrue(SpringFeel.PRESS_DAMPING in 0.4f..0.9f)
        assertTrue(SpringFeel.FOCUS_DAMPING in 0.4f..0.9f)
    }

    @Test
    fun the_springs_are_stiff_enough_to_settle_quickly() {
        assertTrue(SpringFeel.PRESS_STIFFNESS >= 200f)
        assertTrue(SpringFeel.FOCUS_STIFFNESS >= 200f)
    }
}
