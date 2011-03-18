/**
 * Button library.
 * 
 * @requires Prototype 1.6.1+
 * @version 0.5
 * 
 * @author Andrei Astrouski
 */
var Button = Class.create({

    initialize: function(id) {
        var button = $(id);

        button.addClassName('component button state_default corner');

        ['mouseover', 'mouseout'].each(function(event) {
            button.observe(event, function() {
                this.toggleClassName('state_hover');
                this.removeClassName('state_active');
            });
        });
        ['mousedown', 'mouseup'].each(function(event) {
            button.observe(event, function() {
                this.toggleClassName('state_active');
            });
        });
    }

});

var ToggleButton = Class.create({

    initialize: function(id, checked) {
        var button = $(id);

        button.addClassName('component button state_default corner');
        if (checked) {
            button.addClassName('state_active');
        }

        ['mouseover', 'mouseout'].each(function(event) {
            button.observe(event, function() {
                this.toggleClassName('state_hover');
            });
        });
        ['click'].each(function(event) {
            button.observe(event, function() {
                this.toggleClassName('state_active');
            });
        });
    }

});

var RadioButtonSet = Class.create({

    checkedButton: null,

    initialize: function(ids, checkedId) {
        var buttons = [];

        ids.each(function(id, index) {
            buttons[index] = $(id);
        });

        this.checkedButton = $(checkedId);

        buttons.each(function(button) {
            button.addClassName('component button state_default corner');
        });

        if (this.checkedButton) {
            this.checkedButton.addClassName('state_active');
        }

        var _this = this;
        buttons.each(function(button) {
            ['mouseover', 'mouseout'].each(function(event) {
                button.observe(event, function() {
                    this.toggleClassName('state_hover');
                });
            });
            ['click'].each(function(event) {
                button.observe(event, function() {
                    if (_this.checkedButton) {
                        _this.checkedButton.removeClassName('state_active');
                    }
                    this.addClassName('state_active');
                    _this.checkedButton = button;
                });
            });

        });
    }

});
