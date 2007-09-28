/**
 * Suggest input editor.
 *
 * @author Andrey Naumenko
 */

document.write("<script src='" + jsPath + "suggest/suggest.js'></script>");
document.write("<link rel='stylesheet' type='text/css' href='" + jsPath + "suggest/suggest.css'></link>");

var SuggestEditor = Class.create();

SuggestEditor.prototype = Object.extend(new BaseEditor(), {

/** Constructor */
  editor_initialize: function() {
    this.node = document.createElement("input");
    this.node.setAttribute("type", "text");
    this.node.style.border = "0px none";
    this.node.style.height = "100%";
    this.node.style.margin = "0px";
    this.node.style.padding = "0px";
    this.node.style.width = "100%";
    this.node.setAttribute("id", "statesautocomplete");
    cell.appendChild(this.node);
    this.node.focus();

    var states = new Array("Alabama", "Alaska", "American Samoa", "Arizona", "Arkansas", "California",
      "Colorado", "Connecticut", "Delaware", "District of Columbia",
      "Federated States of Micronesia", "Florida", "Georgia", "Guam", "Hawaii", "Idaho", "Illinois",
      "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine",
      "Marshall Islands", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
      "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico",
      "New York", "North Carolina", "North Dakota",
      "Northern Mariana Islands", "Ohio", "Oklahoma", "Oregon", "Palau", "Pennsylvania", "Puerto Rico",
      "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont",
      "Virgin Islands", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming",
      "Armed Forces Africa", "Armed Forces Americas", "Armed Forces Canada",
      "Armed Forces Europe", "Armed Forces Middle East", "Armed Forces Pacific");

    new AutoSuggest(document.getElementById('statesautocomplete'), states);

  }
});

TableEditor.Editors["suggestbox"] = SuggestEditor;
