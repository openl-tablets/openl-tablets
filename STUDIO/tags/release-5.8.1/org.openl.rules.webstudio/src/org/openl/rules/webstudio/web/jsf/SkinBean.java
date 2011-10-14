package org.openl.rules.webstudio.web.jsf;

import javax.faces.model.SelectItem;
import javax.servlet.http.Cookie;

import org.openl.commons.web.jsf.FacesUtils;

/**
 * The container of current skin.
 *
 * @author Andrey Naumenko
 */
public class SkinBean {
    private static final String[] skinsArray = { "DEFAULT", "blueSky", "classic", "deepMarine", "emeraldTown",
            "japanCherry", "ruby", "wine" };

    private static final String DEFAULT_SKIN = skinsArray[2];
    private static final String SKIN_COOKIE = "rulesskin";

    private String skin;

    public String getSkin() {
        if (skin == null) {
            Cookie[] cookies = FacesUtils.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (SKIN_COOKIE.equals(cookie.getName())) {
                        skin = cookie.getValue();
                        break;
                    }
                }
            }

            if (skin == null) {
                skin = DEFAULT_SKIN;
            }
        }

        return skin;
    }

    public SelectItem[] getSkins() {
        return FacesUtils.createSelectItems(skinsArray);
    }

    public void setSkin(String skin) {
        if (skin != null && !skin.equals(this.skin)) {
            Cookie skinCookie = new Cookie(SKIN_COOKIE, skin);
            skinCookie.setMaxAge(60 * 60 * 24 * 365); // store it for a year
            FacesUtils.addCookie(skinCookie);
        }

        this.skin = skin;
    }
}
