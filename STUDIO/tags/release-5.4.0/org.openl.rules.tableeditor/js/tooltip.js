
var Tooltip = Class.create({

    firedTooltips: $H(),

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
        var tooltipDiv = this.createTooltip();
        if (!this.firedTooltips.get(this.element.id)) {
            this.firedTooltips.set(this.element.id, tooltipDiv);
            document.body.appendChild(tooltipDiv);
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
            tooltipPointerDiv.className = 'tooltip_pointer_down ' + skinClass;
            var tooltipPointerBodyDiv = new Element("div");

            tooltipPointerBodyDiv.className = 'tooltip_pointer_down_body';
            tooltipPointerDiv.appendChild(tooltipPointerBodyDiv);
            tooltipDiv.appendChild(tooltipPointerDiv);
        }

        var styleClasses = "tooltip corner_all " + skinClass + " " + pointerClass;
        tooltipDiv.className = styleClasses;

        // set pointer background
        tooltipPointerBodyDiv.setStyle({borderTopColor: tooltipDiv.getStyle('backgroundColor')});

        var pos = Element.cumulativeOffset(this.element);
        pos[0] += this.element.getWidth() - 25;
        pos[1] -= (this.element.getHeight() + 10 + (this.content.length / 2));
        tooltipDiv.style.left = pos[0] + "px";
        tooltipDiv.style.top = pos[1] + "px";

        return tooltipDiv;
    },

    hide: function() {
        var currenTooltip = this.firedTooltips.get(this.element.id);
        if (currenTooltip) {
            this.firedTooltips.unset(this.element.id);
            document.body.removeChild(currenTooltip);
        }
    }

});
