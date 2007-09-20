/**
 * Base class for Editors. If you need to create own layout just overload
 * methods of this class.
 *
 * @requires prototype JavaScript library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create();

BaseEditor.prototype = {
    // -------------------------------------------------------------- Object properties --
    node : null,

    /** Standard constructor. */
    initialize : function() {
        this.node = document.createElement("input");
        this.node.setAttribute("type", "text");
        this.node.size = 40;
    },

    // ----------------------------------------------------------------- Public methods --

    /**
     * Append current instance of multivalued editor into element.
     * element can be String - element id or DOM node.
     */
    create : function(/* String|HTMLElement */ element) {
        element = $(element);
        return (element != null) ? element.appendChild(this.node) : this.node;
    },

    /** Return actual value from editor */
    getValue : function() {
        return (this.node != null) ? this.node.value : null;
    },

    /** Set value into editor. */
    setValue : function(/* String */ value) {
        if (this.node != null)
            this.node.value = value;
    },

    /** Editor specific actions for release of any resources(other actions) before removing
    * editor control from the document.
    */
    destroy : function() {
        // default: do nothing
    },

    /** Clears content in editor or reset his value to default. */
    clear : function() {
        this.node.value = "";
    },

    addOnChangeListener : function(editor, element) {
        /*if (element != null) {
            $(element).observe('change',
                function(event){
                     editor.hasChangesFlag = true;
                });
        }*/
    }
}
