package com.javalauncher;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import java.util.Locale;

/** One launchable app in the grid. */
final class AppEntry {
    final String label;
    final String labelLower;
    final String packageName;
    final ComponentName component;
    final Drawable icon;

    AppEntry(String label, String packageName, ComponentName component, Drawable icon) {
        this.label = label;
        this.labelLower = label.toLowerCase(Locale.getDefault());
        this.packageName = packageName;
        this.component = component;
        this.icon = icon;
    }
}
