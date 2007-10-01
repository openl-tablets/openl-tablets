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
    editor_initialize: function() {
        // for testing
        if (this.td.innerHTML.length<5) this.td.innerHTML="05/05/2007";

        if (!window._grid_calendar) _grid_calendar_init();
        var pos = Position.page(this.td)
        pos[1] += Element.Methods.getDimensions(this.td).height;
        window._grid_calendar.render(pos[0], pos[1], this, this.td.innerHTML);
        Event.observe(window._grid_calendar.table.parentNode.parentNode, "click", BaseEditor.stopPropagationHandler, false);
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
        return ("m/d/y").replace("m", this._2dg((z.getMonth() * 1 + 1))).replace("d", this._2dg(z.getDate())).replace("y", this._2dg((z.getFullYear() * 1)));
    },

    detach: function() {
        BaseEditor.prototype.detach.apply(this);
        window._grid_calendar.hide();
    }
});

TableEditor.Editors["date"] = DateEditor;
