function initUserDisplayNameSelect($displayNameBox, $firstName, $lastName, $displayName) {
    $displayNameBox.html(
        '<select size="1">' +
             '<option value="firstLast" selected="">First Last</option>' +
             '<option value="lastFirst">Last First</option>' +
             '<option value="other">Other</option>' +
         '</select>'
    );
    let $displayNameSelect = $j($displayNameBox.children()[0]);
    $displayNameSelect.attr("name", $displayNameBox.attr("id"));
    $firstName.keyup(function() {
        configureDisplayNameField($displayNameSelect, $firstName, $lastName, $displayName);
    });
    $lastName.keyup(function() {
        configureDisplayNameField($displayNameSelect, $firstName, $lastName, $displayName);
    });
    $displayNameSelect.change(function() {
       configureDisplayNameField($displayNameSelect, $firstName, $lastName, $displayName);
    });

    if ($displayName.val() === "") {
       configureDisplayNameField($displayNameSelect, $firstName, $lastName, $displayName);
    } else {
       configureDisplayNameSelect($displayNameSelect, $firstName, $lastName, $displayName);
    }
}

function configureDisplayNameField($displayNameSelect, $firstName, $lastName, $displayName) {
    if ($displayNameSelect.val() === 'firstLast') {
        $displayName.css("pointer-events", "none");
        $displayName.val(($firstName.val() + ' ' + $lastName.val()).trim());
    } else if ($displayNameSelect.val() === 'lastFirst') {
        $displayName.css("pointer-events", "none");
        $displayName.val(($lastName.val() + ' ' + $firstName.val()).trim());
    } else {
        $displayName.css("pointer-events", "");
    }
    $displayName.change();
}

function configureDisplayNameSelect($displayNameSelect, $firstName, $lastName, $displayName) {
    if ($displayName.val() === ($firstName.val() + ' ' + $lastName.val()).trim()) {
        $displayNameSelect.val('firstLast');
        $displayName.css("pointer-events", "none");
    } else if ($displayName.val() === ($lastName.val() + ' ' + $firstName.val()).trim()) {
        $displayNameSelect.val('lastFirst');
        $displayName.css("pointer-events", "none");
    } else {
        $displayName.css("pointer-events", "");
        $displayNameSelect.val('other');
    }
}
