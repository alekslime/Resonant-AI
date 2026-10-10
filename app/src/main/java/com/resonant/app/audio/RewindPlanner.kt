package com.resonant.app.audio

/**
 * Where to resume when the listener rewinds N seconds. Pure, so it is unit tested.
 *
 * What was heard is a list of segments, oldest first: each earlier unit up to the point it was
 * left, then the current unit up to where it is playing now. Rewinding walks back through that
 * list, so a rewind can cross from one unit into the one before it.
 */
internal object RewindPlanner {

    class Segment(val sampleRate: Int, val frames: Int)

    class Position(val segment: Int, val frame: Int)

    /**
     * The segment and frame that is [seconds] before the end of [heard]. If less than that was
     * heard, the very start of what was heard. Null when nothing was heard at all.
     */
    fun locate(seconds: Float, heard: List<Segment>): Position? {
        if (seconds <= 0f || heard.none { it.frames > 0 }) return null
        var remaining = seconds
        var earliest = 0
        for (i in heard.indices.reversed()) {
            val s = heard[i]
            if (s.frames <= 0) continue
            val available = s.frames.toFloat() / s.sampleRate
            if (available >= remaining) {
                return Position(i, (s.frames - (remaining * s.sampleRate).toInt()).coerceIn(0, s.frames))
            }
            remaining -= available
            earliest = i
        }
        return Position(earliest, 0)
    }
}
