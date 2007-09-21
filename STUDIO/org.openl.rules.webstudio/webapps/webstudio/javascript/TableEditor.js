/**
 * Table editor.
 *
 * @requires prototype JavaScript library
 *
 * @author Andrey Naumenko
 */

var TableEditor = Class.create();

TableEditor.prototype = {
  tableContainer : null,
  currentElement : null,
  editor : null,
  saveUrl : null,

/** Constructor */
  initialize : function(id, url) {
    this.tableContainer = document.getElementById(id);
    this.loadData(url);
  },

/**
 * @desc: load data from specific url
 * @type: private
 */
  loadData : function(url) {
    var self = this;
    new Ajax.Request(url, {
      method      : "get",
      encoding    : "utf-8",
      contentType : "text/javascript",
      onSuccess   : function(data) {
        self.renderTable(data);
      }
    });
  },

/**
 * @desc: renders table
 * @type: private
 */
  renderTable : function(data) {
    this.tableContainer.innerHTML = data.responseText;
    var table = Prototype.Browser.IE ? this.tableContainer.childNodes[0] : this.tableContainer.childNodes[1];
    var self = this;

    $(document.body).observe("click", function(e) { self.editStop() }, false)
    $(table).observe("click", function(e) { Event.stop(e)})
    $(table).observe("dblclick", function(e) { self.handleDoubleClick(e); Event.stop(e)})
  },

/**
 * @desc: handles mouse double click on table
 * @type: private
 */
  handleDoubleClick: function(e) {
    var targetElement = Prototype.Browser.IE ? window.event.srcElement : e.target;

    // Save value of current editor and close it
    this.editStop();

   // Create and activate new editor
   // now Text and Dropdown editors are created in turn
   this.editor = this.editor && this.editor.initValue ?
                 new TextEditor(targetElement.innerHTML) :
                 new DropdownEditor(targetElement.innerHTML);
   this.editor.setValue(targetElement.innerHTML);
   this.currentElement = targetElement;
    targetElement.removeChild(targetElement.childNodes[0]);
    targetElement.appendChild(this.editor.node);
    this.editor.node.focus();
  },

  editStop : function() {
    if (this.editor!=null) {
      new Ajax.Request(this.saveUrl + '?id=' + this.currentElement.title + '&value=' + this.editor.getValue(), {
        method      : "get",
        encoding    : "utf-8",
        contentType : "text/javascript",
        onSuccess   : function(data) {
          //alert('saved !');
        }
      });

      this.currentElement.innerHTML = this.editor.getValue();
    }
  }
}