package net.gregbeaty.flipview.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {
    private int mCount;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView text;

        public ViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.list_item_text);
        }
    }

    @Override
    public SampleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SampleAdapter.ViewHolder holder, int position) {
        holder.text.setText(String.format(Locale.getDefault(), "%d", position + 1));
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    public void setItemCount(int count) {
        mCount = count;
        notifyDataSetChanged();
    }
}
