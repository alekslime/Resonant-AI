package com.resonant.app.audio

/**
 * Where to carry on after a pause. Pure, so it is unit tested.
 *
 * Returns the frame to play from, or null when the unit should simply start again: the audio
 * kept is for another unit, nothing was heard yet, or it had already played to the end.
 */
internal object ResumePlanner {

    fun startFrame(heardIndex: Int, currentIndex: Int, heardFrames: Int, totalFrames: Int): Int? =
        if (heardIndex != currentIndex || heardFrames <= 0 || heardFrames >= totalFrames) null else heardFrames
}
