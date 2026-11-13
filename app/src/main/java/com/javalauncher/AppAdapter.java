package com.javalauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Feeds the app grid. */
final class AppAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<AppEntry> items = new ArrayList<>();
    AppAdapter(Context context) { inflater = LayoutInflater.from(context); }
    void setItems(List<AppEntry> newItems) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged();
    }
    @Override public int getCount() { return items.size(); }
    @Override public AppEntry getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new Holder();
            holder.icon = convertView.findViewById(R.id.icon);
            holder.label = convertView.findViewById(R.id.label);
            convertView.setTag(holder);
        } else holder = (Holder) convertView.getTag();
        AppEntry entry = items.get(position);
        holder.icon.setImageDrawable(entry.icon);
        holder.label.setText(entry.label);
        return convertView;
    }
    private static final class Holder { ImageView icon; TextView label; }
}
