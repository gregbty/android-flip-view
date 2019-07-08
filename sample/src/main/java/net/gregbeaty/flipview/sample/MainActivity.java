package net.gregbeaty.flipview.sample;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import net.gregbeaty.flipview.FlipView;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private TextView position;
    private TextView totalItems;
    private TextView distanceText;
    private TextView angleText;
    private FlipView view;
    private SampleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.plant(new Timber.DebugTree());

        setContentView(R.layout.activity_main);

        position = findViewById(R.id.main_position);
        totalItems = findViewById(R.id.main_total_items);
        distanceText = findViewById(R.id.main_flip_distance);
        angleText = findViewById(R.id.main_flip_angle);

        adapter = new SampleAdapter();

        for (int i = 0; i < 10; i++) {
            adapter.addItem();
        }

        view = findViewById(R.id.main_flip_view);

        view.addOnPositionChangeListener(new FlipView.OnPositionChangeListener() {
            @Override
            public void onPositionChange(FlipView flipView, int position) {
                refreshDetails();
            }
        });

        view.addOnScrollListener(new FlipView.OnScrollListener() {
            @Override
            public void onScrolled(FlipView flipView, int dx, int dy) {
                refreshDetails();
            }

            @Override
            public void onScrollStateChanged(FlipView flipView, int newState) {
                if (newState != FlipView.SCROLL_STATE_IDLE) {
                    return;
                }

                refreshDetails();
            }
        });

        view.setAdapter(adapter);
    }

    private void refreshDetails() {
        position.setText(String.format("Position: %s", view.getPosition()));
        totalItems.setText(String.format("Total Item: %s", adapter.getItemCount()));
        distanceText.setText(String.format("Distance: %s", view.getScrollDistance()));
        angleText.setText(String.format("Angle: %s", view.getAngle()));

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem deleteMenuItem = menu.findItem(R.id.delete_item);
        deleteMenuItem.setVisible(view.getPosition() != FlipView.NO_POSITION);

        MenuItem scrollToBeginningItem = menu.findItem(R.id.scroll_to_beginning);
        scrollToBeginningItem.setVisible(view.getPosition() > 0);

        MenuItem scrollToEndItem = menu.findItem(R.id.scroll_to_end);
        scrollToEndItem.setVisible(view.getPosition() < adapter.getItemCount() - 1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_item:
                adapter.addItem();
                return true;
            case R.id.delete_item:
                adapter.removeItem(view.getPosition());
                return true;
            case R.id.scroll_to_beginning:
                view.smoothScrollToPosition(0);
                return true;
            case R.id.scroll_to_end:
                view.smoothScrollToPosition(adapter.getItemCount() - 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
