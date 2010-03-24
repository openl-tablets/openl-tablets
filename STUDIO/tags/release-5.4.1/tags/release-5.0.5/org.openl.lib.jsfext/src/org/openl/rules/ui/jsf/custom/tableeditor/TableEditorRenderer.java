package org.openl.rules.ui.jsf.custom.tableeditor;

import static org.openl.rules.ui.jsf.custom.HTML.*;
import org.openl.rules.ui.jsf.custom.tableeditor.model.TableModel;
import org.openl.rules.ui.jsf.custom.tableeditor.model.CellModel;
import org.ajax4jsf.renderkit.RendererBase;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public class TableEditorRenderer extends RendererBase {
	/**
	 * Identifier of this renderer type.
	 */
	public static final String RENDERER_TYPE = "org.openl.rules.faces.render.TableEditor";
	/**
	 * Get base component class, targetted for this renderer. Used for check arguments in decode/encode.
	 *
	 * @return <code>TableEditor.class</code> 
	 */
	protected Class getComponentClass() {
		return TableEditor.class;
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	private static void validate(TableEditor tableEditor) {
		if (tableEditor.getChildCount() != 1) {
			throw new IllegalStateException("table editor must have one child");
		}

		if (!(tableEditor.getChildren().get(0) instanceof UICell)) {
			throw new IllegalStateException("table editor must contain UI cell child");
		}
	}

	@Override
	public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException {
		TableEditor tableEditor = (TableEditor) uiComponent;
		validate(tableEditor);
		ResponseWriter writer = facesContext.getResponseWriter();

		writer.startElement(TABLE, tableEditor);
		writer.writeAttribute("cellspacing", "0", null);
		writer.writeAttribute("cellpadding", "0", null);
		writer.writeAttribute("border", "0", null);
	}

	@Override
	public void encodeChildren(FacesContext facesContext, UIComponent uiComponent) throws IOException {
		TableEditor tableEditor = (TableEditor) uiComponent;
		ResponseWriter writer = facesContext.getResponseWriter();

		UICell uiCell = (UICell) tableEditor.getChildren().get(0);
		TableModel tableModel = tableEditor.getTableModel();

		for (int row = 0,  rowCount = tableModel.getCells().length; row < rowCount; ++row) {
			CellModel[] modelRow = tableModel.getCells()[row];
			writer.startElement(TABLE_ROW,  tableEditor);

			for (int col = 0, colCount = tableModel.getCells()[0].length; col < colCount; ++col) {
				CellModel cellModel = modelRow[col];
				if (cellModel.isReal()) {
					tableEditor.setSelectedCell(row, col);
					writer.startElement(TABLE_COLUMN,  uiCell);
					if (cellModel.getColspan() != 1)
						writer.writeAttribute("colspan", String.valueOf(cellModel.getColspan()), null);
					if (cellModel.getRowspan() != 1)
						writer.writeAttribute("rowspan", String.valueOf(cellModel.getRowspan()), null);
					writeAttributeIfNotNull("bgcolor", uiCell.getBgcolor(), writer);
					writeAttributeIfNotNull("align", uiCell.getHalign(), writer);
					writeAttributeIfNotNull("valign", uiCell.getValign(), writer);
					writeAttributeIfNotNull("style", uiCell.getCssStyle(), writer);
					{
						Integer width = uiCell.getWidth();
						if (width != null && width >= 0)
							writer.writeAttribute("width", width, null);
					}

					renderChild(facesContext, uiCell);

					writer.endElement(TABLE_COLUMN);
				}
			}
			writer.endElement(TABLE_ROW);
		}
	}

	public static void writeAttributeIfNotNull(String prop, String value, ResponseWriter writer) throws IOException {
		if (value != null) {
			writer.writeAttribute(prop, value, null);
		}
	}

	@Override
	public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException {
		ResponseWriter writer = facesContext.getResponseWriter();

		writer.endElement(TABLE);
	}
}
