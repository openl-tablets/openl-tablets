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
        this.element = $(id);
        this.content = content;
        this.params = params;

        this.showOn = params && params.showOn ? (params.showOn == false ? '' : params.showOn) : "mouseover";
        this.hideOn = params && params.hideOn ? (params.hideOn == false ? '' : params.hideOn) : "mouseout";

        if (this.showOn) {
            this.showHandler = this.show.bindAsEventListener(this);
            Event.observe(this.element, this.showOn, this.showHandler);
        }
        if (this.hideOn) {
            this.hideHandler = this.hide.bindAsEventListener(this);
            Event.observe(this.element, this.hideOn, this.hideHandler);
        }
    },

    show: function() {
        var tooltip = this.createTooltip();
        if (!this.firedTooltips.get(this.element.id)) {
            // Show tooltip
            document.body.appendChild(tooltip);
            this.firedTooltips.set(this.element.id, tooltip);

            this.applyStylesToPointer(tooltip);

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

    applyStylesToPointer: function(tooltip) {
        var pointer = tooltip.down('div.tooltip_pointer_down_body');
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

        var pointer = this.params && this.params.pointer
            ? (this.params.pointer == false ? '' : this.params.pointer) : 'left';
        var pointerClass = '';
        if (pointer) {
            switch (pointer) {
                case 'left':
                    pointerClass = 'tooltip_left';
                    break;
                case 'center':
                    pointerClass = 'tooltip_center';
                    break;
                case 'right':
                    pointerClass = 'tooltip_right';
                    break;
            }

            var tooltipPointerDiv = new Element("div");
            tooltipPointerDiv.addClassName('tooltip_pointer_down')
            tooltipPointerDiv.addClassName(skinClass);
            var tooltipPointerBodyDiv = new Element("div");

            tooltipPointerBodyDiv.addClassName('tooltip_pointer_down_body');
            tooltipPointerDiv.appendChild(tooltipPointerBodyDiv);
            tooltipDiv.appendChild(tooltipPointerDiv);
        }

        tooltipDiv.addClassName("tooltip");
        tooltipDiv.addClassName(skinClass);
        tooltipDiv.addClassName(pointerClass);
        tooltipDiv.addClassName("corner_all");
        tooltipDiv.addClassName("shadow_all");

        var pos = Element.cumulativeOffset(this.element);
        pos[0] += this.element.getWidth() - 25;
        pos[1] -= (this.element.getHeight() + 10 + (this.content.length / 2));
        tooltipDiv.style.left = pos[0] + "px";
        tooltipDiv.style.top = pos[1] + "px";

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
