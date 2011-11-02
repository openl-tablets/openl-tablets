/**
 * Tooltip.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Andrei Astrouski
 */
var Tooltip = Class.create({

    ie6: Prototype.Browser.IE
        && parseInt(navigator.userAgent.substring(
                navigator.userAgent.indexOf("MSIE") + 5)) == 6,

    firedTooltips: $H(),
    firedIE6Popups: $H(),

    initialize: function(id, content, params) {
        var self = this;

        this.element = $(id);
        this.content = content;
        this.params = params;

        if (params && params.showOn) {
            this.showOn = params.showOn instanceof Array ? params.showOn : [params.showOn];
        } else {
            this.showOn = ["mouseover"]; // by default
        }

        if (params && params.hideOn) {
            this.hideOn = params.hideOn instanceof Array ? params.hideOn : [params.hideOn];
        } else {
            this.hideOn = ["mouseout"]; // by default
        }

        this.showOn.each(function(e) {
            self.showHandler = self.show.bindAsEventListener(self);
            Event.observe(self.element, e, self.showHandler);
        });

        this.hideOn.each(function(e) {
            self.hideHandler = self.hide.bindAsEventListener(self);
            Event.observe(self.element, e, self.hideHandler);
        });
    },

    show: function() {
        var tooltip = this.createTooltip();
        if (!this.firedTooltips.get(this.element.id)) {
            // Show tooltip
            document.body.appendChild(tooltip);
            this.firedTooltips.set(this.element.id, tooltip);

            var position = (this.params && this.params.position) ? this.params.position : 'top_right';
            tooltip.addClassName('tooltip_' + position);

            this.applyStylesToPointer(tooltip);

            var pos = this.calculateInitPosition(tooltip, position);
            tooltip.style.left = pos[0] + "px";
            tooltip.style.top = pos[1] + "px";

            // The iframe hack to cover selectlists in Internet Explorer 6.
            // Selectlists are always on top in IE 6, so they should be covered by iframe.
            if (this.ie6) {
                var ie6Popup = this.createIE6Popup();
                this.openIE6Popup(ie6Popup,
                        tooltip.style.left,
                        tooltip.style.top,
                        tooltip.offsetWidth + "px",
                        tooltip.offsetHeight + "px");
            }
            this.firedIE6Popups.set(this.element.id, ie6Popup);
        }
    },

    calculateInitPosition: function(tooltip, position) {
        var initPos = Element.viewportOffset(this.element);

        switch (position) {
            case 'top_right':
                initPos[0] += (this.element.getWidth() - 25);
                initPos[1] -= (this.element.getHeight() + tooltip.getHeight() - 4);
                break;
            case 'top_center':
                break;
            case 'top_left':
                break;
            case 'right_bottom':
                initPos[0] += (this.element.getWidth() + 10);
                initPos[1] -= 4;
                break;
            case 'right_center':
                break;
            case 'right_bottom':
                break;
            case 'bottom_right':
                break;
            case 'bottom_center':
                break;
            case 'bottom_left':
                break;
            case 'left_top':
                break;
            case 'left_center':
                break;
            case 'left_bottom':
                break;
        }

        return initPos;
    },

    applyStylesToPointer: function(tooltip) {
        var pointer = tooltip.down('div.tooltip_pointer_body');
        if (pointer) {
            // Set pointer background
            var tooltipBackground = tooltip.getStyle('backgroundColor');
            pointer.setStyle({borderTopColor: tooltipBackground});
        }
    },

    createTooltip: function() {
        var tooltipDiv = new Element("div");

        tooltipDiv.id = this.element.id + "_tooltip";
        tooltipDiv.update(this.content);

        var skin = this.params && this.params.skin ? this.params.skin : 'default';
        var skinClass = "tooltip_skin-" + skin;

        var pointer = this.params && this.params.pointer == false ? false : true;
        if (pointer) {
            var tooltipPointerDiv = new Element("div");
            tooltipPointerDiv.addClassName('tooltip_pointer')
            tooltipPointerDiv.addClassName(skinClass);
            var tooltipPointerBodyDiv = new Element("div");

            tooltipPointerBodyDiv.addClassName('tooltip_pointer_body');
            tooltipPointerDiv.appendChild(tooltipPointerBodyDiv);
            tooltipDiv.appendChild(tooltipPointerDiv);
        }

        if (this.params) {
            if (this.params.width) {
                tooltipDiv.style.width = this.params.width;
            } else if (this.params.maxWidth) {
                tooltipDiv.style.maxWidth = this.params.maxWidth;
            } else {
                tooltipDiv.style.maxWidth = "140px"; // by default
            }
        }

        tooltipDiv.addClassName("tooltip");
        tooltipDiv.addClassName(skinClass);
        tooltipDiv.addClassName("corner_all");
        tooltipDiv.addClassName("shadow_all");

        return tooltipDiv;
    },

    hide: function() {
        var currentTooltip = this.firedTooltips.get(this.element.id);
        if (currentTooltip) {
            this.firedTooltips.unset(this.element.id);
            document.body.removeChild(currentTooltip);

            // Hide iframe in IE 6
            if (this.ie6) {
                var currentIE6Popup = this.firedIE6Popups.get(this.element.id);
                if (currentIE6Popup) {
                    this.firedIE6Popups.unset(this.element.id);
                    this.destroyIE6Popup(currentIE6Popup);
                }
            }
        }
    },

    createIE6Popup: function() {
        var ie6Popup = new Element("iframe");
        ie6Popup.src = "javascript:'<html></html>';";
        ie6Popup.setAttribute('className','tooltip_ie6Popup');
        // Remove iFrame from tabIndex                                        
        ie6Popup.setAttribute("tabIndex", -1);                              
        ie6Popup.scrolling = "no";
        ie6Popup.frameBorder = "0";
        return ie6Popup;
    },

    openIE6Popup: function(ie6Popup, left, top, width, height) {
        if (ie6Popup) {
            ie6Popup.style.left = left;
            ie6Popup.style.top = top;
            ie6Popup.style.width = width;
            ie6Popup.style.height = height;
            ie6Popup.style.display = "block";

            document.body.appendChild(ie6Popup);
        }
    },

    destroyIE6Popup: function(ie6Popup) {
        if (ie6Popup) {
            Element.remove(ie6Popup);
        }
    }

});
