package com.resonant.app.gestures

/** What a list or menu screen should do for a gesture. The screen decides how, this decides which. */
sealed interface ListAction {
    object Next : ListAction
    object Previous : ListAction
    /** Act on the focused item. */
    object Activate : ListAction
    object TogglePause : ListAction
    object Repeat : ListAction
    object Back : ListAction
    object WhereAmI : ListAction
    object FasterSpeech : ListAction
    object SlowerSpeech : ListAction
    object None : ListAction
}

/**
 * The shared grammar for lists and menus (Home, Lessons, Quiz list, Settings, Profile), in one place so it can be
 * tested. A vertical swipe is already "inverted" by the detector: a finger moving down arrives as UP and means next.
 * Hold-to-speed start and end, and the release of a centre long press, are the detector's own business: no action.
 */
object ListGestures {
    fun resolve(gesture: ResonantGesture): ListAction = when (gesture) {
        is ResonantGesture.Swipe ->
            if (gesture.zone != InteractionZone.CENTER) ListAction.None
            else when (gesture.direction) {
                SwipeDirection.UP -> ListAction.Next
                SwipeDirection.DOWN -> ListAction.Previous
                SwipeDirection.LEFT, SwipeDirection.RIGHT -> ListAction.None
            }
        is ResonantGesture.Tap -> when (gesture.zone) {
            InteractionZone.CENTER -> ListAction.Activate
            InteractionZone.LEFT_EDGE -> ListAction.TogglePause
            InteractionZone.RIGHT_EDGE -> ListAction.None   // a dead zone, so a stray thumb does nothing
        }
        is ResonantGesture.DoubleTap ->
            if (gesture.zone == InteractionZone.LEFT_EDGE) ListAction.Repeat else ListAction.None
        is ResonantGesture.LongPress ->
            if (gesture.zone == InteractionZone.RIGHT_EDGE) ListAction.Back else ListAction.None
        ResonantGesture.ThreeFingerTap -> ListAction.Repeat
        ResonantGesture.ThreeFingerHold -> ListAction.WhereAmI
        ResonantGesture.HoldSpeedUp -> ListAction.FasterSpeech
        ResonantGesture.HoldSpeedDown -> ListAction.SlowerSpeech
        is ResonantGesture.LongPressEnd, ResonantGesture.HoldStart, ResonantGesture.HoldEnd -> ListAction.None
    }
}
