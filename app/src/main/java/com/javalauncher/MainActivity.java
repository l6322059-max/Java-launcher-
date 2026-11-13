package com.javalauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Java Launcher: the home screen. */
public class MainActivity extends Activity {
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final List<AppEntry> allApps = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppAdapter adapter;
    private GridView grid;
    private EditText search;
    private TextView greetingView;
    private TextView clockView;
    private TextView dateView;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60000 - (now % 60000) + 50);
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { loadApps(); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        greetingView = findViewById(R.id.greeting);
        clockView = findViewById(R.id.clock);
        dateView = findViewById(R.id.date);
        search = findViewById(R.id.search);
        grid = findViewById(R.id.grid);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(16), insets.getSystemWindowInsetTop() + dp(24),
                    dp(16), insets.getSystemWindowInsetBottom() + dp(8));
            return insets;
        });

        adapter = new AppAdapter(this);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> launch(adapter.getItem(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            openAppInfo(adapter.getItem(position));
            return true;
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (adapter.getCount() > 0) launch(adapter.getItem(0));
            return true;
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        updateClock();
        loadApps();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }
    @Override protected void onDestroy() {
        try { unregisterReceiver(packageReceiver); } catch (IllegalArgumentException ignored) {}
        loader.shutdown();
        super.onDestroy();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            clearSearch();
            grid.smoothScrollToPosition(0);
        }
    }
    @Override public void onBackPressed() {
        if (search.getText().length() > 0) clearSearch();
        else {
            hideKeyboard();
            search.clearFocus();
            grid.smoothScrollToPosition(0);
        }
    }

    private void loadApps() {
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        try {
            loader.execute(() -> {
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> infos = pm.queryIntentActivities(query, 0);
                List<AppEntry> list = new ArrayList<>(infos.size());
                for (ResolveInfo info : infos) {
                    ActivityInfo ai = info.activityInfo;
                    if (self.equals(ai.packageName)) continue;
                    list.add(new AppEntry(String.valueOf(info.loadLabel(pm)), ai.packageName,
                            new ComponentName(ai.packageName, ai.name), info.loadIcon(pm)));
                }
                Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
                runOnUiThread(() -> {
                    allApps.clear();
                    allApps.addAll(list);
                    applyFilter();
                });
            });
        } catch (RejectedExecutionException ignored) {}
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        if (q.isEmpty()) { adapter.setItems(allApps); return; }
        List<AppEntry> starts = new ArrayList<>();
        List<AppEntry> contains = new ArrayList<>();
        for (AppEntry entry : allApps) {
            if (entry.labelLower.startsWith(q)) starts.add(entry);
            else if (entry.labelLower.contains(q)) contains.add(entry);
        }
        starts.addAll(contains);
        adapter.setItems(starts);
    }
    private void launch(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(intent); clearSearch(); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
        }
    }
    private void openAppInfo(AppEntry app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
        }
    }
    private void clearSearch() { search.setText(""); search.clearFocus(); hideKeyboard(); }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }
    private void updateClock() {
        Date now = new Date();
        Locale locale = Locale.getDefault();
        String timePattern = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        clockView.setText(new SimpleDateFormat(timePattern, locale).format(now));
        String datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd");
        dateView.setText(new SimpleDateFormat(datePattern, locale).format(now));
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greeting = hour < 5 || hour >= 22 ? R.string.good_night
                : hour < 12 ? R.string.good_morning
                : hour < 17 ? R.string.good_afternoon : R.string.good_evening;
        greetingView.setText(greeting);
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
