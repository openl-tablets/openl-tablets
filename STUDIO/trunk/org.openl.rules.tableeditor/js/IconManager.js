/**
 * Class for managing (enabling/disabling) icons.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Aliaksandr Antonik
 */

var IconManager = Class.create({

    initialize: function(enabledClass, overClass, disabledClass) {
        this.disabledClass = disabledClass;
        this.enabledClass = enabledClass;

        this.onMouseOver = function() {
            this.className = overClass;
        }
        this.onMouseOut = function() {
            this.className = enabledClass;
        }
    },

    init: function(img) {
        this.disable(img);
    },

    enabled: function(img) {
        return img.className != this.disabledClass;
    },

    enable: function(img) {
        if (this.enabled(img = $(img))) return;
        img.className = this.enabledClass;

        img.onmouseover = img._mouseover;
        img.onmouseout = img._mouseout;
        img.onclick = img._onclick;
    },

    disable: function(img) {
        if (!this.enabled(img = $(img))) return;
        img.className = this.disabledClass;
        
        img._mouseover = img.onmouseover;
        img._mouseout = img.onmouseout;
        img._onclick = img.onclick;
        img.onmouseover = img.onmouseout = img.onclick = Prototype.emptyFunction;
    }
});