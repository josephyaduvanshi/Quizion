package com.shaivites.quizion.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class TypeWriter extends AppCompatTextView {

    private CharSequence mText;
    private int mIndex;
    private long mDelay = 50; // Default delay in ms

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mCharacterAdder = new Runnable() {
        @Override
        public void run() {
            setText(mText.subSequence(0, mIndex++));
            if (mIndex <= mText.length()) {
                mHandler.postDelayed(this, mDelay);
            }
        }
    };

    public TypeWriter(Context context) {
        super(context);
    }

    public TypeWriter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TypeWriter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void animateText(CharSequence text) {
        mText = text;
        mIndex = 0;
        setText("");
        mHandler.removeCallbacks(mCharacterAdder);
        mHandler.postDelayed(mCharacterAdder, mDelay);
    }

    public void setCharacterDelay(long millis) {
        mDelay = millis;
    }
}
