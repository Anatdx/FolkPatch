package me.bmax.apatch.ui.theme.tokens

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens, so a press, an expand and a fade feel the same wherever they happen. Only the
 * duration and the spring constants are decided here; the easing stays Compose's default.
 */
object FolkMotion {
    /** How far a pressed surface shrinks. */
    const val PressedScale = 0.97f

    /** The spring a pressed surface returns with: a small overshoot that reads as a response. */
    val PressScale: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 700f)

    /** Expanding and collapsing: settles without overshooting, so a growing group does not bounce
     *  past its final height. */
    fun <T> smoothSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 250f)

    /** Fading something in or out. */
    val FadeInOut: FiniteAnimationSpec<Float> = tween(durationMillis = 200)

    /** Turning an arrow around to point the other way. */
    val IconRotation: FiniteAnimationSpec<Float> = tween(durationMillis = 300)
}
