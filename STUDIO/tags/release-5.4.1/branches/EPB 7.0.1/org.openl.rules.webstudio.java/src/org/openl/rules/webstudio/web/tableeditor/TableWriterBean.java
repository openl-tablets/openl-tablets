package org.openl.rules.webstudio.web.tableeditor;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

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
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.WebTool;

public class TableWriterBean {

    public boolean ajaxrequest = false;

    public void printComponent(UIComponent comp,String prefix) {
        if (null != comp) {
            System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
            for (int i=0; i < comp.getChildren().size(); i++) {
                printComponent((UIComponent)(comp.getChildren().get(i)), prefix + prefix);
            }
        }
    }
    protected TableModel tableModel;
    protected int elementID;
    protected String view;
    protected String title;
    protected int initialRow;
    protected int initialColumn;
    protected String name;
    protected org.openl.syntax.ISyntaxError[] se;
    protected String url;
    protected String uri;
    protected boolean runnable;
    protected boolean testable;
    protected String parsView;
    protected String sid;

    protected HtmlOutputText createText(String text,String id,boolean escape) {
        //
        HtmlOutputText result = new HtmlOutputText();
        result.setId(id);
        result.setValue(text);
        result.setEscape(escape);
        return result;
    }

    @SuppressWarnings("unchecked")
    protected void initialize() {
        //
        WebStudio studio = WebStudioUtils.getWebStudio();
        Map request = FacesUtils.getRequestParameterMap();

        this.sid = (String)(request.get("elementID"));
           this.elementID = -100;
           if (sid != null)
           {
             elementID = Integer.parseInt(sid);
             studio.setTableID(elementID);
        }
        else {
          elementID = studio.getTableID();
        }
       this.url = studio.getModel().makeXlsUrl(elementID);
       this.uri = studio.getModel().getUri(elementID);
       this.title = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
       this.name = studio.getModel().getDisplayNameFull(elementID);
       this.runnable  = studio.getModel().isRunnable(elementID);
       this.testable  = studio.getModel().isTestable(elementID);
       this.se = studio.getModel().getErrors(elementID);

        String[] menuParamsView = {"transparency", "filterType", "view"};
        this.parsView = WebTool.listParamsExcept2(menuParamsView, request);
        this.view = studio.getModel().getTableView((String) request.get("view"));
       //FacesContext fc = FacesContext.getCurrentInstance();
       //TableWriter tw = new TableWriter(elementID,view,studio);

        System.out.println("elementID="+elementID);
        System.out.println("view=" + view);

        initializeTableModel(elementID, view, studio);
    }

    @SuppressWarnings("unchecked")
    protected void modifyView2(UIViewRoot root) {
        //
        UIComponent spr = root.findComponent("spreadsheet");
        spr.getChildren().clear();
        spr.getChildren().add(createText("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n", root.createUniqueId(), false));
        for (int i=0; i < tableModel.getCells().length; i++) {
            spr.getChildren().add(createText("<tr>\n", root.createUniqueId(), false));
            for (int j=0; j < tableModel.getCells()[i].length; j++) {
                ICellModel cell = tableModel.getCells()[i][j];
                if ((null != cell) && (cell.isReal())) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<td");
                    if (cell instanceof CellModel) {
                        ((CellModel)(cell)).atttributesToHtml(sb, tableModel);
                    }

                    StringBuffer id = new StringBuffer();
                    for(int k=0;k<cell.getRowspan();k++) {
                        for(int l=0; l < cell.getColspan(); l++) {
                            id.append("cell-" + String.valueOf(i+k+1) + "-" + String.valueOf(j+l+1) + "_");
                        }
                    }

                    sb.append(" title=\"").append(id).append("\">\n");

                    spr.getChildren().add(createText(sb.toString(), root.createUniqueId(), false));
                    spr.getChildren().add(createText(cell.getContent(), id.append("text").toString(), false));
                    spr.getChildren().add(createText("</td>\n", root.createUniqueId(), false));
                }
            }
            spr.getChildren().add(createText("</tr>\n", root.createUniqueId(), false));
        }
        spr.getChildren().add(createText("</table>", root.createUniqueId(), false));
    }


    public void render(Writer writer) {
        //
//		TableWriter tw = new TableWriter(elementID,view,getWebStudio(),ajaxrequest);
//		setInitialRow(tw.getInitialRow());
//		setInitialColumn(tw.getInitialColumn());
//		tw.render(writer);
        //
        FacesContext fc = FacesContext.getCurrentInstance();
        ResponseWriter rw = fc.getResponseWriter();
        UIViewRoot root = fc.getViewRoot();
        try {
            fc.setResponseWriter(new HtmlResponseWriterImpl(writer, "text/html", "UTF-8"));
            //printComponent(root,"-");
            //modifyView(root);
            if (!ajaxrequest) {
                modifyView2(root);
            }
            //printComponent(root, "-");
            renderResponse(fc,root.findComponent("spreadsheet"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            fc.setResponseWriter(rw);
        }
    }

    public TableWriterBean() {
        initialize();
    }

    public int getInitialRow() {
        return initialRow;
    }

    public void setInitialRow(int initialRow) {
        this.initialRow = initialRow;
    }

    public int getInitialColumn() {
        return initialColumn;
    }

    public void setInitialColumn(int initialColumn) {
        this.initialColumn = initialColumn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getElementID() {
        return elementID;
    }

    public void setElementID(int elementID) {
        this.elementID = elementID;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public org.openl.syntax.ISyntaxError[] getSe() {
        return se;
    }

    public void setSe(org.openl.syntax.ISyntaxError[] se) {
        this.se = se;
    }

    public String getUrl() {
        return url;
    }

    public String getUri() {
        return uri;
    }

    public boolean isRunnable() {
        return runnable;
    }

    public boolean isTestable() {
        return testable;
    }

    public String getParsView() {
        return parsView;
    }

    public String getSid() {
        return sid;
    }

    
    protected void renderResponse(FacesContext context,UIComponent component) throws IOException {
        //
        component.encodeBegin(context);
        if (component.getRendersChildren()) {
            component.encodeChildren(context);
        } else {
            Iterator<?> kids = component.getChildren().iterator();
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
        view = studio.getModel().getTableView(view);
        
        @SuppressWarnings("unused")
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
        tableModel = tv.buildModel(gt);

        // return new TableViewer().showTable(gt, new ICellFilter[]{cellFilter});
    }
}