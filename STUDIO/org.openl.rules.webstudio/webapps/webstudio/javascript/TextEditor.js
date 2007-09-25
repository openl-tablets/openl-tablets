/**
 * Base class for text based editors.
 *
 * @author Andrey Naumenko
 */
var TextEditor = Prototype.emptyFunction;

TextEditor.prototype = Object.extend(new BaseEditor(), {
	  eventHandler : null,
	 /** Constructor */
    initialize : function() {
		  this.node = $(document.createElement("input"));
        this.node.setAttribute("type", "text");
        this.node.style.border = "0px none";
        this.node.style.height = "100%";
        this.node.style.margin = "0px";
        this.node.style.padding = "0px";
        this.node.style.width = "100%";

		 this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
		 Event.observe(this.node, "keyup", this.eventHandler);
	 },

	 handleKeyPress: function (event) {
		 switch (event.keyCode) {
		 case 27: this.cancelEdit(); break;
		 case 13: this.doneEdit(); break;
		 }
	 },

	 destroy: function() {
		 Event.stopObserving(this.node,  "keyup", this.eventHandler);
	 }
});

TableEditor.Editors["inputbox"] = TextEditor.prototype;
