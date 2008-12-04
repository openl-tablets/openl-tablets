
var im = new IconManager("item_enabled", "item_over", "item_disabled");

function setEnabled(who) {
    im.enable(who);
}

function setDisabled(who) {
    im.disable(who);
}

var save_items = ["save_all", "undo", "redo"];
var align_items = ["align_left", "align_center", "align_right"];
var move_items = ["move_row_down_button", "move_row_up_button", "move_column_right_button", "move_column_left_button"];
var addremove_items = ["add_row_before_button", "remove_row_button", "add_column_before_button", "remove_column_button"];
var other_items = ["help"];

[save_items, align_items, move_items, addremove_items, other_items].flatten().each(function(who) {
    im.init(who);
});
[other_items].flatten().each(setEnabled);