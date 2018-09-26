package tw.inspect.scalechartsample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.inspect.scalechart.OnLineSegmentClickedListener;
import tw.inspect.scalechart.ScaleChartView;

public class MainActivity extends Activity {
    private ScaleChartView scaleChartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scaleChartView = findViewById(R.id.scale_chart_view);


        List<Event> eventList = new ArrayList<>();

        SimpleBuilder[] simpleBuilders = {
                new SimpleBuilder(0, 30, 0, 45, "event1"),
                new SimpleBuilder(2, 1, 2, 2, "event2"),
                new SimpleBuilder(2, 3, 2, 4, "event3"),
                new SimpleBuilder(2, 5, 2, 6, "event4"),
                new SimpleBuilder(2, 7, 2, 8, "event5"),
                new SimpleBuilder(3, 30, 4, 45, "event6"),
                new SimpleBuilder(6, 0, 7, 15, "event7"),
                new SimpleBuilder(10, 0, 12, 15, "event8"),
                new SimpleBuilder(13, 0, 13, 15, "event9"),
                new SimpleBuilder(14, 30, 16, 15, "event10"),
                new SimpleBuilder(18, 30, 19, 0, "event11"),
                new SimpleBuilder(21, 30, 23, 0, "event12")
        };

        for (SimpleBuilder simpleBuilder : simpleBuilders) {
            eventList.add(simpleBuilder.event);
        }

        scaleChartView.setSortedLineSegments(eventList);
        scaleChartView.setOnLineSegmentClickedListener(new OnLineSegmentClickedListener() {

            @Override
            public void onLineSegmentClicked(@NonNull final ScaleChartView scaleChartView, int eventIndex) {
                Toast.makeText(
                        MainActivity.this,
                        "eventTitle: " + scaleChartView.getSortedLineSegments().get(eventIndex).getText(),
                        Toast.LENGTH_SHORT
                ).show();
            }

        });

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                scaleChartView.scrollToUnit(scaleChartView.getCurrentUnit() + 1);
            }

        });
        findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                scaleChartView.scrollToUnit(scaleChartView.getCurrentUnit() - 1);
            }

        });
        scaleChartView.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                scaleChartView.setCurrentUnit(24 * 60 * 60 / 2);
                scaleChartView.setResolution((scaleChartView.getWidth() - 20) / getResources().getDisplayMetrics().xdpi / (24 * 60 * 60));
                scaleChartView.setScrollable(false);
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });

        //testHandler.postDelayed(testRunnable, 10);
    }

    private double testInitial = 3.0;
    private final Handler testHandler = new Handler();
    private final Runnable testRunnable = new Runnable() {
        @Override
        public void run() {
            scaleChartView.scrollToUnit(testInitial++);
            testHandler.postDelayed(testRunnable, 10);
        }
    };

    private static class SimpleBuilder {
        @NonNull
        final Event event;

        SimpleBuilder(int beginHour, int beginMinute, int endHour, int endMinute, @NonNull String title) {
            Calendar from = Calendar.getInstance();
            from.set(Calendar.HOUR_OF_DAY, beginHour);
            from.set(Calendar.MINUTE, beginMinute);
            Calendar to = Calendar.getInstance();
            to.set(Calendar.HOUR_OF_DAY, endHour);
            to.set(Calendar.MINUTE, endMinute);
            event = new Event(from, to, title);
        }
    }

}
