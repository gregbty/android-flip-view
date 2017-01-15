package net.gregbeaty.flipview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import net.gregbeaty.flipview.FlipLayoutManager;
import net.gregbeaty.flipview.FlipView;

public class MainActivity extends AppCompatActivity {
    private TextView mCurrentPosition;
    private TextView mDistanceText;
    private TextView mAngleText;
    private FlipView mFlipView;
    private FlipLayoutManager mLayoutManager;
    private SampleAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mCurrentPosition = (TextView) findViewById(R.id.main_current_position);
        mDistanceText = (TextView) findViewById(R.id.main_flip_distance);
        mAngleText = (TextView) findViewById(R.id.main_flip_angle);

        mLayoutManager = new FlipLayoutManager(this, FlipLayoutManager.VERTICAL);
        mAdapter = new SampleAdapter();
        mAdapter.setItemCount(10);

        mFlipView = (FlipView) findViewById(R.id.main_flip_view);
        mFlipView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private void refreshDetails() {
                mCurrentPosition.setText(String.format("Position: %s", mLayoutManager.getCurrentPosition()));
                mDistanceText.setText(String.format("Distance: %s", mLayoutManager.getScrollDistance()));
                mAngleText.setText(String.format("Angle: %s", mLayoutManager.getFlipAngle()));
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

        mFlipView.setLayoutManager(mLayoutManager);
        mFlipView.setAdapter(mAdapter);
    }
}