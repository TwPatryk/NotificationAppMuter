package com.example.notificationappmuter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private final List<AppInfo> fullList;
    private List<AppInfo> filteredList;
    private OnSelectionChangedListener selectionListener;

    public AppAdapter(List<AppInfo> appList) {
        this.fullList = appList;
        this.filteredList = new ArrayList<>(appList);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            int count = 0;
            for (AppInfo app : fullList) {
                if (app.isSelected) count++;
            }
            selectionListener.onSelectionChanged(count);
        }
    }

    public void sortBySelection() {
        Collections.sort(fullList, (a, b) -> {
            if (a.isSelected != b.isSelected) {
                return a.isSelected ? -1 : 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });
        updateData();
    }

    public void filter(String query) {
        filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppInfo app : fullList) {
                if (app.name.toLowerCase().contains(lowerQuery) || 
                    app.packageName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateData() {
        this.filteredList = new ArrayList<>(fullList);
        notifyDataSetChanged();
    }

    public void selectFiltered(boolean select) {
        for (AppInfo app : filteredList) {
            app.isSelected = select;
        }
        notifySelectionChanged();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = filteredList.get(position);
        holder.tvAppName.setText(app.name);
        holder.ivAppIcon.setImageDrawable(app.icon);
        holder.cbSelected.setChecked(app.isSelected);

        holder.itemView.setOnClickListener(v -> {
            app.isSelected = !app.isSelected;
            holder.cbSelected.setChecked(app.isSelected);
            notifySelectionChanged();
        });
        
        holder.cbSelected.setOnClickListener(v -> {
            app.isSelected = holder.cbSelected.isChecked();
            notifySelectionChanged();
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        CheckBox cbSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            cbSelected = itemView.findViewById(R.id.cbSelected);
        }
    }
}