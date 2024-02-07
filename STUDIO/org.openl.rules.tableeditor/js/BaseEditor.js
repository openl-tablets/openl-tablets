/**
 * Base class for Editors.
 * If you need to create your own editor just override methods of this class.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create({

    tableEditor: null,
    parentElement: null,
    input: null,
    params: null,
    initialValue: null,
    stoppedEvents: null,
    focussed: null,
    style: null,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. Saves initial cell value into initialValue variable
     *   2. Creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, parentId, params, initialValue, focussed, style) {
        if (parentId) {
            this.tableEditor = tableEditor;
            this.parentElement = $(parentId);

            this.style = style;

            this.initialValue = initialValue;

            this.params = params;
            this.editor_initialize(params);
            this.input.id = this.getId();
            this.focussed = (focussed && focussed == true) ? focussed : '';
            this.show(this.initialValue);
        }
    },

    /**
     *  Editor specific constructor.
     *  Typically HTML node is created and possible some events handlers are registered.
     */
    editor_initialize: Prototype.emptyFunction,

    /**
     * Obtains current value from HTML editor control.
     */
    getValue: function() {
        return this.input ? this.input.value.toString().replace(/\u00A0/g, ' ') : null;
    },

    setValue: function(value) {
        this.input.value = value;
    },

    getDisplayValue: function() {
        var value = this.isCancelled() ? this.initialValue : this.getValue();
        if (!value.strip()) {
            value = "&nbsp";
        } else {
            value = value.escapeHTML().replace(/\n/g, "<br/>");
        }
        return value;
    },

    /**
     * Is responsible for making editor visible and active.
     * In most cases it is not needed to be overridden.
     */
    show: function(value) {
        if (this.input) {
            this.parentElement.innerHTML = "";
            this.parentElement.appendChild(this.input);
            this.setValue(value);
            if (this.focussed) {
                this.focus();
            }
        }
    },

    focus: function() {
        this.input.focus();
    },

    /**
     * Returns if the editing was cancelled.
     */
    isCancelled : function() {
        return (this.initialValue == this.getValue() || !this.isValid(this.getValue()));
    },

    switchTo: function(editorName) {
        this.tableEditor.switchEditor(editorName);
    },

    /**
     * Can be overridden in editors to clean up resources.
     */
    destroy: function() {
        this.unbind();
    },

    getId: function() {
        return '_' + this.parentElement.id;
    },

   /**
     * Notifies table editor that editing is finished.
     */
    doneEdit: function() {
        this.tableEditor.setCellValue();
    },

    /**
     * Notifies table editor that editing is finished and canceled.
     */
    cancelEdit: function() {
        this.isCancelled = BaseEditor.T;
        this.doneEdit();
    },

    /**
     *  Returns HTML element which is actually main input element for this editor.
     */
    getInputElement: function() {
        return this.input;
    },

    is: function(element) {
        return element == this.getInputElement();
    },

    bind: function(event, handler) {
        Event.observe(this.getInputElement(), event, handler);
    },

    unbind: function(event, handler) {
        Event.stopObserving(this.getInputElement(), event, handler);
    },

    /**
     * Validates input value.
     */
    isValid: function(value) {
        return true;
    }

});

BaseEditor.T = function() {
    return true;
}

BaseEditor.isTableEditorExists = function() {
    return typeof TableEditor != 'undefined';
}
