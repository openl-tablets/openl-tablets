function changeAllItemStatus(element, areaId) {
            $j("#"+areaId+" INPUT[type='checkbox']:not(:disabled)").prop("checked", element.checked);
        }
        
function changeItemStatus(element, areaId, selectAllElemId) {
    if(!element.checked && $(selectAllElemId).checked) {
        $(selectAllElemId).checked = false;
    }

    checkedCount = $j("#"+areaId+" INPUT[type='checkbox']:checked").not("INPUT[id='"+selectAllElemId+"']").size();
    disabledCount = $j("#"+areaId+" INPUT[type='checkbox']:disabled").not("INPUT[id='"+selectAllElemId+"']").size();
    allCount = $j("#"+areaId+" INPUT[type='checkbox']").not("INPUT[id='"+selectAllElemId+"']").size();

    if (checkedCount + disabledCount == allCount && checkedCount > 0) {
        $(selectAllElemId).checked = true;
    }
}