package org.openl.rules.ui.jsf.custom.tableeditor.model;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public class CellModelImpl implements CellModel {
	private int rowspan;
	private int colspan;
	/**
	 * @return the rowspan
	 */
	public int getRowspan() {
		return rowspan;
	}

	public boolean isReal() {
		return true;
	}

	/**
	 * @param rowspan the rowspan to set
	 */
	public void setRowspan(int rowspan) {
		this.rowspan = rowspan;
	}
	/**
	 * @return the colspan
	 */
	public int getColspan() {
		return colspan;
	}
	/**
	 * @param colspan the colspan to set
	 */
	public void setColspan(int colspan) {
		this.colspan = colspan;
	}

	public static CellModel createUnrealCell(CellModel realCell) {
		return new CellModelDelegator(realCell);
	}
	
	private static class CellModelDelegator implements CellModel {
		CellModel delegate;
		public CellModelDelegator(CellModel delegate) {
			this.delegate = delegate;
		}

		public int getColspan() {
			return delegate.getColspan();
		}

		public int getRowspan() {
			return delegate.getRowspan();
		}

		public boolean isReal() {
			return false;
		}
	}
	
}
