package com.gmail.station;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

public class MyTouchEvent extends View {

    public MyTouchEvent(Context context) {
        super(context);
    }

    public void collapseKeyboard(View view) {
        view.setOnTouchListener((view1, motionEvent) -> {
            view1.performClick();
            UIUtil.hideKeyboard((Activity) getContext());
            return false;
        });
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
