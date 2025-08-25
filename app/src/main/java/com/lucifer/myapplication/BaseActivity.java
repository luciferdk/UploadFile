package com.lucifer.myapplication;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Automatically apply to all TextViews
        applyDarkModeToAllTextViews();
    }

    private void applyDarkModeToAllTextViews() {
        setTextColorsRecursively(getWindow().getDecorView());
    }

    private void setTextColorsRecursively(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            int color = (nightMode == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;
            textView.setTextColor(color);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setTextColorsRecursively(group.getChildAt(i));
            }
        }
    }
}

