/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.ui;

import org.openl.base.INamedThing;
import org.openl.rules.dt.DecisionTable.DecisionTableTraceObject;
import org.openl.rules.dt.DecisionTable.DecisionTableTraceObject.RuleTracer;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITableTracerObject;
import org.openl.rules.table.ui.ColorGridFilter;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.ITracerObject;


/**
 * @author snshor
 *
 */
public class TraceHelper
{
	
	
	
	ITracerObject root;
	
	public void setRoot(ITracerObject root)
	{
		this.root = root;
	}
	
	
	public TableInfo getTableInfo(int elementID)
	{
		ITracerObject tt = (ITracerObject)traceRenderer.map.getObject(elementID);
		
		if (tt == null)
			return null;

        if (!(tt instanceof ITableTracerObject)) {
            return null;
        }

//        DecisionTableTraceObject dtt = null;
//		ITracerObject[] rtt = null;
		String displayName = null;
//		IGridTable gt = null;
		
//		if (tt instanceof DecisionTableTraceObject)
//		{
//			dtt = (DecisionTableTraceObject)tt;
//			rtt = dtt.getTracerObjects();
//			DecisionTable dt = dtt.getDT();
//			displayName = dtt.getDisplayName(INamedThing.REGULAR);
//			TableSyntaxNode tsn = dt.getTableSyntaxNode();
//
//			gt = tsn.getTable().getGridTable();
//		}
//		else if (tt instanceof RuleTracer)
//		{
//			
//			dtt = ((RuleTracer)tt).getParentTraceObject();
//			rtt = new ITracerObject[]{tt};
//			displayName = dtt.getDisplayName(INamedThing.REGULAR);
//			displayName += rtt[0].getDisplayName(INamedThing.REGULAR);
//			gt = ((RuleTracer)rtt[0]).getRuleTable().getGridTable();
//		}
//		else
//			return null;
			
        ITableTracerObject tto = (ITableTracerObject)tt;
        TableSyntaxNode tsn = tto.getTableSyntaxNode();
        IGridTable gt = tsn.getTable().getGridTable();
        
        if (tto instanceof ATableTracerNode) {
            displayName = tto.getDisplayName(INamedThing.REGULAR);
        } else {
            // ATableTracerLeaf
            displayName = tto.getDisplayName(INamedThing.REGULAR);
            ITableTracerObject[] rtt = tto.getTableTracers();
            displayName += rtt[0].getDisplayName(INamedThing.REGULAR);
        }
		
		return new TableInfo(gt, displayName, false);
	}

	
	
	public String showTrace(int id, ProjectModel model, String view)
	{
		
		ITracerObject tt = (ITracerObject)traceRenderer.map.getObject(id);

		
		if (tt == null)
			return "ERROR ID = " + id;
		
		if (!(tt instanceof ITableTracerObject)) {
		    return "----";
		}
		
		ITableTracerObject tto = (ITableTracerObject)tt;
		TableSyntaxNode tsn = tto.getTableSyntaxNode();
		ITableTracerObject[] rtt = tto.getTableTracers();

		IGridTable gt = tsn.getTable().getGridTable();
        view = model.getTableView(view);
		ILogicalTable gtx =  tsn.getSubTables().get(view);
		if (gtx != null)
				gt = new TableEditorModel(gtx.getGridTable()).getUpdatedTable();

        TableModel tableModel = ProjectModel.buildModel(gt, new IGridFilter[]{makeFilter(rtt, model)});
        return new HTMLRenderer.TableRenderer(tableModel).renderWithMenu(null);
	}

    public int getProjectNodeIndex(int id, ProjectModel model) {
        ITracerObject tt = (ITracerObject)traceRenderer.map.getObject(id);


		if (tt == null)
			return -1;

		DecisionTableTraceObject dtt = null;

		if (tt instanceof DecisionTableTraceObject)
		{
			dtt = (DecisionTableTraceObject)tt;
		}
		else if (tt instanceof RuleTracer)
		{

			dtt = ((RuleTracer)tt).getParentTraceObject();
		} else
            return -1;

        return model.indexForNode(dtt.getDT().getTableSyntaxNode());
    }


    IGridFilter makeFilter(ITableTracerObject[] rtt, ProjectModel model)
	{
		IGridRegion[] regions = new IGridRegion[rtt.length];
		
		for (int i = 0; i < rtt.length; i++)
		{
          regions[i] = rtt[i].getGridRegion();
		}
		
//		return ColorGridFilter.makeTransparentFilter(new RegionGridSelector(regions, true), 0.7, 0x00ff00);
		
		return new ColorGridFilter(new RegionGridSelector(regions, true), model.getFilterHolder().makeFilter());

		
		//return new RuleTracerCellFilter(rtt);
		
	}
	
	
//	static class RuleTracerCellFilter implements ICellFilter, ICellSelector
//	{
//		ITracerObject[] rtt;
//
//		/* (non-Javadoc)
//		 * @see org.openl.rules.ui.ICellSelector#select(org.openl.rules.table.IGridTable, int, int)
//		 */
//		public boolean select(IGridTable table, int col, int row)
//		{
//
//			int xcol = table.getGridColumn(col, row);
//			int xrow = table.getGridRow(col, row);
//			
//			for (int i = 0; i < rtt.length; i++)
//			{
//				if (IGridRegion.Tool.contains(((RuleTracer)rtt[i]).getRuleTable().getGridTable().getRegion(), xcol, xrow))
//					return false;
//			}
//			
//			return true;
//		}
//
//		/**
//		 * @param rtt
//		 */
//		public RuleTracerCellFilter(ITracerObject[] rtt)
//		{
//			this.rtt = rtt;
//		}
//
//		/* (non-Javadoc)
//		 * @see org.openl.rules.ui.ICellFilter#getColorFilter()
//		 */
//		public IColorFilter[] getColorFilter()
//		{
//			return new IColorFilter[]{cf,cf, cf};
//		}
//
//		/* (non-Javadoc)
//		 * @see org.openl.rules.ui.ICellFilter#getSelector()
//		 */
//		public ICellSelector getSelector()
//		{
//			return this;
//		}
//
//		/* (non-Javadoc)
//		 * @see org.openl.rules.ui.ICellFilter#getTextFilter()
//		 */
//		public ITextFilter getTextFilter()
//		{
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}
	

	
	public String printTraceMethod(IOpenMethodHeader header, Object[] params, StringBuffer buf, ProjectModel model)
	{
		buf.append(header.getName()).append('(');
		ObjectViewer viewer = new ObjectViewer(model);
		
		for (int i = 0; i < params.length; i++)
		{
			if (i > 0)
				buf.append(",");
			buf.append(viewer.displayResult(params[i]));
		}
		
		buf.append(')');
		return buf.toString();
	}
	
	
	TraceTreeRenderer traceRenderer;
	
	
	public String renderTraceTree(String targetJsp, String targetFrame)
	{
		traceRenderer = new TraceTreeRenderer(targetJsp, targetFrame);
		
		return traceRenderer.renderRoot(root);
	}
}
