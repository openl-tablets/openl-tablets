// @Deprecated
var PopupMenu = {
	showChild: function (id, show)
	{
		document.getElementById(id).style.display = show ? "inline" : "none";
	},

	menu_ie: !!(window.attachEvent && !window.opera),
	menu_ns6: document.getElementById && !document.all,
	menuON: false,
	te_menu : undefined,
	delayedFunction: undefined,
	disappearFunction: undefined,
	disappearInterval1: 5000,
	disappearInterval2: 1000,
	delayedState: {
		extraClass: undefined,
		evt: {},
		contentElement: undefined
	},
    lastTarget: null,
    

    getWindowSize: function () {
		var myWidth = 0, myHeight = 0;
		if (typeof( window.innerWidth ) == 'number') {
			//Non-IE
			myWidth = window.innerWidth;
			myHeight = window.innerHeight;
		} else if (document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight )) {
			//IE 6+ in 'standards compliant mode'
			myWidth = document.documentElement.clientWidth;
			myHeight = document.documentElement.clientHeight;
		} else if (document.body && ( document.body.clientWidth || document.body.clientHeight )) {
			//IE 4 compatible
			myWidth = document.body.clientWidth;
			myHeight = document.body.clientHeight;
		}
		return [myWidth, myHeight];
	},
	
	getScrollXY: function () {
		var scrOfX = 0, scrOfY = 0;
		if (typeof( window.pageYOffset ) == 'number') {
			//Netscape compliant
			scrOfY = window.pageYOffset;
			scrOfX = window.pageXOffset;
		} else if (document.body && ( document.body.scrollLeft || document.body.scrollTop )) {
			//DOM compliant
			scrOfY = document.body.scrollTop;
			scrOfX = document.body.scrollLeft;
		} else if (document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop )) {
			//IE6 standards compliant mode
			scrOfY = document.documentElement.scrollTop;
			scrOfX = document.documentElement.scrollLeft;
		}
		return [ scrOfX, scrOfY ];
	},

	_showPopupMenu: function (contentElement, event, extraClass) {
		this.cancelDisappear();

		var scrollXY = this.getScrollXY();
		var windowSizeXY = this.getWindowSize();

		this.te_menu.style.visibility = "hidden";
		this.te_menu.innerHTML = document.getElementById(contentElement).innerHTML;
		this.te_menu.style.display = "inline";
		var divWidth = this.te_menu.clientWidth;
		var divHeight = this.te_menu.clientHeight;

		var posX = event.clientX + 5; var delta = 25;
		if (posX + delta + divWidth > windowSizeXY[0]) posX = windowSizeXY[0] - delta - divWidth;
		if (posX < 0) posX = 0;
		var posY = event.clientY + 5; delta = 5;
		if ( (window.opera && document.body.scrollWidth > windowSizeXY[0])
				  || (window.scrollMaxX && window.scrollMaxX > 0))
			delta = 25;

		if (posY + delta + divHeight > windowSizeXY[1]) posY = event.clientY - 5 - divHeight;
		if (posY < 0) posY = windowSizeXY[1] - delta - divHeight;

		posX += scrollXY[0];posY += scrollXY[1];
		if (this.menu_ns6) {
			this.te_menu.style.left = posX + "px";
			this.te_menu.style.top = posY + "px";
		} else {
			this.te_menu.style.pixelLeft = posX;
			this.te_menu.style.pixelTop = posY;
		}
		if (extraClass)
			this.te_menu.className = "te_menu " + extraClass;
		else
			this.te_menu.className = "te_menu";

		this.te_menu.style.visibility = "visible";
		this.menuON = true;
		this.disappearFunction = setTimeout("PopupMenu.closeMenu()", this.disappearInterval1);

        this.lastTarget = this.delayedState.evt.target || this.delayedState.evt.srcElement;
    },

	cancelDisappear : function() {
		if (this.disappearFunction) clearTimeout(this.disappearFunction);
		this.disappearFunction = undefined;
	},

	closeMenu: function () {
		this.cancelDisappear();
		if (this.menuON) {
			this.te_menu.style.display = "none";
		}
	},

	inMenuDiv: function (el) {
		if (el == undefined) return false;
		if (el == this.te_menu) return true;
		if (el.tagName && el.tagName.toLowerCase() == 'a') return false;
		return this.inMenuDiv(el.parentNode);
	},

	getTarget: function (e) {
		var evt = this.menu_ie ? window.event : e;
		if (evt.target) {
			return evt.target;
		} else if (evt.srcElement) {
			return evt.srcElement;
		}
		return undefined;
	},

	_init: function () {
		document.onclick = function(e) {
			var el = PopupMenu.getTarget(e);
			if (el && (el.name != 'menurevealbutton') && !PopupMenu.inMenuDiv(el))
				PopupMenu.closeMenu();
			return true;
		};

		try {
			this.te_menu = document.createElement('<div id="divmenu" class="te_menu" style="display:none; float:none;z-index:5; position:absolute;">');
		} catch (e) {
			this.te_menu = document.createElement("div");
			this.te_menu.setAttribute("class", "te_menu");
			this.te_menu.setAttribute("id", "divmenu");
			this.te_menu.style.display = "none";
			this.te_menu.style.cssFloat = "none";
			this.te_menu.style.zIndex = "5";
			this.te_menu.style.position = "absolute";
		}

		this.te_menu.onmouseout = function(e) {
			if (PopupMenu.getTarget(e) == PopupMenu.te_menu) {
				PopupMenu.cancelDisappear();
				PopupMenu.disappearFunction = setTimeout("PopupMenu.closeMenu()", PopupMenu.disappearInterval2);
			}
		};
		this.te_menu.onmouseover = function() {
			PopupMenu.cancelDisappear();
		};

		document.body.appendChild(this.te_menu);
		this.showPopupMenu = this._showPopupMenu;
		this.sheduleShowMenu = this._sheduleShowMenu;
	},

	cancelShowMenu: function() {
		if (this.delayedFunction) clearTimeout(this.delayedFunction);
		this.delayedFunction = undefined;
	},

	showAfterDelay : function() {
        if (!document.getElementById(this.delayedState.contentElement)) return;
		this.te_menu.style.display = "none";
		this._showPopupMenu(this.delayedState.contentElement, this.delayedState.evt, this.delayedState.extraClass);
    },

	_sheduleShowMenu: function(contentElement, event, delay, extraClass) {
		this.cancelShowMenu();
		this.delayedState.evt.clientX = event.clientX;
		this.delayedState.evt.clientY = event.clientY;
		this.delayedState.evt.target = event.target ? event.target : undefined;
		this.delayedState.evt.srcElement = event.srcElement ? event.srcElement : undefined;
		this.delayedState.extraClass = extraClass;
		this.delayedState.contentElement = contentElement;

		this.delayedFunction = setTimeout("PopupMenu.showAfterDelay()", delay);
	},

	// init
	showPopupMenu: function() {this._init(); this._showPopupMenu.apply(this, arguments);},
	sheduleShowMenu: function() {this._init(); this._sheduleShowMenu.apply(this, arguments);}
};