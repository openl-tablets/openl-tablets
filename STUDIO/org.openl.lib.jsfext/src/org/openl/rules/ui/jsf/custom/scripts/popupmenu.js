var PopupMenu = {
	showChild: function (id, show)
	{
		document.getElementById(id).style.display = show ? "inline" : "none";
	},

	menu_ie: document.all,
	menu_ns6: document.getElementById && !document.all,
	menuON: false,
	menuHolderDiv : undefined,
	delayedFunction: undefined,
	disappearFunction: undefined,
	disappearInterval1: 5000,
	disappearInterval2: 1000,
	delayedState: {
		extraClass: undefined,
		evt: {},
		contentElement: undefined
	},

	_showPopupMenu: function (contentElement, event, extraClass) {
		this.cancelDisappear();

		if (this.menu_ns6)
		{
			this.menuHolderDiv.style.left = (event.clientX + document.body.scrollLeft + 5) + "px";
			this.menuHolderDiv.style.top = (event.clientY + document.body.scrollTop + 5) + "px";
		} else
		{
			this.menuHolderDiv.style.pixelLeft = event.clientX + document.body.scrollLeft + 5;
			this.menuHolderDiv.style.pixelTop = event.clientY + document.body.scrollTop + 5;
		}
		if (extraClass)
			this.menuHolderDiv.className = "menuholderdiv " + extraClass;
		else
			this.menuHolderDiv.className = "menuholderdiv";
		this.menuHolderDiv.innerHTML = document.getElementById(contentElement).innerHTML;
		this.menuHolderDiv.style.display = "inline";

		this.menuON = true;
		this.disappearFunction = setTimeout("PopupMenu.closeMenu()", this.disappearInterval1);
	},

	cancelDisappear : function() {
		if (this.disappearFunction) clearTimeout(this.disappearFunction);
		this.disappearFunction = undefined;
	},

	closeMenu: function () {
		this.cancelDisappear();
		if (this.menuON) {
			this.menuHolderDiv.style.display = "none";
		}
	},

	inMenuDiv: function (el) {
		if (el == undefined) return false;
		if (el == this.menuHolderDiv) return true;
		if (el.tagName && el.tagName.toLowerCase() == 'a') return false;
		return this.inMenuDiv(el.parentNode);
	},

	getTarget: function (e) {
		var evt = this.menu_ie ? window.event : e;
		var el = undefined;
		if (evt.target) {
			return evt.target;
		} else if (evt.srcElement) {
			return evt.srcElement;
		}
		;
		return undefined;
	},

	_init: function (contentElement, event, extraClass) {
		document.onclick = function(e) {
			var el = PopupMenu.getTarget(e);
			if (el && (el.name != 'menurevealbutton') && !PopupMenu.inMenuDiv(el))
				PopupMenu.closeMenu();
			return true;
		}

		try {
			this.menuHolderDiv = document.createElement('<div id="divmenu" class="menuholderdiv" style="display:none; float:none;z-index:5; position:absolute;">');
		} catch (e) {
			this.menuHolderDiv = document.createElement("div");
			this.menuHolderDiv.setAttribute("class", "menuholderdiv");
			this.menuHolderDiv.setAttribute("id", "divmenu");
			this.menuHolderDiv.style.display = "none";
			this.menuHolderDiv.style.cssFloat = "none";
			this.menuHolderDiv.style.zIndex = "5";
			this.menuHolderDiv.style.position = "absolute";
		}

		this.menuHolderDiv.onmouseout = function(e) {
			if (PopupMenu.getTarget(e) == PopupMenu.menuHolderDiv) {
				PopupMenu.cancelDisappear();
				PopupMenu.disappearFunction = setTimeout("PopupMenu.closeMenu()", PopupMenu.disappearInterval2);
			}
		}
		this.menuHolderDiv.onmouseover = function(e) {
			PopupMenu.cancelDisappear();
		}

		document.body.appendChild(this.menuHolderDiv);
		this.showPopupMenu = this._showPopupMenu;
		this.sheduleShowMenu = this._sheduleShowMenu;
	},

	cancelShowMenu: function() {
		if (this.delayedFunction) clearTimeout(this.delayedFunction);
		this.delayedFunction = undefined;
	},

	showAfterDelay : function() {
		this.menuHolderDiv.style.display = "none";
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
	showPopupMenu: function(a,b,c) {this._init(); this._showPopupMenu(a,b,c);},
	sheduleShowMenu: function(a,b,c,d) {this._init(); this._sheduleShowMenu(a,b,c,d);}
}
