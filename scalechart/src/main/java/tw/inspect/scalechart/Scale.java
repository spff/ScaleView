package tw.inspect.scalechart;

public class Scale {
    final int unit;
    final double textResolution;
    final double scaleResolution;
    final double lengthRatio;
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
