/**
 * Validation.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Andrei Astrouski
 */
var Validator = Class.create({

    initialize: function(name, errorMessage, validateFunc) {
        this.name = name;
        this.validateFunc = validateFunc;
        this.errorMessage = errorMessage || 'Validation Error';
    },

    validate: function(value, params) {
        this.processMessage(value, params);
        return this.validateFunc(value, params);
    },

    // TODO Refactor to support more than one params
    processMessage: function(value, params) {
        if (/^.*\{\d+\}.*$/.test(this.errorMessage)) {
            if (params.messageParams) {
                value = params.messageParams;
            }
            this.errorMessage = this.errorMessage.replace(/\{\d+\}/, value);
        }
    }

});

Validator.methods = {

    empty: function(value) {
        return !value || value.length == 0;
    },

    alpha: function(value) {
        return value && /^[_\-a-zA-Z\s]*$/.test(value);
    },

    numeric: function(value) {
        return value && /^\-?\d*(\.\d+)?$/.test(value);
    },

    digits: function(value) {
        return value && /^\-?\d*$/.test(value);
    },

    date: function(value) {
        var date = new Date(value);
        return !isNaN(date);
    },

    compareTo: function(value, toValue) {
        if (this.date(value)) {
            value = new Date(value);
            toValue = new Date(toValue);
        } else if (this.digits(value)) {
            value = new Number(value);
            toValue = new Number(toValue);
        }
        if (value > toValue) {
            return 1;
        } else if (value < toValue) {
            return -1;
        }
        return 0;
    },

    lessThan: function(value, params) {
        var toValue = params.compareToFieldId ? $(params.compareToFieldId).value : params;
        if (!toValue) return true;
        var less = this.compareTo(value, toValue) == -1;
        if (less && params.compareToFieldId) {
            Validation.prototype.hideMessage(params.compareToFieldId);
        }
        return less;
    },

    moreThan: function(value, params) {
        var toValue = params.compareToFieldId ? $(params.compareToFieldId).value : params;
        if (!toValue) return true;
        var more = this.compareTo(value, toValue) == 1;
        if (more && params.compareToFieldId) {
            Validation.prototype.hideMessage(params.compareToFieldId);
        }
        return more;
    }
};

var Validation = Class.create({

    validators: $H(),

    firedMessageTips: $H(),

    initialize: function(inputId, validatorName, event, params) {
        this.input = $(inputId);
        this.validatorName = validatorName;
        this.params = params;
        if (event) {
            this.event = event;
            this.eventHandler = this.validate.bindAsEventListener(this);
            Event.observe(this.input, event, this.eventHandler);
        }
    },

    addValidator: function(validatorName, errorMessage, validateFunc) {
        var validator = new Validator(validatorName, errorMessage, validateFunc);
        this.validators.set(validatorName, validator);
    },

    getValidator: function(validatorName) {
        return this.validators.get(validatorName);
    },

    validate: function() {
        var validator = this.getValidator(this.validatorName);
        var inputValue = this.input.value;
        var valid = validator.validate(inputValue, this.params);
        if (!valid) {
            this.showMessage(validator.errorMessage);
        } else {
            this.hideMessage();
        }
        return valid;
    },

    validateAll: function() {
        // TODO Implement
        return false;
    },

    cancel: function() {
        if (this.eventHandler) {
            Event.stopObserving(this.input, this.event, this.eventHandler);
        }
    },

    showMessage: function(message) {
        // TODO Refactor to use Tooltip
        var messageTip = new Element("div");
        messageTip.id = this.input.id + "_message";
        messageTip.innerHTML = message;
        messageTip.style.padding = "3px";
        messageTip.style.background = "rgb(255,180,180)";
        messageTip.style.border = "1px solid rgb(255,100,80)";
        messageTip.style.position = "absolute";
        var pos = Element.viewportOffset(this.input);
        pos[0] += this.input.getWidth();
        pos[1] -= 2;
        messageTip.style.left = pos[0] + "px";
        messageTip.style.top = pos[1] + "px";
        messageTip.zIndex = "100";
        if (!this.firedMessageTips.get(this.input.id)) {
            this.firedMessageTips.set(this.input.id, messageTip);
            document.body.appendChild(messageTip);
        }
    },

    hideMessage: function(id) {
        if (!id) id = this.input.id; // Current input id
        var messageTip = this.firedMessageTips.get(id);
        if (messageTip) {
            this.firedMessageTips.unset(id);
            document.body.removeChild(messageTip);
        }
    },

    hideAllMessages: function() {
        // TODO Implement
    }

});

Validation.isAllValidated = function() {
    return Validation.prototype.firedMessageTips.keys().length == 0;
};

Validation.add = function(a, b, c) {
    Validation.prototype.addValidator(a, b, c);
};

Validation.add('required', 'This field is required', function(value, params) {
    return !Validator.methods.empty(value);
});
Validation.add('alpha', 'This field must contain only letters', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.alpha(value);
});
Validation.add('numeric', 'This field must contain only numbers', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.numeric(value);
});
Validation.add('digits', 'This field must contain only digits', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.digits(value);
});
Validation.add('date', 'Please enter a valid date', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.date(value);
});
Validation.add('lessThan', 'Value of this field must be less than {0}', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.lessThan(value, params);
});
Validation.add('moreThan', 'Value of this field must be more than {0}', function(value, params) {
    return Validator.methods.empty(value) || Validator.methods.moreThan(value, params);
});
