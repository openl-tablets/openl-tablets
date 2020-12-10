package org.openl.rules.dt.element;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.table.ILogicalTable;

public class RuleRow {

    private final int row;
    private final ILogicalTable table;

    public RuleRow(int row, ILogicalTable table) {
        this.row = row;
        this.table = table;
    }

    private final Map<Integer, String> cache = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public String getRuleName(int col) {
        try {
            readLock.lock();
            String ruleName = cache.get(col);
            if (ruleName != null) {
                return ruleName;
            }
        } finally {
            readLock.unlock();
        }
        try {
            ILogicalTable valueCell = table
                .getSubtable(col + IDecisionTableConstants.SERVICE_COLUMNS_NUMBER, row, 1, 1);
            String ruleName = valueCell.getSource().getCell(0, 0).getStringValue();
            if (ruleName == null) {
                ruleName = StringUtils.EMPTY;
            }
            writeLock.lock();
            cache.put(col, ruleName);
            return ruleName;
        } finally {
            writeLock.unlock();
        }
    }

}
