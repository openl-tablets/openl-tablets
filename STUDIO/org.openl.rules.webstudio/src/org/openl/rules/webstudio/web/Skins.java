package org.openl.rules.webstudio.web;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;

public class Skins {
    private static final String[] skinsArray = new String[] { "DEFAULT", "blueSky", "classic", "deepMarine", "emeraldTown", "japanCherry", "ruby", "wine", "plain" };
    private static final String defaultSkin = skinsArray[0];
    private static final String BEAN_NAME = "SkinBean";
    
    private String skin = defaultSkin;

    private UISelectOne createComponent() {
        UISelectOne selectOne = new UISelectOne();
        selectOne.setValue(skin);

        for (int i = 0; i < skinsArray.length; i++) {
            String skinName = skinsArray[i];

            UISelectItem item = new UISelectItem();
            item.setItemLabel(skinName);
            item.setItemValue(skinName);
            item.setId("skinSelectionFor_" + skinName);

            selectOne.getChildren().add(item);
        }

        return selectOne;
    }

    public String getSkin() {
        return skin;
    }

    public UIComponent getComponent() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map requestMap = facesContext.getExternalContext().getRequestMap();
        Object object = requestMap.get(BEAN_NAME);
        if (object != null) {
            return (UISelectOne) object;
        }

        UISelectOne selectOne = createComponent();
        requestMap.put(BEAN_NAME, selectOne);
        return selectOne;
    }

    public void setComponent(UIComponent component) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(BEAN_NAME, component);
    }

    public String change() {
        UISelectOne selectOne = (UISelectOne) getComponent();
        skin = (String) selectOne.getValue();
        return null;
    }
}
