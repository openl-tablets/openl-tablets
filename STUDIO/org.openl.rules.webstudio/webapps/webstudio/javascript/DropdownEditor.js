/**
 * Base dropdown editor.
 *
 * @author Aliaksandr Antonik.
 */
var DropdownEditor = Prototype.emptyFunction;

DropdownEditor.prototype = Object.extend(new BaseEditor(), {
	/** Constructor */
	initialize : function() {
		this.node = document.createElement("select");
		this.node.style.width = "100%";
		this.node.style.border = "0px none";
		this.node.style.margin = "0px";
		this.node.style.padding = "0px";
		
		var self = this;
		$H({
			"": "-- Select a value --",
			"Item 1": "Mazda",
			"Item 2": "Opel",
			"Item 3": "Citroen",
			"Item 4": "Subaru"
		}).each(function (pair) {
			self.addOption(pair.key, pair.value);
		});
	},

	/**
	 *  @desc add an option element to this select
	 *  @type public
	 */
	addOption : function(/* String */ value, /* String */ name) {
		var optionElement = document.createElement("option");
		optionElement.value = value;
		optionElement.innerHTML = name;
		this.node.appendChild(optionElement);
	},

	/**
	 * @desc overrides base class implementation to support situation when nothing is selected.
	 */
	isCancelled: function() {
		return this.node.value == "";	
	}
});

TableEditor.Editors["selectbox"] = DropdownEditor.prototype;