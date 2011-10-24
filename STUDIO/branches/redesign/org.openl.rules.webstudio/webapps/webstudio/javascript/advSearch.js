function init() {
	$j('input[id^="advSearchForm\\:typeValue_"]').each(function() {
	    var pair = this.id.split("_");
	    var index = pair[1];
	    makeValue1Visible(document.getElementById("advSearchForm:type1_" + index), index);
    });
}

function body_load() {
	document.getElementById('img_action').value = '';
}

function img_action_click(action) {
	document.getElementById('img_action').value = action;
	document.getElementById('adv_search_form').submit();
}

function makeValue1Visible(el, index) {
    document.getElementById("advSearchForm:typeValue_" + index).disabled = el.value == 'header';
}

function jsclean(id,def_value) {
 el=document.getElementById(id)
 if (el.value == def_value)
 {
   el.value='';
   el.className='search';
 }  
}

function jssetType(id) {
    var el = document.getElementById(id);
    el.checked = 'checked';
}

function jsSetTableTypes(checked) {
    var value = checked ? 'checked' : '';
    var elements = document.getElementsByName('advSearchForm:tableType');
    for (var i = 0; i < elements.length; ++i)
        if (elements[i].tagName == 'INPUT')
            elements[i].checked = value;
}

function alignGop(el) {
    var tdel = el;
    while (tdel && tdel.tagName != 'TD') tdel = tdel.parentNode;
    var value = el.value
    if (value.indexOf('-') == 0)
    {
        el.className = 'search-group';
        tdel.align = 'center';
        tdel.className = 'search-group';
    }
    else
    {
        el.className = 'search-element';
        tdel.align = 'left';
        tdel.className = 'search-element';
    }
}

function jsclearType(id) {
 el=document.getElementById(id);
 el.checked='';
}

init();
