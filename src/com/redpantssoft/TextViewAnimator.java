package com.redpantssoft;

import android.view.animation.Animation;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Provides an animated TextView
 * <p/>
 * This class wraps a text view and provides animations for drawing
 * and clearing the text.  The Text can automatically clear after
 * a set period of milliseconds, are be manually cleared. The delay for
 * automatically clearing is implemented on a UI Timer.
 * <p/>
 * It also implements a queue of text strings if numerous setText requests
 * are made. A text string will remain visible until manually cleared, or
 * the display millisecond value expires.
 * <p/>
 *
 * @author Jeff clyne
 */
public class TextViewAnimator implements Animation.AnimationListener, Timer.TimerHandler {
    /**
     * TextView that is wrapped and animated
     */
    private final TextView textView;

    /**
     * Animation for initial display of text
     */
    private final Animation inAnim;

    /**
     * Animation to clean up the text
     */
    private final Animation outAnim;

    /**
     * Specifies the background color of the textView when text is displayed
     * This is mainly used when the textView is layered in a  FrameLayout.
     * The background color can make the text more readable
     */
    private int backgroundColor = 0x50000000;
    /**
     * Timer to handle automatic clearing of the animated text
     */
    private final Timer timer;

    /**
     * Flag that indicates whether or not an animation is active
     */
    private boolean active = false;

    /**
     * Class that represents a queued string to be displayed. It contains
     * the text string as well as the desired display time in milliseconds.
     */
    class TextDisplay {
        /**
         * Text string to be displayed
         */
        private final String mText;

        /**
         * Time to display string before auto cleanup
         */
        private final int mDisplayMillis;

        /**
         * Constructor for an auto cleanup item
         *
         * @param text          text string to display
         * @param displayMillis time to display the string in milliseconds
         */
        TextDisplay(String text, int displayMillis) {
            mText = text;
            mDisplayMillis = displayMillis;
        }

        /**
         * Accessor for Display Text
         *
         * @return text string to displayed for this item
         */
        public final String getText() {
            return mText;
        }

        /**
         * Accessor for Display time
         *
         * @return time to display the text before auto cleanup
         */
        public final int getDisplayMillis() {
            return mDisplayMillis;
        }
    }

    /**
     * Queue containing the TextDisplay objects to be displayed
     */
    private final Queue<TextDisplay> textQueue;

    /**
     * Constructor
     *
     * @param textView specifies an existing text view to wrap
     * @param inAnim   animation to be used to display the text in the view
     * @param outAnim  animation to be used to to clear the text in the view
     */
    public TextViewAnimator(TextView textView, Animation inAnim, Animation outAnim) {
        this.textView = textView;
        this.inAnim = inAnim;
        this.outAnim = outAnim;

        // Use the linked list implementation of Queue
        textQueue = new LinkedList<TextDisplay>();

        // This object acts as the listener for the animation events
        // as well as the timer events
        this.inAnim.setAnimationListener(this);
        this.outAnim.setAnimationListener(this);
        timer = new Timer(this);
    }

    /**
     * Displays text in the view
     * Text displayed with this method needs
     * to be manually cleared with {@link #clearText() clearText}
     *
     * @param text string to display
     * @see #setText(String text, int millis)
     */
    public void setText(String text) {
        textQueue.add(new TextDisplay(text,0));
        if (!active) {
            active = true;
            textView.startAnimation(inAnim);
            textView.invalidate();
        }
    }

    /**
     * Displays text in the view
     * Text displayed with this method will
     * automatically cleared after millis milliseconds
     *
     * @param text   string to display
     * @param millis milliseconds to display the text before clearing
     * @see #setText(String)
     */
    public void setText(String text, int millis) {
        textQueue.add(new TextDisplay(text, millis));
        if (!active) {
            active = true;
            textView.startAnimation(inAnim);
            textView.invalidate();
        }
    }

    /**
     * Clears the currently displayed text.
     * This can be used with a text item that is automatically cleared as well
     */
    public void clearText() {
        if (!active) return;

        timer.stop();
        textView.startAnimation(outAnim);
        textView.invalidate();
    }

    /**
     * Clears all text in the Queue as well as the currently displayed
     * string.
     */
    public void clear() {
        textQueue.clear();
        clearText();
    }

    /**
     * Getter for the background color
     *
     * @return  current background color
     */
    public final int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Setter for the background color
     *
     * @param backgroundColor new background color
     */
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }


    /**
     * Animation end handler.
     * When the out animation is completed, this will check the text
     * queue and start the display the next item, if one exists.
     * Regardless it will clear the text and background
     *
     * @param animation specifies which animation is ending
     */
    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == outAnim) {
            textView.setBackgroundColor(0);
            textView.setText("");
            if (!textQueue.isEmpty()) {
                textView.startAnimation(inAnim);
                textView.invalidate();
            } else
                active = false;
        }
    }

    /**
     * Animation Repeat Handler
     * N/A
     *
     * @param animation specifies which animation is repeating
     */
    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    /**
     * Animation Start Handler
     * For the  in animation, this will retrieve the text display item
     * from the queue
     *
     * @param animation specifies which animation is starting
     */
    @Override
    public void onAnimationStart(Animation animation) {
        if (animation == inAnim) {
            try {
                TextDisplay d = textQueue.remove();
                textView.setBackgroundColor(backgroundColor);
                textView.setText(d.getText());
                if (d.getDisplayMillis() > 0)
                    timer.start(d.getDisplayMillis());
            } catch (NoSuchElementException e) {
                animation.cancel();
            }
        }
    }

    /**
     * Auto clear timer handler
     * This just automates calling clearText
     *
     * @return boolean to indicate if the timer should re-armed
     */
    @Override
    public boolean HandleTimer() {
        clearText();
        return false;
    }
}
