/**
 * Base class for Editors. If you need to create your own editor just override
 * methods of this class.
 *
 * @requires Prototype library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create();

/**
 * Exteding default implementation of the function for IE and WebKit
 */
if (Prototype.Browser.WebKit || Prototype.Browser.IE) Object.extend(String.prototype, {
  unescapeHTML: function() {
    return this.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&nbsp;/g,' ');
  }
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

BaseEditor.prototype = {
// -------------------------------------------------------------- Object properties --
    tableEditor : null,
    node : null,
    td : null,
    initialValue : null,
    stoppedEvents : null,

    MAX_FIELD_SIZE : 1500,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. saves initial cell value into initialValue variable
     *   2. creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, td, param, typedText) {
        if (tableEditor) {
            this.tableEditor = tableEditor;
            this.td = td;

            // save initial value
            this.initialValue = this.td.innerHTML.replace(/<br>/ig, "\n").unescapeHTML().strip();

            this.editor_initialize(param);
            this.show(typedText ? typedText : this.initialValue);
        }
    },
// ----------------------------------------------------------------- Public methods --

    /** Obtains current value from HTML editor control */
    getValue : function() {
        var node = this.getInputElement();
        return node ? node.value : null;
    },

    /**
     * Destroys HTML editor control, writes value to cell.
     */
    setTDValue : function(/* String */ value) {
        if (!value.strip()) {
          this.td.innerHTML = "&nbsp";
        } else
            this.td.innerHTML = value.escapeHTML().replace(/\n/g, "<br>");
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
            var node = this.node;
            this.stoppedEvents.each(function(evt) {
                Event.stopObserving(node, evt, BaseEditor.stopPropagationHandler)
            })
        }
        this.destroy();
    },

    doSwitching: function(newEditor) {
        var value = this.isCancelled() ? this.initialValue : this.getValue();
        newEditor.tableEditor = this.tableEditor;
        newEditor.td = this.td;
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
        if (this.node) {
            this.getInputElement().value = value;
            this.td.innerHTML = "";
            this.td.appendChild(this.node);
            this.node.focus();
        }
    },

    /**
     *  Stops given event propogation up to parent elements.
     *  Remembers information required for automatic unregistering listeners on destruction.
     */
    stopEventPropogation: function(name) {
        if (!this.stoppedEvents) this.stoppedEvents = [];
        this.stoppedEvents.push(name);
        this.node.observe(name, BaseEditor.stopPropagationHandler, false);
    },

    /**
     * Can be overridden in editors to clean up resources
     */
    destroy: Prototype.emptyFunction,

    /** Handles F2 press */
    handleF2: Prototype.emptyFunction,
    /** Handles F3 press */
    handleF3: Prototype.emptyFunction,

// ----------------------------------------------------------------- Protected methods --

    /** Notifies table editor that editing is finished */
    doneEdit: function() {
        this.tableEditor.editStop();
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
     *  Returns HTML element which is actually main input element for this editor. It will "this.node" for most editors.
     */
    getInputElement: function() {return this.node}
}
