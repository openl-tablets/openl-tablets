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

IconManager.prototype = {
    initialize: function(parentStyleClasses, parentStyleClassesMouseOver, disabledClass) {
        this.disabledClass = disabledClass;
        this.parentStyleClasses = parentStyleClasses;

        this.onMouseOver = function() {this.className = parentStyleClassesMouseOver}
        this.onMouseOut = function() {this.className = parentStyleClasses}
    },

    enable: function(img) {
        img = $(img);
        img.className = "";

        var parent = img.up();
        parent.className = this.parentStyleClasses;
        parent.onmouseover = this.onMouseOver;
        parent.onmouseout = this.onMouseOut;
    },

    disable: function(img) {
        img = $(img);
        img.className = this.disabledClass;

        var parent = img.up();
        parent.onmouseover = Prototype.emptyFunction;
        parent.onmouseout = Prototype.emptyFunction;
        parent.className = this.parentStyleClasses;
    }
}