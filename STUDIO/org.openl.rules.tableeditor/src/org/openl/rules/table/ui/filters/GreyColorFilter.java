package org.openl.rules.table.ui.filters;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class GreyColorFilter implements IColorFilter {

    private final double brightness;

    @Override
    public short[] filterColor(short[] color) {

        if (color == null) {
            color = BLACK;
        }

        var avg = (color[0] + color[1] + color[2]) / 3;

        avg = (int) (avg * brightness);

        return new short[]{(short) avg, (short) avg, (short) avg};
    }

}
