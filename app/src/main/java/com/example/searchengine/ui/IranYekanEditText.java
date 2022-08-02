package com.example.searchengine.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.example.searchengine.utils.Constants;

public class IranYekanEditText extends AppCompatEditText {

    public IranYekanEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public IranYekanEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IranYekanEditText(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), Constants.APP_FONT);
        setTypeface(tf, Typeface.NORMAL);
    }
}
