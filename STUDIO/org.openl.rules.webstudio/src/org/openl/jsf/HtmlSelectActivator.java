package org.openl.jsf;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneListbox;

public class HtmlSelectActivator implements ICellEditorActivator {
	
	@Override
	public UIComponent createInstance(Object value, Object metadata) {
		//
		HtmlSelectOneListbox result = new HtmlSelectOneListbox();
		result.setSize(1);
		if (metadata instanceof List) {
			List l = (List)metadata;
			for (int i=0; i < l.size(); i++) {
				UISelectItem item = new UISelectItem();
				item.setItemValue(l.get(i));
				result.getChildren().add(item);				
			}
		}
		return result;
	}
}