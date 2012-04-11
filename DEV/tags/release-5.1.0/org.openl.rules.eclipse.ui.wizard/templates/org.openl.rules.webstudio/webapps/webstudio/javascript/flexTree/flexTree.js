/**
 * Flex tree.
 */
var xmlElement = Class.create({
  initialize: function(data) {
    this.element = data;
  },
  getAttribute: function(name) {
    return this.element.getAttribute(name);
  },
  getAttributes: function() {
    var attributes = {};
    $A(this.element.attributes).each(function(a) {
      attributes[a.name] = a.value
    });
    return attributes;
  },
  exists: function() {
    return !!this.element;
  },
  iterateChildren: function(name, handler, t, start) {
    var children = this.element.childNodes;
    var element = new xmlElement();
    for (var i = start || 0; i < children.length; i++) {
      if (children[i].tagName == name) {
        element.element = children[i];
        if (handler.apply(t, [element, i]) == -1) return;
      }
    }
  },
  hasChild: function(name) {
    return !!$A(this.element.childNodes).find(function(child) {
      return child.tagName == name
    });
  }
});

/**
 * @desc: tree constructor
 * @param: htmlObject - parent html object or id of parent html object
 * @param: width - tree width
 * @param: height - tree height
 * @param: rootId - id of virtual root node (same as tree node id attribute in xml)
 * @type: public
 */
function flexTree(parentElement, width, height, rootId) {
  if (Prototype.Browser.IE) {
    try {
      document.execCommand("BackgroundImageCache", false, true);
    } catch (e) {
    }
  }

  this.width = width;
  this.height = height;
  this.rootId = rootId;

  // node id delimeter
  this.delimiter = ",";

  // ?
  this.xmlstate = 0;


  // ?
  this._selected = new Array();

  this.cursor = Prototype.Browser.IE ? "hand" : "pointer";
  this.showNodeTooltips = false;

  this.imagePath = "images/";
  this.def_img_x = "16px";
  this.def_img_y = "16px";
  this.def_line_img_x = "18px";
  this.def_line_img_y = "18px";
  this.clickableImages = true;
  this.showTreeLines = true;
  this.showTreeImages = true;
  // todo: rename
  this.checkArray = new Array("iconUncheckAll.gif", "iconCheckAll.gif", "iconCheckGray.gif", "iconUncheckDis.gif", "iconCheckDis.gif", "iconCheckDis.gif");
  this.radioArray = new Array("radio_off.gif", "radio_on.gif", "radio_on.gif", "radio_off.gif", "radio_on.gif", "radio_on.gif");
  this.lineArray = new Array("line2.gif", "line3.gif", "line4.gif", "blank.gif", "blank.gif", "line1.gif");
  this.minusArray = new Array("minus2.gif", "minus3.gif", "minus4.gif", "minus.gif", "minus5.gif");
  this.plusArray = new Array("plus2.gif", "plus3.gif", "plus4.gif", "plus.gif", "plus5.gif");
  this.imageArray = new Array("leaf.gif", "folderOpen.gif", "folderClosed.gif");

  // ?
  this.XMLsource = 0;
  // ?
  this.XMLloadingWarning = 0;
  // ?
  this.items = {};

  // ?
  this._ld_id = null;


  //@todo: use Element next() previous()

  //create root
  this.htmlNode = new flexTreeItem(this.rootId, "", 0, this);
  this.htmlNode.htmlNode.childNodes[0].childNodes[0].style.display = "none";
  this.htmlNode.htmlNode.childNodes[0].childNodes[0].childNodes[0].className = "hiddenRow";

  //init tree structures
  this.treeContainerElement = document.createElement('div');
  this.treeContainerElement.className = "containerTableStyle";
  this.treeContainerElement.style.width = this.width;
  this.treeContainerElement.style.height = this.height;
  $(parentElement).appendChild(this.treeContainerElement);

  this.treeContainerElement.appendChild(this.htmlNode.htmlNode);

  this.treeContainerElement.onselectstart = new Function("return false;");

  this.XMLLoader = new dataLoader(this._parseXMLTree, this, true, this.no_cashe);
  if (Prototype.Browser.IE) this.preventIECaching(true);

  /*
    $(window).observe("unload", function(event) {
      try {
        self.destructor();
      } catch(e) {}
    });
  */
  // ?
  this.dhx_Event();

  // ?
  this._onEventSet = {onMouseIn:function() {
    this.ehlt = true;
  },onMouseOut:function() {
    this.ehlt = true;
  },onSelect:function() {
    this._onSSCF = true;
  }}

  return this;
}

/**
 * @desc: deletes tree and clears memory
 * @type: public
 */
flexTree.prototype.destructor = function() {
  for (var a in this.items) {
    var z = this.items[a];
    if (!z) continue;
    z.parentElement = null;
    z.parentItem = null;
    z.tree = null;
    z.childNodes = null;
    z.span = null;
    z.tr.nodem = null;
    z.tr = null;
    z.htmlNode = null;
    this.items[a] = null;
  }
  this.treeContainerElement.innerHTML = "";
  this.XMLLoader.destructor();
  for (var a in this) {
    this[a] = null;
  }
}

/**
 * @desc: tree node constructor
 * @param: itemId - node id
 * @param: itemText - node label
 * @param: parentItem - parent item object
 * @param: treeObject - tree object
 * @param: actionHandler - onclick event handler(optional)
 * @param: mode - do not show images
 * @type: private
 * @todo: purpose
 */
var flexTreeItem = Class.create({
  initialize: function(id, text, parentItem, tree, actionHandler, hideImages) {
    this.id = tree.generateId(id);
    tree.items[this.id] = this;
    this.parentItem = parentItem;
    this.tree = tree;
    this.actionHandler = actionHandler;

    this.acolor = "";
    this.scolor = "";
    this.tr = 0;
    this.childsCount = 0;
    this.span = 0;
    this.closable = true;
    this.childNodes = new Array();

    this.text = text;
    this.images = new Array(tree.imageArray[0], tree.imageArray[1], tree.imageArray[2]);

    this.htmlNode = this.tree._createItem(0, this, hideImages);
  }
});

flexTree.prototype.generateId = function(id) {
  if (!this.items[id]) return id;
    if (!arguments.callee.id) arguments.callee.id = 0;
    return arguments.callee.id++;
}

/**
 * @desc: load tree from xml string
 * @type: public
 * @param: xmlString - XML string
 * @param: afterCall - function which will be called after xml loading
 */
flexTree.prototype.loadXMLString = function(xmlString, afterCall) {
  var that = this;
  if (!this.parsCount) this.callEvent("onXLS", [that,null]);
  this.xmlstate = 1;

  if (afterCall) this.XMLLoader.waitCall = afterCall;
  this.XMLLoader.loadXMLString(xmlString);
}

/**
 * @desc: load tree from xml file
 * @type: public
 * @param: file - link to XML file
 * @param: afterCall - function which will be called after xml loading
 * @todo: purpose
 */
flexTree.prototype.loadXML = function(file, afterCall) {
  var that = this;
  // ?
  if (!this.parsCount) this.callEvent("onXLS", [that,this._ld_id]);
  // ?
  this._ld_id = null;
  // ?
  this.xmlstate = 1;
  this.XMLLoader = new dataLoader(this._parseXMLTree, this, true, this.no_cashe);

  if (afterCall) this.XMLLoader.waitCall = afterCall;
  this.XMLLoader.loadXML(file);
}

/**
 *     @desc: set function called when tree node selected
 *     @param: func - event handling function
 *     @type: public
 *     @event: onClick
 *     @eventdesc: Event raised immideatly after text part of item in tree was clicked, but after default onClick functionality was processed.
 Richt mouse button click can be catched by onRightClick handler.
 *     @eventparam:  ID of clicked item
 */
flexTree.prototype.setOnClickHandler = function(handler) {
  this.onClickHandler = (typeof(handler) == "function") ? handler : eval(handler);
}

/**
 *     @desc: set function called when tree node double clicked
 *     @param: func - event handling function
 *     @type: public
 *     @topic: 0,7
 *     @event: onDblClick
 *     @depricated: use grid.attachEvent("onDblClick",func); instead
 *     @eventdesc: Event raised immideatly after item in tree was doubleclicked, before default onDblClick functionality was processed.
 Beware using both onClick and onDblClick events, because component can  generate onClick event before onDblClick event while doubleclicking item in tree.
 ( that behavior depend on used browser )
 *     @eventparam:  ID of item which was doubleclicked
 *     @eventreturn:  true - confirm opening/closing; false - deny opening/closing;
 */
flexTree.prototype.setOnDblClickHandler = function(handler) {
  this.onDblClickHandler = (typeof(handler) == "function") ? handler : eval(handler);
}

/**
 *     @desc: set function called before tree node opened/closed
 *     @param: func - event handling function
 *     @event:  onOpen
 *     @eventdesc: Event raised immideatly after item in tree got command to open/close , and before item was opened//closed. Event also raised for unclosable nodes and nodes without open/close functionality - in that case result of function will be ignored.
 Event not raised if node opened by flexTree API.
 *     @eventparam: ID of node which will be opened/closed
 *     @eventparam: Current open state of tree item. 0 - item has not childs, -1 - item closed, 1 - item opened.
 *     @eventreturn: true - confirm opening/closing; false - deny opening/closing;
 */
flexTree.prototype.setOnOpenHandler = function(handler) {
  this.onOpenHandler = (typeof(handler) == "function") ? handler : eval(handler);
};

/**
 * @desc: create and return  new line in tree
 * @type: private
 * @param: htmlObject - parent Node object
 * @param: node - item object
 * <tr><td> </td><td colspan="3">#{htmlObject}</td></tr>
 * @todo: purpose
 */
flexTree.prototype.createItemTR = function(htmlObject) {
  var tr = document.createElement('tr');

  var td1 = document.createElement('td');
  td1.appendChild(document.createTextNode(" "));
  tr.appendChild(td1);

  var td2 = document.createElement('td');
  td2.colSpan = 3;
  td2.appendChild(htmlObject);
  tr.appendChild(td2);

  return tr;
}

/**
 * @desc: create new child node
 * @type: private
 * @param: parentItem - parent node object
 * @param: itemId - new node id
 * @param: itemText - new node text
 * @param: itemActionHandler - function fired on node select event
 * @param: image1 - image for node without children;
 * @param: image2 - image for closed node;
 * @param: image3 - image for opened node
 * @param: optionStr - string of otions
 * @param: childs - node childs flag (for dynamical trees) (optional)
 * @param: beforeNode - node, after which new node will be inserted (optional)
 */
flexTree.prototype._attachChildNode = function(parentItem, itemId, itemText, itemActionHandler, image1, image2, image3, optionStr, childs, beforeNode, afterNode) {

  if (beforeNode && beforeNode.parentItem) parentItem = beforeNode.parentItem;
  if (((parentItem.XMLload == 0) && (this.XMLsource)) && (!this.XMLloadingWarning))
  {
    parentItem.XMLload = 1;
    this._loadDynXML(parentItem.id);

  }

  var Count = parentItem.childsCount;
  var Nodes = parentItem.childNodes;


  if (afterNode) {
    if (afterNode.tr.previousSibling.previousSibling) {
      beforeNode = afterNode.tr.previousSibling.nodem;
    }
    else
      optionStr = optionStr.replace("TOP", "") + ",TOP";
  }

  if (beforeNode)
  {
    var ik,jk;
    for (ik = 0; ik < Count; ik++)
      if (Nodes[ik] == beforeNode)
      {
        for (jk = Count; jk != ik; jk--)
          Nodes[1 + jk] = Nodes[jk];
        break;
      }
    ik++;
    Count = ik;
  }

  if (!itemActionHandler && this.onClickHandler) itemActionHandler = this.onClickHandler;

  if (optionStr) {
    var tempStr = optionStr.split(",");
    for (var i = 0; i < tempStr.length; i++)
    {
      switch (tempStr[i])
        {
        case "TOP": if (parentItem.childsCount > 0) {
          beforeNode = new Object;
          beforeNode.tr = parentItem.childNodes[0].tr.previousSibling;
        }
          parentItem._has_top = true;
          for (ik = Count; ik > 0; ik--)
            Nodes[ik] = Nodes[ik - 1];
          Count = 0;
          break;
      }
    }
    ;
  }
  ;

  var n;
  if (!(n = this.items[itemId]) || n.span != -1) {
    n = Nodes[Count] = new flexTreeItem(itemId, itemText, parentItem, this, itemActionHandler, 1);
    itemId = Nodes[Count].id;
    parentItem.childsCount++;
  }

  if (!n.htmlNode) {
    n.text = itemText;
    n.htmlNode = this._createItem(0, n);
  }

  if (image1) n.images[0] = image1;
  if (image2) n.images[1] = image2;
  if (image3) n.images[2] = image3;


  var tr = this.createItemTR(n.htmlNode);
  if ((this.XMLloadingWarning) || (this._hAdI))
    n.htmlNode.parentNode.parentNode.style.display = "none";


  if ((beforeNode) && (beforeNode.tr.nextSibling))
    parentItem.htmlNode.childNodes[0].insertBefore(tr, beforeNode.tr.nextSibling);
  else
    if (this.parsingOn == parentItem.id) {
      this.parsedArray[this.parsedArray.length] = tr;
    }
    else
      parentItem.htmlNode.childNodes[0].appendChild(tr);


  if ((beforeNode) && (!beforeNode.span)) beforeNode = null;

  if (this.XMLsource) if ((childs) && (childs != 0)) n.XMLload = 0; else n.XMLload = 1;
  n.tr = tr;
  tr.nodem = n;

  if (parentItem.itemId == 0)
    tr.childNodes[0].className = "hiddenRow";

  if ((parentItem._r_logic) || (this._frbtr))
    n.htmlNode.childNodes[0].childNodes[0].childNodes[1].childNodes[0].src = this.imagePath + this.radioArray[0];

  if (optionStr) {
    var tempStr = optionStr.split(",");

    for (var i = 0; i < tempStr.length; i++)
    {
      switch (tempStr[i])
        {
        case "SELECT": this.selectItem(itemId, false); break;
        case "CALL": this.selectItem(itemId, true);   break;
        case "CHILD":  n.XMLload = 0;  break;
        case "CHECKED":
          if (this.XMLloadingWarning)
            this.setCheckList += this.delimiter + itemId;
          else
            this.setCheck(itemId, 1);
          break;
        case "HCHECKED":
          this._setCheck(n, "unsure");
          break;
        case "OPEN": n.openMe = 1;  break;
      }
    }
    ;
  }
  ;

  if (!this.XMLloadingWarning)
  {
    if ((this._getOpenState(parentItem) < 0) && (!this._hAdI)) this.openItem(parentItem.id);

    if (beforeNode)
    {
      this._correctPlus(beforeNode);
      this._correctLine(beforeNode);
    }
    this._correctPlus(parentItem);
    this._correctLine(parentItem);
    this._correctPlus(n);
    if (parentItem.childsCount >= 2)
    {
      this._correctPlus(Nodes[parentItem.childsCount - 2]);
      this._correctLine(Nodes[parentItem.childsCount - 2]);
    }
    if (parentItem.childsCount != 2) this._correctPlus(Nodes[0]);
  }
  return n;
};


/**
 * @desc: create new node as a child to specified with parentId
 * @type: deprecated
 * @param: parentId - parent node id
 * @param: itemId - new node id
 * @param: itemText - new node text
 * @param: itemActionHandler - function fired on node select event (optional)
 * @param: image1 - image for node without children; (optional)
 * @param: image2 - image for closed node; (optional)
 * @param: image3 - image for opened node (optional)
 * @param: optionStr - options string (optional)
 * @param: children - node children flag (for dynamical trees) (optional)
 */
flexTree.prototype.insertNewItem = function(parentId, itemId, itemText, itemActionHandler, image1, image2, image3, optionStr, children) {
  var parentItem = this.items[parentId];
  if (!parentItem) return (-1);
  var nodez = this._attachChildNode(parentItem, itemId, itemText, itemActionHandler, image1, image2, image3, optionStr, children);

  return nodez;
};
/**
 * @desc: create new node as a child to specified with parentId
 * @type: public
 * @param: parentId - parent node id
 * @param: itemId - new node id
 * @param: itemText - new node label
 * @param: itemActionHandler - function fired on node select event (optional)
 * @param: image1 - image for node without children; (optional)
 * @param: image2 - image for closed node; (optional)
 * @param: image3 - image for opened node (optional)
 * @param: optionStr - options string (optional)
 * @param: children - node children flag (for dynamical trees) (optional)
 */
flexTree.prototype.insertNewChild = function(parentId, itemId, itemText, itemActionHandler, image1, image2, image3, optionStr, children) {
  return this.insertNewItem(parentId, itemId, itemText, itemActionHandler, image1, image2, image3, optionStr, children);
}

/**
 * @desc: parse xml
 * @type: private
 * @param: node - top XML node
 * @param: parentId - parent node id
 * @param: level - level of tree
 */
flexTree.prototype._parseXMLTree = function(tree, xmlLoader) {
  var p = new xmlElement(xmlLoader.getXMLTopNode("tree"));
  tree._parse(p);
}

flexTree.prototype._parse = function(xmlElement, parentId, level, start) {
  if (!xmlElement.exists()) return;

  if (this.parsCount) {
    this.parsCount++;
  } else {
    this.parsCount = 1;
  }

  this.XMLloadingWarning = 1;
  this.nodeAskingCall = "";

  if (!parentId) {          //top level
    parentId = xmlElement.getAttribute("id");
    if (xmlElement.getAttribute("radio")) {
      this.htmlNode._r_logic = true;
    }
    this.parsingOn = parentId;
    this.parsedArray = new Array();
    this.setCheckList = "";
  }

  var temp = this.items[parentId];
  if (!temp) return myError.throwError("DataStructure", "XML reffers to not existing parent");

  if ((temp.childsCount) && (!start) && (!this._edsbps) && (!temp._has_top))
    var preNode = temp.childNodes[temp.childsCount - 1];
  else
    var preNode = 0;

  this.npl = 0;

  xmlElement.iterateChildren("node", function(pointer, i) {
    temp.XMLload = 1;
    this._parseItem(pointer, temp, preNode);
    this.npl++;
  }, this, start);


  if (!level) {
    temp.XMLload = 1;

    var parsedNodeTop = this.items[this.parsingOn];

    for (var i = 0; i < this.parsedArray.length; i++)
      temp.htmlNode.childNodes[0].appendChild(this.parsedArray[i]);

    this.lastLoadedXMLId = parentId;
    this.XMLloadingWarning = 0;

    var chArr = this.setCheckList.split(this.delimiter);
    for (var n = 0; n < chArr.length; n++)
      if (chArr[n]) this.setCheck(chArr[n], 1);

    this._redrawFrom(this, null, start)


    if (xmlElement.getAttribute("order") && xmlElement.getAttribute("order") != "none")
      this._reorderBranch(temp, xmlElement.getAttribute("order"), true);

    if (this.nodeAskingCall != "")   this.selectItem(this.nodeAskingCall, true);
    if (this._branchUpdate) this._branchUpdateNext(xmlElement);
  }


  if (this.parsCount == 1) {
    this.parsingOn = null;
  }
  this.parsCount--;


  return this.nodeAskingCall;
}

flexTree.prototype._parseItem = function(xmlElement, temp, preNode) {
  var a = xmlElement.getAttributes();

  var modifiers = [];
  if (a.select) modifiers.push("SELECT");
  if (a.top) modifiers.push("TOP");
  if (a.open == 1) modifiers.push("OPEN");
  if (a.checked == -1) {
    modifiers.push("HCHECKED");
  } else if (a.checked) {
    modifiers.push("CHECKED");
  }

  var newNode = this._attachChildNode(temp, a.id, a.text, 0, a.im0, a.im1, a.im2, modifiers.join(","), a.child, 0, preNode);

  if (a.call) nodeAskingCall = a.id;
  if (a.tooltip) newNode.span.parentNode.parentNode.title = a.tooltip;
  if (a.style) $(newNode.span).setStyle(a.style);
  if (a.radio) newNode._r_logic = true;

  if (a.nocheckbox) {
    newNode.span.parentNode.previousSibling.previousSibling.childNodes[0].style.display = 'none';
    newNode.nocheckbox = true;
  }

  if (a.disabled) {
    if (a.checked != null) this._setCheck(newNode, convertStringToBoolean(a.checked));
  }

  if (this.parserExtension) this.parserExtension._parseExtension(node.childNodes[i], this.parserExtension, a.id, parentId);

  this.setItemColor(newNode, a.aCol, a.sCol);

  if (!Object.isUndefined(a.closeable)) this.setItemClosable(newNode, a.closeable);


  if (a.topoffset) this.setItemTopOffset(newNode, a.topoffset);

  if (xmlElement.hasChild("node")) {
    this._parse(xmlElement, a.id, 1);
  }
}


flexTree.prototype._branchUpdateNext = function(p) {
  p.iterateChildren("node", function(c) {
    var nid = c.getAttribute("id");
    if (this.items[nid] && (!this.items[nid].XMLload))  return;
    this._branchUpdate++;
    this.smartRefreshItem(c.getAttribute("id"), c);
  }, this)
  this._branchUpdate--;
}

/**
 * @desc: reset tree images from selected level
 * @type: private
 * @param: tree - tree
 * @param: itemObject - current item
 */
flexTree.prototype._redrawFrom = function(tree, itemObject, start, visMode) {
  if (!itemObject) {
    var tempx = tree.items[tree.lastLoadedXMLId];
    tree.lastLoadedXMLId = -1;
    if (!tempx) return 0;
  }
  else tempx = itemObject;
  var acc = 0;

  for (var i = (start ? start - 1 : 0); i < tempx.childsCount; i++)
  {
    if ((!this._branchUpdate) || (this._getOpenState(tempx) == 1))
      if ((!itemObject) || (visMode == 1)) tempx.childNodes[i].htmlNode.parentNode.parentNode.style.display = "";
    if (tempx.childNodes[i].openMe == 1)
    {
      this._openItem(tempx.childNodes[i]);
      tempx.childNodes[i].openMe = 0;
    }

    tree._redrawFrom(tree, tempx.childNodes[i]);


  }

  tree._correctLine(tempx);
  tree._correctPlus(tempx);
}

/**
 * @desc: collapse target node
 * @type: private
 * @param: itemObject - item object
 */
flexTree.prototype._xcloseAll = function(itemObject)
{
  if (itemObject.unParsed) return;
  if (this.rootId != itemObject.id) {
    var Nodes = itemObject.htmlNode.childNodes[0].childNodes;
    var Count = Nodes.length;

    for (var i = 1; i < Count; i++)
      Nodes[i].style.display = "none";

    this._correctPlus(itemObject);
  }

  for (var i = 0; i < itemObject.childsCount; i++)
    if (itemObject.childNodes[i].childsCount)
      this._xcloseAll(itemObject.childNodes[i]);
};
/**
 * @desc: expand target node
 * @type: private
 * @param: itemObject - item object
 */
flexTree.prototype._xopenAll = function(itemObject)
{
  this._HideShow(itemObject, 2);
  for (var i = 0; i < itemObject.childsCount; i++)
    this._xopenAll(itemObject.childNodes[i]);
};
/**
 * @desc: set correct tree-line and node images
 * @type: private
 * @param: itemObject - item object
 */
flexTree.prototype._correctPlus = function(itemObject) {
  if (!itemObject.htmlNode) return;
  var imsrc = itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[0].lastChild;
  var imsrc2 = itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[2].childNodes[0];

  var workArray = this.lineArray;
  if ((this.XMLsource) && (!itemObject.XMLload))
  {
    var workArray = this.plusArray;
    imsrc2.src = this.imagePath + itemObject.images[2];
    if (this._txtimg) return (imsrc.innerHTML = "[+]");
  }
  else
    if ((itemObject.childsCount) || (itemObject.unParsed))
    {
      if ((itemObject.htmlNode.childNodes[0].childNodes[1]) && ( itemObject.htmlNode.childNodes[0].childNodes[1].style.display != "none" ))
      {
        if (!itemObject.wsign) var workArray = this.minusArray;
        imsrc2.src = this.imagePath + itemObject.images[1];
        if (this._txtimg) return (imsrc.innerHTML = "[-]");
      }
      else
      {
        if (!itemObject.wsign) var workArray = this.plusArray;
        imsrc2.src = this.imagePath + itemObject.images[2];
        if (this._txtimg) return (imsrc.innerHTML = "[+]");
      }
    }
    else
    {
      imsrc2.src = this.imagePath + itemObject.images[0];
    }


  var tempNum = 2;
  if (!itemObject.tree.showTreeLines) imsrc.src = this.imagePath + workArray[3];
  else {
    if (itemObject.parentItem) tempNum = this._getCountStatus(itemObject.id, itemObject.parentItem);
    imsrc.src = this.imagePath + workArray[tempNum];
  }
};

/**
 * @desc: set correct tree-line images
 * @type: private
 * @param: itemObject - item object
 */
flexTree.prototype._correctLine = function(itemObject) {
  if (!itemObject.htmlNode) return;
  var sNode = itemObject.parentItem;
  if (sNode)
    if ((this._getLineStatus(itemObject.id, sNode) == 0) || (!this.showTreeLines))
      for (var i = 1; i <= itemObject.childsCount; i++) {
        if (!itemObject.htmlNode.childNodes[0].childNodes[i]) break;
        itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundImage = "";
        itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundRepeat = "";
      }
    else
      for (var i = 1; i <= itemObject.childsCount; i++) {
        if (!itemObject.htmlNode.childNodes[0].childNodes[i]) break;
        itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundImage = "url(" + this.imagePath + this.lineArray[5] + ")";
        itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundRepeat = "repeat-y";
      }
};
/**
 * @desc: return type of node
 * @type: private
 * @param: itemId - item id
 * @param: itemObject - parent node object
 */
flexTree.prototype._getCountStatus = function(itemId, itemObject) {

  if (itemObject.childsCount <= 1) {
    if (itemObject.id == this.rootId) return 4; else  return 0;
  }

  if (itemObject.childNodes[0].id == itemId) if (!itemObject.id) return 2; else return 1;
  if (itemObject.childNodes[itemObject.childsCount - 1].id == itemId) return 0;

  return 1;
};
/**
 * @desc: return type of node
 * @type: private
 * @param: itemId - node id
 * @param: itemObject - parent node object
 */
flexTree.prototype._getLineStatus = function(itemId, itemObject) {
  if (itemObject.childNodes[itemObject.childsCount - 1].id == itemId) return 0;
  return 1;
}

/**
 * @desc: open/close node
 * @type: private
 * @param: itemObject - node object
 * @param: mode - open/close mode [1-close 2-open](optional)
 */
flexTree.prototype._HideShow = function(itemObject, mode) {
  if ((this.XMLsource) && (!itemObject.XMLload)) {
    if (mode == 1) return; //close for not loaded node - ignore it
    itemObject.XMLload = 1;
    this._loadDynXML(itemObject.id);
    return;
  }
  ;

  var Nodes = itemObject.htmlNode.childNodes[0].childNodes;
  var Count = Nodes.length;
  if (Count > 1) {
    if (( (Nodes[1].style.display != "none") || (mode == 1) ) && (mode != 2)) {
      //nb:solves standard doctype prb in IE
      this.treeContainerElement.childNodes[0].border = "1";
      this.treeContainerElement.childNodes[0].border = "0";
      nodestyle = "none";
    }
    else  nodestyle = "";

    for (var i = 1; i < Count; i++)
      Nodes[i].style.display = nodestyle;
  }
  this._correctPlus(itemObject);
}

/**
 * @desc: return node state
 * @type: private
 * @param: itemObject - node object
 */
flexTree.prototype._getOpenState = function(itemObject) {
  var z = itemObject.htmlNode.childNodes[0].childNodes;
  if (z.length <= 1) return 0;
  if (z[1].style.display != "none") return 1;
  else return -1;
}


/**
 * @desc: ondblclick item  event handler
 * @type: private
 */
flexTree.prototype.onRowClick2 = function() {
  var that = this.parentItem.tree;
  if (!that.callEvent("onDblClick", [this.parentItem.id,that])) return 0;
  if (this.onDblClickHandler) this.onDblClickHandler(item.id, that);

  if (this.parentItem.closable)
    that._HideShow(this.parentItem);
  else
    that._HideShow(this.parentItem, 2);
}

/**
 * @desc: onclick item event handler
 * @type: private
 */
flexTree.prototype.onRowClick = function() {
  var that = this.parentItem.tree;
  if (that.onOpenHandler && !that.onOpenHandler(this.parentItem.id, that._getOpenState(this.parentItem))) return 0;
  if (this.parentItem.closable)
    that._HideShow(this.parentItem);
  else
    that._HideShow(this.parentItem, 2);
}

/**
 * @desc: onclick item image event handler
 * @type: private
 */
flexTree.prototype.onRowClickDown = function(e) {
  e = e || window.event;
  var that = this.parentItem.tree;
  that._selectItem(this.parentItem, e);
};


/*****
 SELECTION
 *****/

/**
 * @desc: retun selected item id
 * @type: public
 * @return: id of selected item
 */
flexTree.prototype.getSelectedItemId = function()
{
  var str = new Array();
  for (var i = 0; i < this._selected.length; i++) str[i] = this._selected[i].id;
  return (str.join(this.delimiter));
};

/**
 * @desc: visual select item in tree
 * @type: private
 * @param: node - tree item object
 */
flexTree.prototype._selectItem = function(node, e) {
  if (this._onSSCF) this._onSSCFold = this.getSelectedItemId();

  this._unselectItems();

  this._markItem(node);
  if (this._onSSCF) {
    var z = this.getSelectedItemId();
    if (z != this._onSSCFold)
      this.callEvent("onSelect", [z]);
  }
}

flexTree.prototype._markItem = function(node) {
  if (node.scolor)  node.span.style.color = node.scolor;
  node.span.className = "selectedTreeRow";
  node.i_sel = true;
  this._selected[this._selected.length] = node;
}


/**
 * @desc: visual unselect item in tree
 * @type: private
 * @param: node - tree item object
 */
flexTree.prototype._unselectItem = function(node) {
  if ((node) && (node.i_sel))
  {

    node.span.className = "standartTreeRow";
    if (node.acolor)  node.span.style.color = node.acolor;
    node.i_sel = false;
    for (var i = 0; i < this._selected.length; i++)
      if (!this._selected[i].i_sel) {
        this._selected.splice(i, 1);
        break;
      }
  }
}

/**
 * @desc: visual unselect items in tree
 * @type: private
 * @param: node - tree item object
 */
flexTree.prototype._unselectItems = function() {
  for (var i = 0; i < this._selected.length; i++) {
    var node = this._selected[i];
    node.span.className = "standartTreeRow";
    if (node.acolor)  node.span.style.color = node.acolor;
    node.i_sel = false;
  }
  this._selected = new Array();
}


/**
 * @desc: select node text event handler
 * @type: private
 * @param: e - event object
 * @param: htmlObject - node object
 * @param: mode - if false - call onSelect event
 */
flexTree.prototype.onRowSelect = function(e, htmlObject, mode) {
  e = e || window.event;

  var item = this.parentItem;
  if (htmlObject) item = htmlObject.parentItem;

  var lastId = item.tree.getSelectedItemId();
  if ((!e) || (!e.skipUnSel))
    item.tree._selectItem(item, e);

  if (!mode) {
    if ((e) && (e.button == 2))
      item.tree.callEvent("onRightClick", [item.id,e]);

    if (item.actionHandler) item.actionHandler(item.id, lastId);
    else item.tree.callEvent("onClick", [item.id,lastId]);
  }
};


flexTree.prototype._getImg = function(id) {
  return document.createElement((id == this.rootId) ? "div" : "img");
}

/**
 * @desc: create HTML elements for tree node
 * @type: private
 * @param: acheck - enable/disable checkbox
 * @param: itemObject - item object
 * @param: mode - mode
 */
flexTree.prototype._createItem = function(acheck, itemObject, hideImages) {

  var table = document.createElement('table');
  table.cellSpacing = 0;
  table.cellPadding = 0;
  table.border = 0;

  table.style.margin = 0;
  table.style.padding = 0;

  var tbody = document.createElement('tbody');
  var tr = document.createElement('tr');

  var td1 = document.createElement('td');
  td1.className = "standartTreeImage";

  if (this._txtimg) {
    var img0 = document.createElement("div");
    td1.appendChild(img0);
    img0.className = "dhx_tree_textSign";
  }
  else
  {
    var img0 = this._getImg(itemObject.id);
    img0.border = "0";
    if (img0.tagName == "IMG") img0.align = "absmiddle";
    td1.appendChild(img0);
    img0.style.padding = 0;
    img0.style.margin = 0;
    img0.style.width = this.def_line_img_x;
    img0.style.height = this.def_line_img_y;
  }

  var td11 = document.createElement('td');
//         var inp=document.createElement("input");            inp.type="checkbox"; inp.style.width="12px"; inp.style.height="12px";
  var inp = this._getImg(this.cBROf ? this.rootId : itemObject.id);
  inp.checked = 0;
  inp.src = this.imagePath + this.checkArray[0];
  inp.style.width = "16px";
  inp.style.height = "16px";
            //can cause problems with hide/show check
  if (!acheck) (Prototype.Browser.Opera ? td11 : inp).style.display = "none";

         // td11.className="standartTreeImage";
  //if (acheck)
  td11.appendChild(inp);
  if ((!this.cBROf) && (inp.tagName == "IMG")) inp.align = "absmiddle";
  //inp.onclick = this.onCheckBoxClick;
  inp.tree = this;
  inp.parentItem = itemObject;
  td11.width = "20px";

  var td12 = document.createElement('td');
  td12.className = "standartTreeImage";
  var img = this._getImg(this.showTreeImages ? itemObject.id : this.rootId);
  img.onmousedown = this._preventNsDrag;
  img.ondragstart = this._preventNsDrag;
  img.border = "0";
  if (this.clickableImages) {
    img.parentItem = itemObject;
    if (img.tagName == "IMG") img.align = "absmiddle";
    img.onclick = this.onRowSelect;
  }
  if (!hideImages) img.src = this.imagePath + this.imageArray[0];
  td12.appendChild(img);
  img.style.padding = 0;
  img.style.margin = 0;
  if (this.showTreeImages)
  {
    img.style.width = this.def_img_x;
    img.style.height = this.def_img_y;
  }
  else
  {
    img.style.width = "0px";
    img.style.height = "0px";
    if (Prototype.Browser.Opera) td12.style.display = "none";
  }


  var td2 = document.createElement('td');
  td2.className = "standartTreeRow";

  itemObject.span = document.createElement('span');
  itemObject.span.className = "standartTreeRow";

  td2.noWrap = true;
  td2.style.width = "100%";

//      itemObject.span.appendChild(document.createTextNode(itemObject.text));
  itemObject.span.innerHTML = itemObject.text;
  td2.appendChild(itemObject.span);
  td2.parentItem = itemObject;
  td1.parentItem = itemObject;
  td2.onclick = this.onRowSelect;
  td1.onclick = this.onRowClick;
  td2.ondblclick = this.onRowClick2;
  if (this.showNodeTooltips) tr.title = itemObject.text;

  itemObject.span.style.paddingLeft = "5px";
  itemObject.span.style.paddingRight = "5px";
  td2.style.verticalAlign = "";
  td2.style.fontSize = "10pt";
  td2.style.cursor = this.cursor;
  tr.appendChild(td1);
  tr.appendChild(td11);
  tr.appendChild(td12);
  tr.appendChild(td2);
  tbody.appendChild(tr);
  table.appendChild(tbody);

  if (this.checkEvent && this.checkEvent("onRightClick"))
    tr.oncontextmenu = Function("e", "this.childNodes[0].parentItem.tree.callEvent('onRightClick',[this.childNodes[0].parentItem.id,(e||window.event)]); return false;");


  return table;
}

/**
 *     @desc: set top offset for item
 *     @type: public
 *     @param: itemId - id of item
 *     @param: value - value of top offset
 */
flexTree.prototype.setItemTopOffset = function(itemId, value) {
  if (typeof(itemId) == "string")
    var node = this.items[itemId];
  else
    var node = itemId;

  var z = node.span.parentNode.parentNode;
  for (var i = 0; i < z.childNodes.length; i++) {
    if (i != 0)
      z.childNodes[i].style.height = 18 + parseInt(value) + "px";
    else {
      var w = z.childNodes[i].firstChild;
      if (z.childNodes[i].firstChild.tagName != 'DIV') {
        w = document.createElement("DIV");
        z.childNodes[i].insertBefore(w, z.childNodes[i].firstChild);
      }
      w.style.height = parseInt(value) + "px";
      w.style.backgroundImage = "url(" + this.imagePath + this.lineArray[5] + ")";
      w.innerHTML = "&nbsp;";
      w.style.overflow = 'hidden';
      if (parseInt(value) == 0)
        z.childNodes[i].removeChild(w);
    }
    z.childNodes[i].vAlign = "bottom";
  }
}

/**
 * @desc: enables dynamic loading from XML
 * @type: public
 * @param: filePath - name of script returning XML; in case of virtual loading - user defined function
 */
flexTree.prototype.setXMLAutoLoading = function(filePath) {
  this.XMLsource = filePath;
};

/**
 * @desc: return open/close state
 * @type: public
 * @param: itemId - node id
 * @return: -1 - close, 1 - opened, 0 - node doesn't have children
 */
flexTree.prototype.getOpenState = function(itemId) {
  var temp = this.items[itemId];
  if (!temp) return "";
  return this._getOpenState(temp);
};

/**
 * @desc: set node text color
 * @param: itemId - id of node
 * @param: defaultColor - node color
 * @param: selectedColor - selected node color
 * @type: public
 */
flexTree.prototype.setItemColor = function(itemId, defaultColor, selectedColor)
{
  if ((itemId) && (itemId.span))
    var temp = itemId;
  else
    var temp = this.items[itemId];
  if (!temp) return 0;

  if (temp.i_sel) {
    if (selectedColor) temp.span.style.color = selectedColor;
  } else {
    if (defaultColor) temp.span.style.color = defaultColor;
  }

  if (selectedColor) temp.scolor = selectedColor;
  if (defaultColor) temp.acolor = defaultColor;
}

/**
 *     @desc: collapse node
 *     @param: itemId - id of node
 *     @type: public
 */
flexTree.prototype.closeItem = function(itemId) {
  if (this.rootId == itemId) return 0;
  var temp = this.items[itemId];
  if (!temp) return 0;
  if (temp.closable)
    this._HideShow(temp, 1);
};

/**
 * @desc: expand node
 * @param: itemId - id of node
 * @type: public
 */
flexTree.prototype.openItem = function(itemId) {
  var temp = this.items[itemId];
  if (!temp) return 0;
  else return this._openItem(temp);
};

/**
 * @desc: expand node
 * @param: item - tree node object
 * @type: private
 * @editing: pro
 */
flexTree.prototype._openItem = function(item) {
  var state = this._getOpenState(item);
  if ((state < 0) || (((this.XMLsource) && (!item.XMLload)))) {
    // commented due to infinite recursion
    //if (this.onOpenHandler && !this.onOpenHandler(item.id, state)) return 0;

    this._HideShow(item, 2);
  }
  if (item.parentItem) this._openItem(item.parentItem);
};

/**
 * @desc: prevent node from closing
 * @param: itemId - id of node
 * @param: flag -  if 0 - node can't be closed, else node can be closed
 * @type: public
 */
flexTree.prototype.setItemClosable = function(itemId, closable) {
  if ((itemId) && (itemId.span))
    var temp = itemId;
  else
    var temp = this.items[itemId];
  if (!temp) return 0;
  temp.closable = closable;
}

/**
 * @desc: select node ( and optionaly fire onselect event)
 * @type: public
 * @param: itemId - node id
 * @param: mode - If true, script function for selected node will be called.
 * @param: preserve - preserve earlier selected nodes
 */
flexTree.prototype.selectItem = function(itemId, mode, preserve) {
  mode = convertStringToBoolean(mode);
  var temp = this.items[itemId];
  if ((!temp) || (!temp.parentItem)) return 0;

  if (this.XMLloadingWarning)
    temp.parentItem.openMe = 1;
  else
    this._openItem(temp.parentItem);

      //temp.onRowSelect(0,temp.htmlNode.childNodes[0].childNodes[0].childNodes[3],mode);
  var ze = null;
  if (preserve) {
    ze = new Object;
    ze.ctrlKey = true;
    if (temp.i_sel) ze.skipUnSel = true;
  }
  if (mode)
    this.onRowSelect(ze, temp.htmlNode.childNodes[0].childNodes[0].childNodes[3], false);
  else
    this.onRowSelect(ze, temp.htmlNode.childNodes[0].childNodes[0].childNodes[3], true);
};

///DragAndDrop

flexTree.prototype._preventNsDrag = function(e) {
  if ((e) && (e.preventDefault)) {
    e.preventDefault();
    return false;
  }
  return false;
}


/**
 * @desc: load xml for tree branch
 * @param: id - id of parent node
 * @param: src - path to xml, optional
 * @type: private
 */
flexTree.prototype._loadDynXML = function(id, src) {
  src = src || this.XMLsource;
  var sn = (new Date()).valueOf();
  this._ld_id = id;

  this.loadXML(src + getUrlSymbol(src) + "uid=" + sn + "&id=" + escape(id));
};

/**
 * @desc:  prevent caching in IE  by adding random value to URL string
 * @param: mode - enable/disable random value ( disabled by default )
 * @type: public
 */
flexTree.prototype.preventIECaching = function(mode) {
  this.no_cashe = convertStringToBoolean(mode);
  this.XMLLoader.preventIECaching = this.no_cashe;
}

/**
 * @desc: set escaping mode (used for escaping ID in requests)
 * @param: mode - escaping mode ("utf8" for UTF escaping)
 * @type: public
 */
flexTree.prototype.setEscapingMode = function(mode) {
  this.utfesc = mode;
}

/**
 */
flexTree.prototype.setShowNodeTooltips = function(mode) {
  this.showNodeTooltips = mode;
}

/**
 * @desc: called on mouse out
 * @type: private
 */
flexTree.prototype._itemMouseOut = function() {
  var that = this.childNodes[3].parentItem;
  var tree = that.tree;
  tree.callEvent("onMouseOut", [that.id]);
  if (that.id == tree._l_onMSI) tree._l_onMSI = null;
  if (!tree.ehlta) return;
  that.span.className = that.span.className.replace("_lor", "");
}
/**
 * @desc: called on mouse in
 * @type: private
 */
flexTree.prototype._itemMouseIn = function() {
  var that = this.childNodes[3].parentItem;
  var tree = that.tree;

  if (tree._l_onMSI != that.id) tree.callEvent("onMouseIn", [that.id]);
  tree._l_onMSI = that.id;
  if (!tree.ehlta) return;
  that.span.className = that.span.className.replace("_lor", "");
  that.span.className = that.span.className.replace(/((standart|selected)TreeRow)/, "$1_lor");
}


/**
 * @desc: set path to images directory
 * @param: newPath - path to images directory (related to the page with tree or absolute http url)
 * @type: public
 */
flexTree.prototype.setImagePath = function(newPath) {
  this.imagePath = newPath;
};

flexTree.prototype.dhx_Event = function() {
  this.callEvent = function(name, a) {
    if (this["ev_" + name]) return this["ev_" + name].apply(this, a);
    return true;
  }
  this.checkEvent = function(name) {
    if (this["ev_" + name]) return true;
    return false;
  }
}
