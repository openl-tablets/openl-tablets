package org.openl.jsf;


import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared_impl.renderkit.html.HtmlResponseWriterImpl;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.SimpleHtmlFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.CellModel;
import org.openl.rules.ui.ICellModel;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;
import org.openl.rules.ui.WebStudio;
import org.richfaces.component.html.HtmlColumn;
import org.richfaces.component.html.HtmlDataTable;

public class TableWriter {
	//
	
	public void printComponent(UIComponent comp,String prefix) {
		if (null != comp) {
			System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
			for (int i=0; i < comp.getChildren().size(); i++) {
				printComponent((UIComponent)(comp.getChildren().get(i)), prefix + prefix);
			}
		}
	}
	
	protected TableModel tableModel;
	protected int initialRow;
	protected int initialColumn;
	
	public int getInitialRow() {
		return initialRow;
	}

	public int getInitialColumn() {
		return initialColumn;
	}
	
	protected HtmlColumn createColumn(int rowSpan,int colSpan,boolean breakBefore,UIComponent children[],String id,String style) {
		//
		HtmlColumn result = new HtmlColumn();
		result.setId(id);
		result.setTitle(id);
		result.setRowspan(rowSpan);
		result.setColspan(colSpan);
		result.setBreakBefore(breakBefore);
		if (null != children) {
			for (int i=0; i < children.length; i++) {
				result.getChildren().add(children[i]);
			}
		}
		if (null != style) result.setStyle(style);
		return result;
	}
	
	protected HtmlOutputText createText(String text,String id,boolean escape) {
		//
		HtmlOutputText result = new HtmlOutputText();
		result.setId(id);
		result.setValue(text);
		result.setEscape(escape);
		return result;
	}
	
	protected void printTableModel() {
		System.out.println("printTableModel:begin");
		for (int i=0; i < tableModel.getCells().length; i++) {
			for (int j=0; j < tableModel.getCells()[i].length; j++) {
				ICellModel cell = tableModel.getCells()[i][j];
				if ((null != cell) && (cell.isReal())) {
					System.out.println("cell[" + i + "][" + j + "]");
					System.out.println("content=" + cell.getContent());
					System.out.println("rowSpan=" + cell.getRowspan());
					System.out.println("colSpan=" + cell.getColspan());
					System.out.println();
				}
			}
		}
		System.out.println("printTableModel:end");
	}
	
	protected void modifyView(UIViewRoot root) {

		if ((null != root) && (null == root.findComponent("spreadsheet"))) {
			//
			//printComponent(root, "-");
			HtmlDataTable hdt = new HtmlDataTable();
			
			hdt.setId("spreadsheet");
			hdt.setValue("#{testBean.rows1}");
			hdt.setVar("row");
//			hdt.getChildren().add(createColumn(2,1,false,createText("cell1",root.createUniqueId()),root.createUniqueId()));
//			hdt.getChildren().add(createColumn(1,1,false,createText("cell2",root.createUniqueId()),root.createUniqueId()));
//			hdt.getChildren().add(createColumn(1,1,true,createText("cell3",root.createUniqueId()),root.createUniqueId()));
//			hdt.setId(root.createUniqueId());
			
			boolean first = true;			
			
			for (int i=0; i < tableModel.getCells().length; i++) {
				boolean breakBefore = 0 < i;
				for (int j=0; j < tableModel.getCells()[i].length; j++) {
					ICellModel cell = tableModel.getCells()[i][j];
					if ((null != cell) && (cell.isReal())) {
						String style=null;
						if (cell instanceof CellModel) {
							style = ((CellModel)cell).getHtmlStyle(tableModel);
							if ("".equals(style.trim())) {
								style = null;
							}
						}
						if (first) {
							initialRow = i+1;
							initialColumn = j+1;
							first = false;
						}
						String id="";
						for(int k=0;k<cell.getRowspan();k++) {
							for(int l=0; l < cell.getColspan(); l++) {
								id = id + "cell-" + String.valueOf(i+k+1) + "-" + String.valueOf(j+l+1) + "_";
							}
						}
						UIComponent column = createColumn(cell.getRowspan(),cell.getColspan(),breakBefore,
								new UIComponent[]{createText(cell.getContent(),id + "text",false)},
								id,style); 
						hdt.getChildren().add(column);
						breakBefore = false;
					}
				} //
			}
			root.getChildren().add(hdt);
		}
	}
	
	
	protected void renderResponse(FacesContext context,UIComponent component) throws IOException {
		//
		component.encodeBegin(context);
		if (component.getRendersChildren()) {
			component.encodeChildren(context);
		} else {
			Iterator kids = component.getChildren().iterator();
			while(kids.hasNext()) {
				renderResponse(context, (UIComponent)(kids.next()));
			}
		}
		component.encodeEnd(context);
	}

	protected void initializeTableModel(int elementID,String view,WebStudio studio) {
		//
		//System.out.println("initializeTableModel");
		//::studio.getModel().showTable(elementID, view)
		TableSyntaxNode tsn = studio.getModel().getNode(elementID);
		if (tsn == null) return; // table is not selected yet
		IGridTable gt = tsn.getTable().getGridTable();
		if (view == null) {view = studio.getMode().getTableMode();}
		boolean showGrid = studio.getMode().showTableGrid();
		if (view != null) {
			ILogicalTable gtx = (ILogicalTable) tsn.getSubTables().get(view);
			if (gtx != null) gt = gtx.getGridTable();
		}
		//::return showTable(gt, showGrid);
		//::return showTable(gt, (IGridFilter[]) null, showgrid);
		IGrid htmlGrid = gt.getGrid();
		if (!(htmlGrid instanceof FilteredGrid))
		{
			int N = 2;
			IGridFilter[] f1 = new IGridFilter[N];
			f1[0] = new SimpleXlsFormatter();
			f1[1] = new SimpleHtmlFilter();
			htmlGrid = new FilteredGrid(gt.getGrid(), f1);
		}

		TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
		tableModel = tv.buildModel();
		
		// return new TableViewer().showTable(gt, new ICellFilter[]{cellFilter});
	}

	public void render(Writer writer) {
		FacesContext fc = FacesContext.getCurrentInstance();
		UIViewRoot root = fc.getViewRoot();
		//root.getChildren().clear();
		//UIViewRoot root = new UIViewRoot();
		fc.setResponseWriter(new HtmlResponseWriterImpl(writer, "text/html", "UTF-8"));
		modifyView(root);
		try {
			renderResponse(fc,root);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public TableWriter(int elementID,String view,WebStudio studio) {
		//
		System.out.println("initializing table model:begin");
		initializeTableModel(elementID,view,studio);
		System.out.println("initializing table model:end");
		//printTableModel();
		//
	}
	
	public void addRow() {
		for (int i=0; i < tableModel.getCells().length; i++) {
			
		}
	}
	
}