package com.example.searchengine.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import com.example.searchengine.utils.Constants;

public class IranYekanButton extends AppCompatButton {

    public IranYekanButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public IranYekanButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IranYekanButton(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), Constants.APP_FONT);
        setTypeface(tf, Typeface.NORMAL);
    }
}
