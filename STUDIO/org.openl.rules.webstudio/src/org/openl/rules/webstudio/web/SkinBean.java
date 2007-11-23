package org.openl.rules.webstudio.web;

import javax.faces.model.SelectItem;


/**
 * The container of current skin.
 *
 * @author "Andrey Naumenko"
 */
public class SkinBean {
    private static final String[] skinsArray = {
            "DEFAULT", "blueSky", "classic", "deepMarine", "emeraldTown", "japanCherry",
            "ruby", "wine", "plain"
        };
    private String skin = skinsArray[1];

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public SelectItem[] getSkins() {
        SelectItem[] selectItems = new SelectItem[skinsArray.length];
        for (int i = 0; i < skinsArray.length; i++) {
            selectItems[i] = new SelectItem(skinsArray[i]);
        }
        return selectItems;
    }
}
