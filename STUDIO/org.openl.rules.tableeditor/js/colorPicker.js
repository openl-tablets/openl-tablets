/**
 * Color Picker.
 * 
 * @requires Prototype v1.6.1+ library
 *  
 * @author Andrei Astrouski
 */
var ColorPicker = Class.create({

    palette: [['#FFFFFF', '#FFDDDD', '#DDFFDD', '#DDDDFF', '#FFFFAA', '#FFE4BE', '#FFDDFF', '#FFD6AF', '#E5F5FF', '#DBFFEB'],
              ['#EEEEEE', '#FFAAAA', '#AAFFAA', '#AAAAFF', '#FFFF77', '#FFC29C', '#FFBBFF', '#FFBF80', '#D2EDFF', '#C2FFDD'],
              ['#CCCCCC', '#FF6666', '#66FF66', '#6666FF', '#FFFF33', '#FF9275', '#F19CEC', '#FAA857', '#AEDEFE', '#8FFFC0'],
              ['#999999', '#FF3333', '#45F745', '#3333FF', '#FFFF00', '#FF7256', '#CD69C9', '#FF7F00', '#87CEFF', '#54FF9F'],
              ['#666666', '#EA0B0B', '#25DA25', '#2222D3', '#EEEE00', '#EE6A50', '#B23AEE', '#EE7600', '#7EC0EE', '#4EEE94'],
              ['#333333', '#AA0000', '#00AA00', '#1717AB', '#CDCD00', '#CD5B45', '#9A32CD', '#CD6600', '#6CA6CD', '#43CD80'],
              ['#000000', '#660000', '#006600', '#000066', '#8B8B00', '#8B3E2F', '#68228B', '#8B4500', '#4A708B', '#2E8B57']],

    colorPicker: null,
    documentClickListener: null,
    opened: false,

    initialize: function(id, parent, onSelect, optParams) {
        this.actionElement = $(id);
        this.onSelect = onSelect;
        this.parent = parent;
        this.optParams = optParams;

        if (this.optParams.showOn != false && !this.optParams.showOn) {
            this.showOn = 'click';
        }

        if (this.showOn) {
            this.showHandler = this.show.bindAsEventListener(this);
            Event.observe(this.actionElement, this.showOn, this.showHandler);
        }

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);
    },

    show: function() {
        var self = this;

        if (self.optParams.onShow) {
            if (self.optParams.onShow() === false) {
                return;
            }
        }

        if (!this.opened) {
            if (!this.colorPicker) {
                this.colorPicker = this.createColorPicker();
            }

            // Show Color Picker
            this.parent.appendChild(this.colorPicker);

            if (self.optParams.onMouseOver) {
                this.colorPicker.observe("mouseover", function(e) {
                    self.optParams.onMouseOver();
                });
            }
            if (self.optParams.onMouseOut) {
                this.colorPicker.observe("mouseout", function(e) {
                    self.optParams.onMouseOut();
                });
            }

            Event.observe(document, 'click', this.documentClickListener);

            $$("#" + this.actionElement.id + "_colorPicker .cp_palette table td div").each(function(elem) {
                elem.observe("mouseover", function(e) {
                    this.addClassName("cp_selected");
                    if (self.optParams.onColorMouseOver) {
                        self.optParams.onColorMouseOver(self.toRgb(this.style.backgroundColor));
                    }
                });
                elem.observe("mouseout", function(e) {
                    this.removeClassName("cp_selected");
                    if (self.optParams.onColorMouseOut) {
                        self.optParams.onColorMouseOut();
                    }
                });
                elem.observe("click", function(e) {
                    this.removeClassName("cp_selected");
                    self.hide();
                    self.onSelect(self.toRgb(this.style.backgroundColor));
                });
            });

            this.opened = true;
        }
    },

    documentClickHandler: function(e) {
        var self = this;

        var element = Event.element(e);

        var b = false;
        if (element == this.actionElement) {
            b = true;
        } else {
            do {
                if (element == this.multiselectPanel) {
                    b = true;
                }
            } while (element = element.parentNode);
        }

        if (!b) {
            if (self.optParams.onCancel) {
                if (self.optParams.onCancel() === false) {
                    return;
                }
            }
            this.hide();
        }
    },

    getInitPosition: function() {
        var pos = Element.positionedOffset(this.actionElement);
        pos[1] += this.actionElement.getHeight();
        return pos;
    },

    createColorPicker: function() {
        var colorPickerDiv = new Element("div");

        colorPickerDiv.id = this.actionElement.id + "_colorPicker";
        colorPickerDiv.update(this.createColorPalette());

        var pos = this.getInitPosition();
        colorPickerDiv.style.left = pos[0] + "px";
        colorPickerDiv.style.top = pos[1] + "px";

        colorPickerDiv.addClassName("colorPicker");
        colorPickerDiv.addClassName("corner_all");
        colorPickerDiv.addClassName("shadow_all");

        return colorPickerDiv;
    },

    createColorPalette: function() {
        var nRows = this.palette.length;
        var nCols = this.palette[0].length;
        var paletteHtml = "<div class='cp_palette'><table>";

        for (var row = 0; row < nRows; row++) {
            paletteHtml += "<tr>";
            for (var col = 0; col < nCols; col++) {
                paletteHtml += "<td><div style='background: " + this.palette[row][col] + "'>";
                paletteHtml += "</div></td>";
            }
            paletteHtml += "</tr>";
        }

        paletteHtml += "</table></div>";

        return paletteHtml;
    },

    hide: function() {
        var self = this;

        if (self.optParams.onHide) {
            if (self.optParams.onHide() === false) {
                return;
            }
        }

        if (this.opened) {
            Event.stopObserving(document, 'click', this.documentClickListener);
            Element.remove(this.colorPicker);
            this.opened = false;
        }
    },

    toRgb: function(color) {
        // rgb
        if (color.indexOf("rgb") == 0) {
            return color;
        }

        // hex
        if (color.indexOf("#") == 0) {
            color = color.substr(1);
        }

        var triplets = /^([a-f0-9]{2})([a-f0-9]{2})([a-f0-9]{2})$/i.exec(color).slice(1);

        var red = parseInt(triplets[0], 16);
        var green = parseInt(triplets[1], 16);
        var blue = parseInt(triplets[2], 16);

        return "rgb(" + red + "," + green + "," + blue + ")";
    }

});
