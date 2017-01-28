package net.gregbeaty.flipview.sample;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {
    private ArrayList<Long> items;
    private long mItemIdCount;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView text;

        public ViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.list_item_text);
        }
    }

    public SampleAdapter() {
        items = new ArrayList<>();
        setHasStableIds(true);
    }

    @Override
    public SampleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SampleAdapter.ViewHolder holder, int position) {
        holder.text.setText(String.format(Locale.getDefault(), "%d", items.get(position)));
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
        items.add(++mItemIdCount);
        notifyItemInserted(items.size() - 1);
    }
}
