/**
 * Popup.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Andrei Astrouski
 */
var Popup = Class.create({

    popup: null,

    initialize: function(content, left, top, params) {
        this.content = content;
        this.left = left;
        this.top = top;
        this.params = params;
    },

    show: function() {
        var self = this;

        if (!this.popup) {
            this.popup = this.createPopup();

            // Show popup
            document.body.appendChild(this.popup);
        }
    },

    createPopup: function() {
        var popupDiv = new Element("div");

        popupDiv.update(this.content);

        popupDiv.addClassName('popup');

        popupDiv.style.left = this.left + "px";
        popupDiv.style.top = this.top + "px";

        if (this.params) {
            if (this.params.width) {
                popupDiv.style.width = this.params.width;
            }
            if (this.params.height) {
                popupDiv.style.height = this.params.height;
            }
        }

        return popupDiv;
    },

    hide: function() {
        this.hide(0);
    },

    hide: function(timeout) {
        var self = this;
        if (!timeout || timeout < 0) {
            timeout = 0;
        }
        window.setTimeout(function() {
            if (self.popup) {
                document.body.removeChild(self.popup);
                self.popup = null;
            }
        }, timeout);
    },

    has: function(element) {
    	if (!this.popup || !element) {
    		return false;
    	}
        return element.descendantOf(this.popup); 
    },

    bind: function(event, handler) {
        Event.observe(this.popup, event, handler);
    },

    unbind: function(event, handler) {
        Event.stopObserving(this.popup, event, handler);
    }

});
