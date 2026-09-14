package com.resonant.app.gestures

enum class InteractionZone {
    LEFT_EDGE,
    RIGHT_EDGE,
    CENTER
}

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

sealed class ResonantGesture {
    data class Tap(val zone: InteractionZone) : ResonantGesture()
    data class DoubleTap(val zone: InteractionZone) : ResonantGesture()
    data class LongPress(val zone: InteractionZone) : ResonantGesture()
    data class Swipe(val zone: InteractionZone, val direction: SwipeDirection) : ResonantGesture()

    // Right-edge hold-to-adjust-speed
    object HoldStart : ResonantGesture()
    object HoldSpeedUp : ResonantGesture()
    object HoldSpeedDown : ResonantGesture()
    object HoldEnd : ResonantGesture()

    object ThreeFingerTap : ResonantGesture()
    object ThreeFingerHold : ResonantGesture()
}
