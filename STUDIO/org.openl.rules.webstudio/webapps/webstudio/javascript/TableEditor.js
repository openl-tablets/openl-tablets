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
  currentEditor : null,
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
    var table = this.isIE() ? this.tableContainer.childNodes[0] : this.tableContainer.childNodes[1];
    var self = this;

    table.ondblclick = function(e) {
      self.handleDoubleClick(e);
    }
  },

/**
 * @desc: handles mouse double click on table
 * @type: private
 */
  handleDoubleClick: function(e) {
    var targetElement = this.isIE() ? window.event.srcElement : e.target;

      // Save value of current editor and close it
    if (this.currentEditor != null) {
      new Ajax.Request(this.saveUrl + '?id=' + this.currentElement.title + '&value=' + this.currentEditor.getValue(), {
        method      : "get",
        encoding    : "utf-8",
        contentType : "text/javascript",
        onSuccess   : function(data) {
          //alert('saved !');
        }
      });

      this.currentElement.innerHTML = this.currentEditor.getValue();
    }

      // Create and activate new editor
	   // now Text and Dropdown editors are created in turn 
	 this.currentEditor = this.currentEditor && this.currentEditor.initValue ?
								 new TextEditor(targetElement.innerHTML) :						 
								 new DropdownEditor(targetElement.innerHTML);
	 this.currentEditor.setValue(targetElement.innerHTML); 
	 this.currentElement = targetElement;
    targetElement.removeChild(targetElement.childNodes[0]);
    targetElement.appendChild(this.currentEditor.node);
    this.currentEditor.node.focus();
  },

  isIE : function() {
    return (navigator.appName.indexOf("Microsoft") != -1);
  }
}