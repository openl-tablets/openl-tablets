/**
 * Date editor.
 *
 * @author Andrey Naumenko
 */

document.write("<script src='" + jsPath + "calendar/YAHOO.js'></script>");
document.write("<script src='" + jsPath + "calendar/event.js'></script>");
document.write("<script src='" + jsPath + "calendar/calendar.js'></script>");
document.write("<script src='" + jsPath + "calendar/calendar_init.js'></script>");
document.write("<link rel='stylesheet' type='text/css' href='" + jsPath + "calendar/calendar.css'></link>");

var DateEditor = Class.create();

DateEditor.prototype = Object.extend(new BaseEditor(), {
  eventHandler : null,

  editor_initialize: function() {
    /*this.tableEditor = tableEditor;
    this.cell = cell;
    this.node = $(document.createElement("input"));
    this.node.setAttribute("type", "text");
    this.node.style.border = "0px none";
    this.node.style.height = (this.cell.offsetHeight - (Prototype.Browser.IE ? 6 : 4)) + "px";

    this.node.style.fontFamily = this.cell.style.fontFamily;
    this.node.style.fontSize = this.cell.style.fontSize;
    this.node.style.fontStyle = this.cell.style.fontStyle;
    this.node.style.fontWeight = this.cell.style.fontWeight;
    this.node.style.textAlign = this.cell.align;

    this.node.style.margin = "0px";
    this.node.style.padding = "0px";
    this.node.style.width = "100%";

    this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
    Event.observe(this.node, "keyup", this.eventHandler);

    this.cell.innerHTML = "";
    this.cell.appendChild(this.node);
    this.node.focus();*/

    if (!window._grid_calendar) _grid_calendar_init();
    var pos = Position.page(this.td)
    pos[1] += Element.Methods.getDimensions(this.td).height;
    window._grid_calendar.render(pos[0], pos[1], this, "5/5/2000");

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
    var z = window._grid_calendar.getSelectedDates()[0];
    return this._date2str(z);
  },

  _2dg : function(v) {
    v = v.toString();
    return (v.length == 1) ? "0" + v : v;
  },

  _date2str : function(z) {
    return ("d/m/y").replace("m", this._2dg((z.getMonth() * 1 + 1))).replace("d", this._2dg(z.getDate())).replace("y", this._2dg((z.getFullYear() * 1)));
  },

  detach: function() {
    var v = this.isCancelled ? this.initialValue : this.getValue();
    this.setTDValue(v);
    window._grid_calendar.hide();
  },

  destroy: function() {
  }
});

TableEditor.Editors["date"] = DateEditor;
