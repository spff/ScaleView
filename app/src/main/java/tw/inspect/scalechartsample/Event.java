package tw.inspect.scalechartsample;

import android.support.annotation.NonNull;

import java.util.Calendar;

import tw.inspect.scalechart.LineSegment;

public class Event implements LineSegment {
    @NonNull
    public final int eventTimeStart;
    @NonNull
    public final int eventTimeEnd;
    @NonNull
    public final String title;

    public Event(@NonNull Calendar eventTimeStart, @NonNull Calendar eventTimeEnd, @NonNull String title) {
        this.eventTimeStart = transCalendarToPoint(eventTimeStart);
        this.eventTimeEnd = transCalendarToPoint(eventTimeEnd);
        this.title = title;


    }


    private static int transCalendarToPoint(Calendar eventTime) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.YEAR, eventTime.get(Calendar.YEAR));
        today.set(Calendar.MONTH, eventTime.get(Calendar.MONTH));
        today.set(Calendar.DAY_OF_MONTH, eventTime.get(Calendar.DAY_OF_MONTH));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);


        return (int) ((eventTime.getTimeInMillis() - today.getTimeInMillis()) / 1000);
    }

    @Override
    public int getStartPoint() {
        return eventTimeStart;
    }

    @Override
    public int getEndPoint() {
        return eventTimeEnd;
    }

    @Override
    @NonNull
    public String getText() {
        return title;
    }
}
