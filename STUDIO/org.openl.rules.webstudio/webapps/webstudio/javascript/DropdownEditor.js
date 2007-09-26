/**
 * Base dropdown editor.
 *
 * @author Aliaksandr Antonik.
 */
var DropdownEditor = Prototype.emptyFunction;

DropdownEditor.prototype = Object.extend(new BaseEditor(), {
  /** Constructor */
  initialize : function(parent) {
    this.node = document.createElement("select");
    this.node.style.width = "100%";
    this.node.style.border = "0px none";
    this.node.style.margin = "0px";
    this.node.style.padding = "0px";

    var self = this;
    $H({
      "": "-- Select a value --",
       "AL": "ALABAMA",
       "AK": "ALASKA",
       "AS": "AMERICAN SAMOA",
       "AZ": "ARIZONA",
       "AR": "ARKANSAS",
       "CA": "CALIFORNIA",
       "CO": "COLORADO",
       "CT": "CONNECTICUT",
       "DE": "DELAWARE",
       "DC": "DISTRICT OF COLUMBIA",
       "FM": "FEDERATED STATES OF MICRONESIA",
       "FL": "FLORIDA",
       "GA": "GEORGIA",
       "GU": "GUAM",
       "HI": "HAWAII",
       "ID": "IDAHO",
       "IL": "ILLINOIS",
       "IN": "INDIANA",
       "IA": "IOWA",
       "KS": "KANSAS",
       "KY": "KENTUCKY",
       "LA": "LOUISIANA",
       "ME": "MAINE",
       "MH": "MARSHALL ISLANDS",
       "MD": "MARYLAND",
       "MA": "MASSACHUSETTS",
       "MI": "MICHIGAN",
       "MN": "MINNESOTA",
       "MS": "MISSISSIPPI",
       "MO": "MISSOURI",
       "MT": "MONTANA",
       "NE": "NEBRASKA",
       "NV": "NEVADA",
       "NH": "NEW HAMPSHIRE",
       "NJ": "NEW JERSEY",
       "NM": "NEW MEXICO",
       "NY": "NEW YORK",
       "NC": "NORTH CAROLINA",
       "ND": "NORTH DAKOTA",
       "MP": "NORTHERN MARIANA ISLANDS",
       "OH": "OHIO",
       "OK": "OKLAHOMA",
       "OR": "OREGON",
       "PW": "PALAU",
       "PA": "PENNSYLVANIA",
       "PR": "PUERTO RICO",
       "RI": "RHODE ISLAND",
       "SC": "SOUTH CAROLINA",
       "SD": "SOUTH DAKOTA",
       "TN": "TENNESSEE",
       "TX": "TEXAS",
       "UT": "UTAH",
       "VT": "VERMONT",
       "VI": "VIRGIN ISLANDS",
       "VA": "VIRGINIA",
       "WA": "WASHINGTON",
       "WV": "WEST VIRGINIA",
       "WI": "WISCONSIN",
       "WY": "WYOMING"                         
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