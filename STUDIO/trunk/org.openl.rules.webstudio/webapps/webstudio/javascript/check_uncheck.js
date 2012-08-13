function changeAllItemStatus(element, areaId) {
            $j("#"+areaId+" INPUT[type='checkbox']").prop("checked", element.checked);
        }
        
function changeItemStatus(element, areaId, selectAllElemId) {
    if(!element.checked && $(selectAllElemId).checked) {
        $(selectAllElemId).checked = false;
    }

    checkedCount = $j("#"+areaId+" INPUT[type='checkbox']:checked").not("INPUT[id='"+selectAllElemId+"']").size();
    allCount = $j("#"+areaId+" INPUT[type='checkbox']").not("INPUT[id='"+selectAllElemId+"']").size();

    if (checkedCount == allCount) {
        $(selectAllElemId).checked = true;
    }
}