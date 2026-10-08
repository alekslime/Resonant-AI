package com.resonant.app.gestures

import com.resonant.app.haptics.HapticPattern

/**
 * What the practice area says back for a gesture. [spoken] is the whole sentence,
 * [title] and [meaning] are the same thing as short text for the card.
 */
data class GestureExplanation(
    val title: String,
    val meaning: String,
    val spoken: String,
    val haptic: HapticPattern?
)

private fun said(title: String, did: String, meaning: String, haptic: HapticPattern?) =
    GestureExplanation(title, meaning, "You just $did, which means $meaning.", haptic)

/**
 * Plain-English answer for a gesture, or null for the mechanical ones (hold start and
 * end, release after a centre hold) that are never something the user did on purpose.
 *
 * Vertical swipes are described by where the finger went. ResonantGesture reports the
 * meaning (UP = next), so the words here are the opposite of the enum names.
 */
fun explain(gesture: ResonantGesture): GestureExplanation? = when (gesture) {
    is ResonantGesture.Swipe -> when {
        gesture.zone != InteractionZone.CENTER -> GestureExplanation(
            title = "Swipe on an edge",
            meaning = "nothing",
            spoken = "That swipe started on the edge of the screen, so it does nothing. Swipes work in the middle.",
            haptic = HapticPattern.ERROR
        )
        gesture.direction == SwipeDirection.UP ->
            said("Swipe down", "swiped down with two fingers", "next", HapticPattern.NEXT)
        gesture.direction == SwipeDirection.DOWN ->
            said("Swipe up", "swiped up with two fingers", "previous", HapticPattern.PREVIOUS)
        gesture.direction == SwipeDirection.RIGHT ->
            said("Swipe right", "swiped right", "continue", HapticPattern.SECTION_CHANGE)
        else ->
            said("Swipe left", "swiped left", "back", HapticPattern.BACK)
    }
    is ResonantGesture.Tap -> when (gesture.zone) {
        InteractionZone.CENTER ->
            said("Tap the middle", "tapped the middle", "select. It opens or confirms whatever you are on", HapticPattern.SELECT)
        InteractionZone.LEFT_EDGE ->
            said("Tap the left edge", "tapped the left edge", "pause or resume the speech", HapticPattern.CONFIRM)
        InteractionZone.RIGHT_EDGE -> GestureExplanation(
            title = "Tap the right edge",
            meaning = "nothing",
            spoken = "That was a tap on the right edge. It does nothing, on purpose, so you can't set something off by accident.",
            haptic = null
        )
    }
    is ResonantGesture.DoubleTap ->
        said("Double-tap the left edge", "double-tapped the left edge", "repeat what was just said", HapticPattern.CONFIRM)
    is ResonantGesture.LongPress -> when (gesture.zone) {
        InteractionZone.RIGHT_EDGE ->
            said("Hold the right edge", "held the right edge", "back. On other screens it takes you out of the screen", HapticPattern.BACK)
        InteractionZone.CENTER ->
            said("Hold the middle", "held the middle", "hold to talk in Chat, if you turned that on in Settings", null)
        InteractionZone.LEFT_EDGE -> null
    }
    ResonantGesture.HoldSpeedUp ->
        said("Drag up on the right edge", "dragged up on the right edge", "faster speech", HapticPattern.SPEED_UP)
    ResonantGesture.HoldSpeedDown ->
        said("Drag down on the right edge", "dragged down on the right edge", "slower speech", HapticPattern.SPEED_DOWN)
    ResonantGesture.ThreeFingerTap ->
        said("Three-finger tap", "tapped with three fingers", "repeat what was just said", HapticPattern.CONFIRM)
    ResonantGesture.ThreeFingerHold ->
        said("Three-finger hold", "held three fingers", "where am I. On other screens I tell you the screen and your place in it", null)
    is ResonantGesture.LongPressEnd, ResonantGesture.HoldStart, ResonantGesture.HoldEnd -> null
}
