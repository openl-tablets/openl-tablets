/**
 * Date editor.
 *
 * Requires a part of some old version of YAHOO UI library.
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
        var value =  this.td.innerHTML.unescapeHTML().strip();

        if (!window._grid_calendar) _grid_calendar_init();
        var pos = Position.page(this.td)
        pos[1] += Element.Methods.getDimensions(this.td).height;
        window._grid_calendar.render(pos[0], pos[1], this, value);
        Event.observe(window._grid_calendar.table.parentNode.parentNode, "click", BaseEditor.stopPropagationHandler, false);
    },

    show : Prototype.emptyFunction,

    getValue : function() {
        var z = window._grid_calendar.getSelectedDates()[0];
        return this._date2str(z);
    },

    _2dg : function(v) {
        v = v.toString();
        return (v.length == 1) ? "0" + v : v;
    },

    _date2str : function(z) {
        return ("m/d/y").replace("m", this._2dg((z.getMonth() + 1))).replace("d", this._2dg(z.getDate())).replace("y", this._2dg((z.getFullYear() * 1)));
    },

    destroy: function() {
        window._grid_calendar.hide();
        Event.stopObserving(window._grid_calendar.table.parentNode.parentNode, "click", BaseEditor.stopPropagationHandler);
    }
});

TableEditor.Editors["date"] = DateEditor;
