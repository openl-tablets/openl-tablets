var Validator = Class.create({

    initialize: function(name, errorMessage, validateFunc) {
        this.name = name;
        this.validateFunc = validateFunc;
        this.errorMessage = errorMessage || 'Validation Error';
    },

    validate: function(value, params) {
        return this.validateFunc(value, params);
    },

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
}

var Validation = Class.create({

    validators: $H(),

    firedMessageTips: $H(),

    initialize: function(inputId, validatorName, event, params) {
        this.input = $(inputId);
        this.validatorName = validatorName;
        this.event = event;
        this.params = params;
        if (event) {
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
        var result = validator.validate(inputValue, this.params);
        if (!result) {
            this.showMessage(validator.errorMessage);
        } else {
            this.hideMessage();
        }
        return result;
    },

    cancel: function() {
        if (this.eventHandler) {
            Event.stopObserving(this.input, this.event, this.eventHandler);
        }
    },

    showMessage: function(message) {
        var messageTip = $(document.createElement("div"));
        messageTip.id = this.input.id + "_message";
        messageTip.innerHTML = message;
        messageTip.style.padding = "3px";
        messageTip.style.background = "rgb(255,180,180)";
        messageTip.style.border = "1px solid rgb(255,100,80)";
        messageTip.style.position = "absolute";
        var pos = Element.cumulativeOffset(this.input);
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

    hideMessage: function() {
        var currenMessageTip = this.firedMessageTips.get(this.input.id);
        if (currenMessageTip) {
            this.firedMessageTips.unset(this.input.id);
            document.body.removeChild(currenMessageTip);
        }
    }

});

Validation.add = function(a, b, c) {
    Validation.prototype.addValidator(a, b, c);
}

Validation.add('required', 'This field is required', function(value, params) {
    return !Validator.methods.empty(value);
});
Validation.add('alpha', 'This field must contain only letters', function(value, params) {
    return Validator.methods.alpha(value);
});
Validation.add('numeric', 'This field must contain only numbers', function(value, params) {
    return Validator.methods.numeric(value);
});
Validation.add('digits', 'This field must contain only digits', function(value, params) {
    return Validator.methods.digits(value);
});
