package com.example.morsedetector.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.example.morsedetector.R

class PlaybackButton : FrameLayout {
    companion object {
        private const val ANIMATION_DURATION = 150L
    }

    var currentState: State = State.PLAY
    var view: View? = null
    var stateListener: StateListener? = null

    var currentStateBuffer: ImageView? = null
    var previousStateBuffer: ImageView? = null


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        view = LayoutInflater.from(context).inflate(R.layout.layout_playback_button, this, false)
        this.addView(view)
        currentStateBuffer = view?.findViewById(R.id.ivCurrentState)
        previousStateBuffer = view?.findViewById(R.id.ivPreviousState)
        setState(State.PLAY)
    }

    fun setState(newState: State) {
//        val context = context ?: return
        val oldState = currentState
        currentState = newState
        stateListener?.onChanged(oldState, newState)

        val newStateDrawable = ContextCompat.getDrawable(context, newState.drawableResId)
        val oldStateDrawable = ContextCompat.getDrawable(context, oldState.drawableResId)

        currentStateBuffer?.setImageDrawable(newStateDrawable)
        previousStateBuffer?.setImageDrawable(oldStateDrawable)

        val fadeInAnimator =
            ObjectAnimator.ofFloat(currentStateBuffer, View.ALPHA, 0.1f, 0.2f, 1f).apply {
                duration = ANIMATION_DURATION
            }
        val fadeOutAnimator =
            ObjectAnimator.ofFloat(previousStateBuffer, View.ALPHA, 0.9f, 0.8f, 0f).apply {
                duration = ANIMATION_DURATION
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationEnd(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                    override fun onAnimationStart(p0: Animator?) {}
                })
            }
        fadeInAnimator.start()
        fadeOutAnimator.start()

        swapBuffers()
    }

    private fun swapBuffers() {
        val temp = currentStateBuffer
        currentStateBuffer = previousStateBuffer
        previousStateBuffer = temp
    }

    enum class State(val drawableResId: Int) {
        PLAY(R.drawable.ic_play),
        PAUSE(R.drawable.ic_pause),
        STOP(R.drawable.ic_stop)
    }

    interface StateListener {
        fun onChanged(previousState: State, newState: State)
    }
}
