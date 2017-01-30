package net.gregbeaty.flipview.sample;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {
    private final Random mRandom = new Random();
    private final ArrayList<Integer> mColors;
    private final ArrayList<Long> mItems;
    private long mItemIdCount;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView text;

        public ViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.list_item_text);
        }
    }

    public SampleAdapter() {
        mColors = new ArrayList<>();
        mItems = new ArrayList<>();
        setHasStableIds(true);
    }

    @Override
    public SampleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SampleAdapter.ViewHolder holder, int position) {
        holder.text.setText(String.format(Locale.getDefault(), "%d", mItems.get(position)));
        holder.itemView.setBackgroundColor(mColors.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position);
    }

    public void addItem() {
        mItems.add(++mItemIdCount);
        mColors.add(Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
        notifyItemInserted(mItems.size() - 1);
    }
}
