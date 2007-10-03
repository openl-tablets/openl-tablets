/**
 * Base class for Editors. If you need to create own layout just override
 * methods of this class.
 *
 * @requires Prototype library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create();

if (Prototype.Browser.WebKit || Prototype.Browser.IE) Object.extend(String.prototype, {
  unescapeHTML: function() {
    return this.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&nbsp;/g,' ');
  }
});

BaseEditor.T = function() {return true}

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
    stoppedEvents : [],

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. saves initial cell value into initialValue variable
     *   2. creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, td, param) {
        this.tableEditor = tableEditor;
        this.td = td;
        if (td) {
            // save initial value
            this.initialValue = this.td.innerHTML.unescapeHTML().strip();
        }
        this.editor_initialize(param);
        this.show(this.initialValue);
    },

    /**
     *  Editor specific constructor. Typically HTML node is created and possible some events handlers are registered.
     */
    editor_initialize: Prototype.emptyFunction,

    /**
     * Is responsible for making editor visible and active. In most cases it is not need to be overridden.
     */
    show: function(value) {
        if (this.node) {
            this.node.value = value;
            this.td.innerHTML = "";
            this.td.appendChild(this.node);
            this.node.focus();
        }
    },

    /** Stops given event propogation up to parent elements */
    stopEventPropogation: function(name) {
        this.stoppedEvents.push(name);
        this.node.observe(name, BaseEditor.stopPropagationHandler, false);
    },
// ----------------------------------------------------------------- Public methods --

    /** Obtains current value from HTML editor control */
    getValue : function() {
        return this.node ? this.node.value : null;
    },

    /**
     * Destroys HTML editor control, writes value to cell.
     */
    setTDValue : function(/* String */ value) {
        if (!value.strip()) {
          value = "&nbsp";
        }
        this.td.innerHTML = value;
    },

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
    }
}
