package org.openl.rules.ui.jsf.custom.tableeditor;

import org.openl.rules.ui.jsf.custom.tableeditor.model.TableModel;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import java.io.IOException;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public class TableEditor extends UIComponentBase {
	/**
	 * Identifier of the component type for the component.
	 */
	public static final String COMPONENT_TYPE = "org.openl.rules.faces.TableEditor";
	/**
	 * The identifier of the component family this component belongs to.
	 */
	public static final String COMPONENT_FAMILY = "org.openl.rules.faces";

	private TableModel tableModel;
	private String var;
	private int selectedCellX = -1;
	private int selectedCellY = -1;

	public TableEditor() {
		setRendererType(TableEditorRenderer.RENDERER_TYPE);
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		super.encodeEnd(context);
		setSelectedCell(-1, -1);
	}

	/**
	 * Returns the identifier of the component family to which this component belongs.
	 *
	 * @return {@link #COMPONENT_FAMILY} constant value
	 */
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		super.encodeBegin(context);
	}

	/**
	 * @return the tableModel
	 */
	public TableModel getTableModel() {
		if (tableModel != null) return tableModel;
		ValueBinding vb = getValueBinding("tableModel");
		return tableModel = (vb != null ? (TableModel) vb.getValue(getFacesContext()) : null);
	}

	/**
	 * @param tableModel the tableModel to set
	 */
	public void setTableModel(TableModel tableModel) {
		this.tableModel = tableModel;
	}

	/**
	 * Set the name of the temporary variable that will be exposed to
	 * child components of the table to tell them what the "rowData"
	 * object for the current row is. This value must be a literal
	 * string (EL expression not permitted).
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * @return the var
	 */
	public String getVar() {
		return var;
	}

	public void setSelectedCell(int row, int col) {
		FacesContext facesContext = getFacesContext();
		if (row < 0 || col < 0) {
			selectedCellX = selectedCellY = -1;
			if (var != null) facesContext.getExternalContext().getRequestMap().remove(var);
		} else {
			selectedCellX = row;
			selectedCellY = col;
			if (var != null) facesContext.getExternalContext().getRequestMap().put(var, tableModel.getCells()[row][col]);
			if (!tableModel.isCellAvailable(row, col)) {
				setSelectedCell(-1, -1);
				return;
			}
			if (var != null) facesContext.getExternalContext().getRequestMap().put(var, tableModel.getCells()[row][col]);
		}
	}

	/**
	 * Gets the state of the instance as a <code>Serializable</code> Object.
	 *
	 * @param facesContext 'current' FacesContext instance
	 * @return encoded object
	 */
	public Object saveState(FacesContext facesContext) {
		return new Object[]{super.saveState(facesContext),
				  var,
				  selectedCellX == -1 ? null : selectedCellX,
				  selectedCellY == -1 ? null : selectedCellY
		};
	}

	/**
	 * Performs restoring the state from the entries in the <code>state</code> Object.
	 *
	 * @param facesContext 'current' FacesContext instance
	 * @param object an object to decode state from
	 */
	public void restoreState(FacesContext facesContext, Object object) {
		Object[] values = (Object[]) object;
		super.restoreState(facesContext, values[0]);
		tableModel = null;
		var = (String) values[1];
		selectedCellX = values[2] == null ? -1 : (Integer)values[2];
		selectedCellY = values[3] == null ? -1 : (Integer)values[3];
	}
}
