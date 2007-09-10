package org.openl.jsf;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneListbox;

import org.openl.jsf.editor.metadata.IHtmlSelectMetadata;

public class HtmlSelectActivator implements ICellEditorActivator {
	
	@Override
	public UIComponent createInstance(Object value, Object metadata) {
		//
		HtmlSelectOneListbox result = new HtmlSelectOneListbox();
		result.setOnchange("javascript:stopEditing2();");
		result.setSize(1);
		if (metadata instanceof IHtmlSelectMetadata) {
			List l = ((IHtmlSelectMetadata)metadata).getList();
			for (int i=0; i < l.size(); i++) {
				UISelectItem item = new UISelectItem();
				item.setItemValue(l.get(i));
				result.getChildren().add(item);				
			}
		}
		return result;
	}
}