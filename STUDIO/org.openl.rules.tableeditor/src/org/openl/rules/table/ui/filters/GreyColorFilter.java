package org.openl.rules.table.ui.filters;

/**
 * @author snshor
 *
 */
public class GreyColorFilter implements IColorFilter {

    private double brightness;

    public GreyColorFilter(double brightness) {
        this.brightness = brightness;
    }

    @Override
    public short[] filterColor(short[] color) {

        if (color == null) {
            color = BLACK;
        }

        int avg = (color[0] + color[1] + color[2]) / 3;

        avg = (int) (avg * brightness);

        return new short[] { (short) avg, (short) avg, (short) avg };
    }

}
