/**
 * Multiple choice editor.
 *
 * @author Aliaksandr Antonik.
 */
var MultiselectEditor = Class.create(BaseEditor, {
    ulElement: null,
    entries: null,
    sortedKeys: null,
    separator: null,

    editor_initialize: function(param) {
        this.sortedKeys = param.choices;
        this.entries = $H();

        // creating containing DIV
        this.node = $(document.createElement("div"));
        this.node.style.position = "absolute";
        var pos = Element.cumulativeOffset(this.td);
        pos[1] += this.td.getHeight();
        this.node.style.left = pos[0] + "px";
        this.node.style.top = pos[1] + "px";
        this.node.zIndex = "10";
        this.node.className = "multiselect_container_outer";

        // creating buttons
        var self = this;
        this.node.innerHTML = '&nbsp;<input type="button" value="Select All"> <input type="button" value="Deselect All"> <input type="button" value="Done">&nbsp;'
        var b1 = this.node.down(), b2 = b1.next(), b3 = b2.next();
        b1.onclick = function() {self.setAllCheckBoxes(true)}
        b2.onclick = function() {self.setAllCheckBoxes(false)}
        b3.onclick = function() {self.doneEdit()}

        // creating inner DIV
        var container = $(document.createElement("div"));
        container.className = "multiselect_container";
        this.node.appendChild(document.createElement("br"))
        this.node.appendChild(container)

        // creating UL HTML element
        this.ulElement = $(document.createElement("ul"));
        container.appendChild(this.ulElement);

        // creating entries
        var pc = param.choices, pd = param.displayValues;
        for (var ind = 0, len = pc.length; ind < len; ++ind) {
            var li = $(document.createElement("li"));
            this.ulElement.appendChild(li);

            li.innerHTML = '<input type="checkbox" name="multiselect_cb">' + pd[ind].escapeHTML();
            this.entries[pc[ind]] = li.down();
        }

        ["click", "dblclick"].each(function (s) {self.stopEventPropogation(s)});
        this.separator = param.separator || ',';
    },

    show: function(value) {
        document.body.appendChild(this.node);
        var entries = this.entries;
        value.split(this.separator).each(function (key) {
            if (key && entries[key]) {
                entries[key].checked = true;
            }
        });
    },

    getValue: function() {
        var entries = this.entries;
        return this.sortedKeys.findAll(function(key) {
            return entries[key].checked}
        ).join(this.separator)
    },

    destroy: function() {
        document.body.removeChild(this.node);
    },

    setAllCheckBoxes: function(value) {
        var entries = this.entries;
        this.sortedKeys.findAll(function(key) {
            entries[key].checked = value;
        });
    }
});

TableEditor.Editors["multiselect"] = MultiselectEditor;