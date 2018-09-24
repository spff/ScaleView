package tw.inspect.scalechart;

import android.support.annotation.NonNull;

public interface LineSegment {
    int getStartPoint();

    int getEndPoint();

    @NonNull
    String getText();
}
