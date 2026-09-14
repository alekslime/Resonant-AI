package com.resonant.app.gestures

/**
 * The three conceptual interaction zones described in the Resonant interaction model.
 * Zones are always computed relative to the current size of the gesture surface —
 * never from absolute/fixed coordinates — so the model stays valid across
 * orientation and screen-size changes.
 */
enum class InteractionZone {
    LEFT_EDGE,
    RIGHT_EDGE,
    CENTER
}

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/**
 * All raw gestures the detector can emit. Screens interpret these according to the
 * *same* grammar everywhere (see GestureManager docs) — no screen invents new
 * meanings for a raw gesture type, they just react differently to it in context.
 */
sealed class ResonantGesture {
    data class Tap(val zone: InteractionZone) : ResonantGesture()
    data class DoubleTap(val zone: InteractionZone) : ResonantGesture()
    data class LongPress(val zone: InteractionZone) : ResonantGesture()
    data class Swipe(val zone: InteractionZone, val direction: SwipeDirection) : ResonantGesture()

    // Left-edge "hold to adjust speed" sub-gesture stream.
    object HoldStart : ResonantGesture()
    object HoldSpeedUp : ResonantGesture()
    object HoldSpeedDown : ResonantGesture()
    object HoldEnd : ResonantGesture()

    // Universal multi-finger gestures — global, zone-independent.
    object ThreeFingerTap : ResonantGesture()
    object ThreeFingerHold : ResonantGesture()
}
