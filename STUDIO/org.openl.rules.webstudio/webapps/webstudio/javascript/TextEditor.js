/**
 * Text editor.
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create();

TextEditor.prototype = Object.extend(new BaseEditor(), {
  eventHandler : null,

  editor_initialize: function() {
    this.node = $(document.createElement("input"));
    this.node.setAttribute("type", "text");
    this.node.style.border = "0px none";
    this.node.style.height = (this.td.offsetHeight - (Prototype.Browser.IE ? 6 : 4)) + "px";

    this.node.style.fontFamily = this.td.style.fontFamily;
    this.node.style.fontSize = this.td.style.fontSize;
    this.node.style.fontStyle = this.td.style.fontStyle;
    this.node.style.fontWeight = this.td.style.fontWeight;
    this.node.style.textAlign = this.td.align;

    this.node.style.margin = "0px";
    this.node.style.padding = "0px";
    this.node.style.width = "100%";

    this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
    Event.observe(this.node, "keyup", this.eventHandler);

    this.td.innerHTML = "";
    this.td.appendChild(this.node);
    this.node.focus();

    //Event.observe(this.node, "click", function(e) {(e || event).cancelBubble = true});
    //Event.observe(this.node, "mousedown", function(e) {(e || event).cancelBubble = true});
  },

  handleKeyPress: function (event) {
    switch (event.keyCode) {
      case 27: this.cancelEdit(); break;
      case 13: this.doneEdit(); break;
    }
  },

  destroy: function() {
    Event.stopObserving(this.node, "keyup", this.eventHandler);
  },

  detach: function() {
    this.editorContainer.innerHTML = this.node.value;
  }
});

TableEditor.Editors["text"] = TextEditor;
