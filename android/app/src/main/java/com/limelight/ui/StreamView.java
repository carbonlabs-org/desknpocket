package com.limelight.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import com.limelight.binding.input.StreamInputConnection;

public class StreamView extends SurfaceView {
    private double desiredAspectRatio;
    private InputCallbacks inputCallbacks;
    private StreamInputConnection.Sink imeSink;
    private StreamInputConnection imeConnection;
    private boolean shortcutMode;

    public void setImeSink(StreamInputConnection.Sink sink) { imeSink = sink; }

    public void resetImeContext() {
        if (imeConnection != null) imeConnection.retire();
        imeConnection = null;
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(this);
    }

    public void setShortcutMode(boolean enabled) {
        shortcutMode = enabled;
        resetImeContext();
    }

    @Override public boolean onCheckIsTextEditor() { return true; }

    @Override public InputConnection onCreateInputConnection(EditorInfo info) {
        if (imeSink == null) return null;
        if (imeConnection != null) imeConnection.retire();
        info.inputType = InputType.TYPE_CLASS_TEXT | (shortcutMode
                ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                : InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        info.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_NONE;
        if (android.os.Build.VERSION.SDK_INT >= 26)
            info.imeOptions |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        info.initialSelStart = info.initialSelEnd = 0;
        imeConnection = new StreamInputConnection(this, imeSink);
        return imeConnection;
    }

    public void setDesiredAspectRatio(double aspectRatio) {
        this.desiredAspectRatio = aspectRatio;
    }

    public void setInputCallbacks(InputCallbacks callbacks) {
        this.inputCallbacks = callbacks;
    }

    public StreamView(Context context) {
        super(context);
    }

    public StreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If no fixed aspect ratio has been provided, simply use the default onMeasure() behavior
        if (desiredAspectRatio == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Based on code from: https://www.buzzingandroid.com/2012/11/easy-measuring-of-custom-views-with-specific-aspect-ratio/
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight, measuredWidth;
        if (widthSize > heightSize * desiredAspectRatio) {
            measuredHeight = heightSize;
            measuredWidth = (int)(measuredHeight * desiredAspectRatio);
        } else {
            measuredWidth = widthSize;
            measuredHeight = (int)(measuredWidth / desiredAspectRatio);
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && inputCallbacks != null
                && inputCallbacks.handleImeBack(event)) return true;
        // This callbacks allows us to override dumb IME behavior like when
        // Samsung's default keyboard consumes Shift+Space.
        if (inputCallbacks != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (inputCallbacks.handleKeyDown(event)) {
                    return true;
                }
            }
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (inputCallbacks.handleKeyUp(event)) {
                    return true;
                }
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    public interface InputCallbacks {
        boolean handleImeBack(KeyEvent event);
        boolean handleKeyUp(KeyEvent event);
        boolean handleKeyDown(KeyEvent event);
    }
}
