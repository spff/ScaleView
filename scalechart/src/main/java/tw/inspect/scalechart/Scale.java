package tw.inspect.scalechart;

public class Scale {
    final int unit;
    final double textResolution;
    final double scaleResolution;
    final double lengthRatio;

    /**
     * @param textResolution  unit / inch
     * @param scaleResolution unit / inch
     */
    Scale(final int unit, final double textResolution, final double scaleResolution, final double lengthRatio) {
        this.unit = unit;
        this.textResolution = textResolution;
        this.scaleResolution = scaleResolution;
        this.lengthRatio = lengthRatio;
    }

    String parse(int value) {
        return value / unit + "";
    }
}
