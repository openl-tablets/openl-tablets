/**
 * Common javascript for webstudio application.
 *
 * @requires prototype JavaScript library
 *
 * @author Aliaksandr Antonik.
 */

/**
 * Class for managing icons: enabling/disabling
 */
var IconManager = Class.create();


IconManager.renameProperty = function (obj, p1, p2) {
    obj[p1] = obj[p2];
    if (Prototype.Browser.IE)
        obj[p2] = Prototype.emptyFunction;
    else
        delete obj[p2]
}

IconManager.prototype = {
    initialize: function(parentStyleClasses, parentStyleClassesMouseOver, disabledClass) {
        this.disabledClass = disabledClass;
        this.parentStyleClasses = parentStyleClasses;

        this.onMouseOver = function() {this.className = parentStyleClassesMouseOver}
        this.onMouseOut = function() {
            try {
                this.className = parentStyleClasses
                this.down().offset(0);
            } catch(ex) {}
        }
        this.onMouseDown = function() {this.offset(1)};
        this.onMouseUp = function() {this.offset(0)}; 
    },

    init: function(img) {
        img = $(img);
        img.style.position = "relative";
        img.offset = function(delta) {this.style.top = this.style.left = delta};
        this.disable(img);
    },

    enabled: function(img) {
        return img.className != this.disabledClass;
    },

    enable: function(img) {
        if (this.enabled(img = $(img))) return;
        img.className = "";
        IconManager.renameProperty(img, "onclick", "_onclick")

        var parent = img.up();
        parent.className = this.parentStyleClasses;
        parent.onmouseover = this.onMouseOver;
        parent.onmouseout = this.onMouseOut;

        img.onmousedown = this.onMouseDown;
        img.onmouseup = this.onMouseUp;
    },

    disable: function(img) {
        if (!this.enabled(img = $(img))) return;
        img.className = this.disabledClass;
        IconManager.renameProperty(img, "_onclick", "onclick")
        
        var parent = img.up();
        parent.onmouseover = parent.onmouseout = img.onmousedown = img.onmouseup = Prototype.emptyFunction;
        parent.className = this.parentStyleClasses;
        img.offset(0);
    }
}