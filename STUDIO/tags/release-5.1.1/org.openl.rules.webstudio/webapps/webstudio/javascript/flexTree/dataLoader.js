/**
 * Data loader.
 */
var dataLoader = Class.create({
  /**
   * @desc: dataLoader object
   * @type: private
   * @param: xmlParserFunction - XML parser function
   * @param: flexTree
   * @param: async - sync/async mode (async by default)
   * @param: preventIECaching - enable/disable IE caching preventing
   */
  initialize: function(xmlParserFunction, flexTree, async, preventIECaching) {
    this.xmlParserFunction = xmlParserFunction || null;
    if (typeof(async) != "undefined") {
      this.async = async;
    } else {
      this.async = true;
    }
    this.mainObject = flexTree || null;
    this.preventIECaching = !!preventIECaching;
    this.xmlDoc = "";
    this.waitCall = null;
  },

  /**
   * @desc: xml loading handler
   * @type: private
   * @param: dataLoader - dataLoader object
   * @todo: purpose of waitCall
   */
  onreadystatechange: function(dataLoader) {
    var f = function () {
      if ((dataLoader) && (dataLoader.xmlParserFunction != null)) {
        if ((!dataLoader.xmlDoc.readyState) || (dataLoader.xmlDoc.readyState == 4)) {
          dataLoader.xmlParserFunction(dataLoader.mainObject, dataLoader);
          if (dataLoader.waitCall) {
            dataLoader.waitCall();
            dataLoader.waitCall = null;
          }
        }
      }
    }
    return f;
  },

  /**
   * @desc: return XML top node
   * @param: tagName - top XML node tag name (not used in IE, required for Safari and Mozilla)
   * @type: private
   * @todo: simplify
   * @returns: top XML node
   */
  getXMLTopNode: function(tagName, oldObj) {
    if (this.xmlDoc.responseXML) {
      var elements = this.xmlDoc.responseXML.getElementsByTagName(tagName);
      var element = elements[0];
    } else {
      var element = this.xmlDoc.documentElement;
    }
    if (element) {
      this._retry = false;
      return element;
    }

    if (Prototype.Browser.IE && (!this._retry)) {
      //fall back to MS.XMLDOM
      var xmlString = this.xmlDoc.responseText;
      var oldObj = this.xmlDoc;
      this._retry = true;
      this.xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
      this.xmlDoc.async = false;
      this.xmlDoc["loadXM" + "L"](xmlString);

      return this.getXMLTopNode(tagName, oldObj);
    }
    myError.throwError("LoadXML", "Incorrect XML", [(oldObj || this.xmlDoc),this.mainObject]);
    return document.createElement("DIV");
  },

  /**
   * @desc: load XML from string
   * @type: private
   * @param: xmlString - xml string
   * @todo: purpose of waitCall
   */
  loadXMLString: function(xmlString) {
    try {
      var parser = new DOMParser();
      this.xmlDoc = parser.parseFromString(xmlString, "text/xml");
    } catch(e) {
      this.xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
      this.xmlDoc.async = this.async;
      this.xmlDoc["loadXM" + "L"](xmlString);
    }

    this.xmlParserFunction(this.mainObject, this);
    if (this.waitCall) {
      this.waitCall();
      this.waitCall = null;
    }
  },

  /**
   * @desc: load XML
   * @type: private
   * @param: filePath - xml file path
   * @param: postMode - send POST request
   * @param: postVars - list of vars for post request
   * @todo: rewrite using Ajax.Request
   * @todo: purpose of rpc
   */
  loadXML: function(filePath, isPostMode, postVars, rpc) {
    if (this.preventIECaching) {
      filePath += getUrlSymbol(filePath) + "flexTree_random=" + (new Date()).valueOf();
    }
    this.filePath = filePath;

    if ((!Prototype.Browser.IE) && (window.XMLHttpRequest)) {
      this.xmlDoc = new XMLHttpRequest();
    } else {
      if (document.implementation && document.implementation.createDocument) {
        this.xmlDoc = document.implementation.createDocument("", "", null);
        this.xmlDoc.onload = new this.onreadystatechange(this);
        this.xmlDoc.load(filePath);
        return;
      } else {
        this.xmlDoc = new ActiveXObject("Microsoft.XMLHTTP");
      }
    }

    this.xmlDoc.open(isPostMode ? "POST" : "GET", filePath, this.async);
    if (rpc) {
      this.xmlDoc.setRequestHeader("User-Agent", "flexTreeRPC v0.1 (" + navigator.userAgent + ")");
      this.xmlDoc.setRequestHeader("Content-type", "text/xml");
    } else if (isPostMode) {
      this.xmlDoc.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    }
    this.xmlDoc.onreadystatechange = new this.onreadystatechange(this);
    this.xmlDoc.send(null || postVars);

  }
});


/**
 * @desc: destructor, cleans used memory
 * @type: private
 */
dataLoader.prototype.destructor = function() {
  this.xmlParserFunction = null;
  this.mainObject = null;
  this.xmlDoc = null;
  return null;
}

/**
 * @desc: Convert string to it boolean representation
 * @type: private
 * @param: inputString - string for covertion
 */
function convertStringToBoolean(inputString) {
  if (typeof(inputString) == "string") inputString = inputString.toLowerCase();
  switch (inputString) {
    case "1":
    case "true":
    case "yes":
    case "y":
    case 1:
    case true: return true; break;
    default: return false;
  }
}

/**
 * @desc: find out what symbol to use as url param delimiters in further params
 * @type: private
 * @param: str - current url string
 */
function getUrlSymbol(s) {
  if (s.indexOf("?") != -1) {
    return "&";
  } else {
    return "?";
  }
}

function _myError(type, name, params) {
  if (!this.catches)
    this.catches = new Array();

  return this;
}

_myError.prototype.catchError = function(type, func_name) {
  this.catches[type] = func_name;
}

_myError.prototype.throwError = function(type, name, params) {
  if (this.catches[type]) return  this.catches[type](type, name, params);
  if (this.catches["ALL"]) return  this.catches["ALL"](type, name, params);
  alert("Error type: " + arguments[0] + "\nDescription: " + arguments[1]);
  return null;
}

window.myError = new _myError();
