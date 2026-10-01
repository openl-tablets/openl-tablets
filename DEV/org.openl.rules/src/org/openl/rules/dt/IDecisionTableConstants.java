package org.openl.rules.dt;

/**
 * @author snshor
 */
public final class IDecisionTableConstants {

    public static final int INFO_COLUMN_INDEX = 0;
    public static final int CODE_COLUMN_INDEX = 1;
    public static final int PARAM_COLUMN_INDEX = 2;
    public static final int PRESENTATION_COLUMN_INDEX = 3;

    public static final int SIMPLE_DT_HEADERS_HEIGHT = 3;

    /**
     * When condition is represented as a row. So it is readed from left to right. First 4 columns are service ones.<br>
     * ( see {@link IDecisionTableConstants#INFO_COLUMN_INDEX}, {@link IDecisionTableConstants#CODE_COLUMN_INDEX},
     * {@link IDecisionTableConstants#PARAM_COLUMN_INDEX}, {@link IDecisionTableConstants#PRESENTATION_COLUMN_INDEX})
     */
    public static final int SERVICE_COLUMNS_NUMBER = 4;

    private IDecisionTableConstants() {
    }
}
