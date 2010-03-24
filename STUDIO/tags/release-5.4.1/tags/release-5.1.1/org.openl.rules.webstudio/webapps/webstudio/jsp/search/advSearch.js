<script type="text/javascript">

function body_load()
{
	document.getElementById('img_action').value = ''
}

function img_action_click(action)
{
	document.getElementById('img_action').value = action;
	document.getElementById('adv_search_form').submit();
}


function makeValue1Visible(typeID, vid)
{

  elType = 	document.getElementById(typeID);
  typeValue = elType.value;
  
  el = document.getElementById(vid);	
  <%for(int i=0; i < typeValues.length; ++i){%>

	if (typeValue == '<%=typeValues[i]%>')
	{
//	  el.style.visibility = '<%=typeNeedValue1[i] ? "visible" : "hidden"%>';
	  el.disabled = <%=typeNeedValue1[i] ? "false" : "true"%>;
	}
  <%}%>	
}


function jsclean(id,def_value)
{
 el=document.getElementById(id)
 if (el.value == def_value)
 {
   el.value='';
   el.className='search';
 }  
}

function jscleanAny(id,def_value)
{
 el=document.getElementById(id)
 if (el.value == def_value)
 {
   el.value='';
 }  
}




function jssetType(id)
{
 el=document.getElementById(id);
 el.checked='checked';
}

function jssetAllTypes()
{
<%
for(int i=0; i < tableTypeButtons.length; ++i)
{
  String id = "select" + tableTypeButtons[i];
%>
	jssetType('<%=id%>');
<%
}
%>

}

function alignGop(id)
{
 el=document.getElementById(id);
 tdel=document.getElementById('td' + id);
 value = el.value
 if (value.indexOf('-') == 0)
 {
   el.className='search-group';
   tdel.align='center';
   tdel.className='search-group';
 }  
 else
 {
   el.className='search-element';
   tdel.align='left';
   tdel.className='search-element';
 }  
}

function jsclearType(id)
{
 el=document.getElementById(id);
 el.checked='';
}

function jsclearAllTypes()
{
<%
for(int i=0; i < tableTypeButtons.length; ++i)
{
  String id = "select" + tableTypeButtons[i];
%>
	jsclearType('<%=id%>');
<%
}
%>
}
</script>
