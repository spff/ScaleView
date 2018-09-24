package tw.inspect.scalechart;

import android.support.annotation.NonNull;

public interface OnLineSegmentClickedListener {
    void onLineSegmentClicked(@NonNull ScaleChartView scaleChartView, int segmentIndex);
}
