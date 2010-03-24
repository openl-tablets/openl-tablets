/**
 * Base class for Editors. If you need to create your own editor just override
 * methods of this class.
 *
 * @requires Prototype library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create({
// -------------------------------------------------------------- Object properties --
    tableEditor : null,
    parentElement : null,
    input : null,
    initialValue : null,
    stoppedEvents : null,
    focus : null,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. saves initial cell value into initialValue variable
     *   2. creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, parentId, params, initialValue, focus) {
        if (parentId) {
            this.tableEditor = tableEditor;
            this.parentElement = $(parentId);
    
            // save initial value
            this.initialValue = initialValue ? initialValue
                    : AjaxHelper.unescapeHTML(this.parentElement.innerHTML.replace(/<br>/ig, "\n")).strip();
    
            this.editor_initialize(params);
            this.input.id = this.getId();
            this.focus = (focus && focus == true) ? focus : '';
            this.show(this.initialValue);
        }
    },
// ----------------------------------------------------------------- Public methods --

    /** Obtains current value from HTML editor control */
    getValue : function() {
        var input = this.getInputElement();
        return input ? AjaxHelper.getInputValue(input).toString().replace(/\u00A0/g, ' ') : null;
    },

    /**
     * Destroys HTML editor control, writes value to cell.
     */
    setTDValue : function(/* String */ value) {
        if (!value.strip()) {
          this.parentElement.innerHTML = "&nbsp";
        } else
            this.parentElement.innerHTML = value.escapeHTML().replace(/\n/g, "<br>");
    },

    /** Returns if the editing was cancelled */
    isCancelled : function() {
        return (this.initialValue == this.getValue());
    },

    /**
     * Destroys HTML editor control, writes value to cell, releases editor resources.
     */
    detach : function() {
        var v = this.isCancelled() ? this.initialValue : this.getValue();
        this.setTDValue(v);
        if (this.stoppedEvents) {
            var input = this.input;
            this.stoppedEvents.each(function(evt) {
                Event.stopObserving(input, evt, BaseEditor.stopPropagationHandler)
            })
        }
        this.destroy();
    },

    doSwitching: function(newEditor) {
        var value = this.isCancelled() ? this.initialValue : this.getValue();
        newEditor.tableEditor = this.tableEditor;
        newEditor.parentElement = this.parentElement;
        newEditor.initialValue = this.initialValue;

        this.isCancelled = BaseEditor.T;
        this.detach();

        newEditor.editor_initialize();
        newEditor.show(value);
    },

    /**
     *  Editor specific constructor. Typically HTML node is created and possible some events handlers are registered.
     */
    editor_initialize: Prototype.emptyFunction,

    /**
     * Is responsible for making editor visible and active. In most cases it is not needed to be overridden.
     */
    show: function(value) {
        if (this.input) {
            AjaxHelper.setInputValue(this.getInputElement(), value);
            this.parentElement.innerHTML = "";
            this.parentElement.appendChild(this.input);
            if (this.focus) {
                this.input.focus();
            }
        }
    },

    /**
     *  Stops given event propogation up to parent elements.
     *  Remembers information required for automatic unregistering listeners on destruction.
     */
    stopEventPropogation: function(name) {
        if (!this.stoppedEvents) this.stoppedEvents = [];
        this.stoppedEvents.push(name);
        this.input.observe(name, BaseEditor.stopPropagationHandler, false);
    },

    /**
     * Can be overridden in editors to clean up resources
     */
    destroy: Prototype.emptyFunction,

    getId: function() {
        return '_' + this.parentElement.id;
    },

    /** Handles F2 press */
    handleF2: Prototype.emptyFunction,
    /** Handles F3 press */
    handleF3: Prototype.emptyFunction,

// ----------------------------------------------------------------- Protected methods --

    /** Notifies table editor that editing is finished */
    doneEdit: function() {
        this.tableEditor.setCellValue();
    },

    /** Notifies table editor that editing is finished and cancelled */
    cancelEdit: function() {
        this.isCancelled = BaseEditor.T;
        this.doneEdit();
    },

    switchTo: function(editorName) {
        this.tableEditor.switchEditor(editorName);
    },

    /**
     *  Turns the editor into 'invalid' state. Sets "editor_invalid" CSS style to the input element.
     */
    markInvalid: function() {this.getInputElement().addClassName('editor_invalid')},

    /**
     * Removes "editor_invalid" CSS style for the input element.
     */
    markValid: function() {this.getInputElement().removeClassName('editor_invalid')},

    /**
     *  Returns HTML element which is actually main input element for this editor.
     */
    getInputElement: function() {return this.input}
});

/**
 *  A useful function
 */
BaseEditor.T = function() {return true}

/**
 * Prevents propogation of a javascript event up the DOM hierarchy in browser specific manner.
 */
BaseEditor.stopPropagationHandler = function(e) {
    e = e || event;
    if (e.stopPropagation) {
        e.stopPropagation();
    } else {
        e.cancelBubble = true;
    }
}
