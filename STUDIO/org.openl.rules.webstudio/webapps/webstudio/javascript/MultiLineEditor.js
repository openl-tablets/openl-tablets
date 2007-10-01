/**
 * Text editor.
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create();

MultiLineEditor.prototype = Object.extend(new BaseEditor(), {
  eventHandler : null,

  editor_initialize: function() {
    this.node = document.createElement("div");
    //this.node.setAttribute("type", "text");
    //this.node.style.border = "0px none";
    //this.node.style.height = (this.cell.offsetHeight - (Prototype.Browser.IE ? 6 : 4)) + "px";

    //this.node.style.fontFamily = this.cell.style.fontFamily;
    //this.node.style.fontSize = this.cell.style.fontSize;
    //this.node.style.fontStyle = this.cell.style.fontStyle;
    //this.node.style.fontWeight = this.cell.style.fontWeight;
    //this.node.style.textAlign = this.cell.align;

    //this.node.style.margin = "0px";
    //this.node.style.padding = "0px";
    //this.node.style.width = "100%";
    var ta = document.createElement("textarea");
    ta.cols = 30;
    ta.rows = 3;
    this.node.appendChild(ta);

    this.node.style.position = "absolute";

    var pos = Position.page(this.td);
    pos[1] += Element.Methods.getDimensions(this.td).height;

    this.node.style.left = pos[0] + "px";
    this.node.style.top = pos[1] + "px";
    this.node.zIndex = "10";

    ta.value = this.td.innerHTML.strip();

    document.body.appendChild(this.node);
    ta.focus();

    //this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
    //Event.observe(this.node, "keyup", this.eventHandler);

    //Event.observe(this.node, "click", function(e) {(e || event).cancelBubble = true});
    //Event.observe(this.node, "mousedown", function(e) {(e || event).cancelBubble = true});
  },

  handleKeyPress: function (event) {
    switch (event.keyCode) {
      case 27: this.cancelEdit(); break;
      case 13: this.doneEdit(); break;
    }
  },

  getValue : function() {
    return this.node.firstChild.value;
  },

  detach: function() {
    var v = this.isCancelled ? this.initialValue : this.getValue();
    this.setTDValue(v);
    document.body.removeChild(this.node);
  },

  destroy: function() {
    Event.stopObserving(this.node, "keyup", this.eventHandler);
  }
});

TableEditor.Editors["multilineText"] = MultiLineEditor;
