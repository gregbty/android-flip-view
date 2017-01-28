package net.gregbeaty.flipview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import net.gregbeaty.flipview.FlipLayoutManager;
import net.gregbeaty.flipview.FlipView;

public class MainActivity extends AppCompatActivity {
    private TextView mPosition;
    private TextView mTotalItems;
    private TextView mDistanceText;
    private TextView mAngleText;
    private FlipView mView;
    private SampleAdapter mAdapter;
    private FlipLayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mPosition = (TextView) findViewById(R.id.main_position);
        mTotalItems = (TextView) findViewById(R.id.main_total_items);
        mDistanceText = (TextView) findViewById(R.id.main_flip_distance);
        mAngleText = (TextView) findViewById(R.id.main_flip_angle);

        mAdapter = new SampleAdapter();

        for (int i = 0; i < 10; i++) {
            mAdapter.addItem();
        }

        mLayoutManager = new FlipLayoutManager(this, FlipLayoutManager.VERTICAL);

        mView = (FlipView) findViewById(R.id.main_flip_view);

        mView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private void refreshDetails() {
                mPosition.setText(String.format("Position: %s", mLayoutManager.getCurrentPosition()));
                mTotalItems.setText(String.format("Total Item: %s", mAdapter.getItemCount()));
                mDistanceText.setText(String.format("Distance: %s", mLayoutManager.getScrollDistance()));
                mAngleText.setText(String.format("Angle: %s", mLayoutManager.getAngle()));
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                refreshDetails();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }

                refreshDetails();
            }
        });

        mView.setAdapter(mAdapter);
        mView.setLayoutManager(mLayoutManager);
    }
}