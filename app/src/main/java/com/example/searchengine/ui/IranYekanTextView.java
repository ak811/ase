package com.example.searchengine.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.example.searchengine.utils.Constants;

public class IranYekanTextView extends AppCompatTextView {

    public IranYekanTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public IranYekanTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IranYekanTextView(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), Constants.APP_FONT);
        setTypeface(tf, Typeface.NORMAL);
    }
}
