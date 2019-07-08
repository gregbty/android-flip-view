package net.gregbeaty.flipview.sample;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import timber.log.Timber;

public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {
    private final Random random = new Random();
    private final ArrayList<Integer> colors;
    private final ArrayList<Long> items;
    private long itemIdCount;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView text;

        public ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.list_item_text);
        }
    }

    public SampleAdapter() {
        colors = new ArrayList<>();
        items = new ArrayList<>();
        setHasStableIds(true);
    }

    @Override
    public SampleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SampleAdapter.ViewHolder holder, int position) {
        Timber.d("onBindViewHolder: %d", position);

        holder.text.setText(String.format(Locale.getDefault(), "%d", items.get(position)));
        holder.itemView.setBackgroundColor(colors.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Timber.d("onViewRecycled");

        holder.text.setText("Unbound");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position);
    }

    public void addItem() {
        items.add(++itemIdCount);
        colors.add(Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        items.remove(position);
        colors.remove(position);
        notifyItemRemoved(position);
    }
}