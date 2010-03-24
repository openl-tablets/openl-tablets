<head>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>

<style>

.search-table
{
border-style: solid solid solid solid ; border-width: 1 1 1 1; border-color: #000000 000000 000000 #000000;
}

td.search-group
{
font-family: arial,sans-serif;
background-color: #ffeeee;
border-style: solid solid solid solid ; border-width: 1 1 1 1; border-color: #000000 000000 000000 #000000;
}

select.search-group
{
font-family: arial,sans-serif;
font-size:120%;
}


.search-element
{
font-family: arial,sans-serif;
font-size:90%;
background-color: white;
}

th.search
{
font-family: arial,sans-serif;
font-size:90%;
color:#8B4513;
}

td.search
{
font-family: arial,sans-serif;
font-size:90%;
}

input.search-empty
{
font-family: arial,sans-serif;
font-size:90%;
font-style: italic;
color: lightgrey;
}

input.search
{
font-family: arial,sans-serif;
font-size:90%;
font-style: bold;
color: red;
}


</style>


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
  

	if (typeValue == 'header')
	{
//	  el.style.visibility = 'hidden';
	  el.disabled = true;
	}
  

	if (typeValue == 'property')
	{
//	  el.style.visibility = 'visible';
	  el.disabled = false;
	}
  	
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

	jssetType('selectRules');

	jssetType('selectData');

	jssetType('selectMethod');

	jssetType('selectDatatype');

	jssetType('selectTest');

	jssetType('selectRun');

	jssetType('selectEnv');

	jssetType('selectOther');


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

	jsclearType('selectRules');

	jsclearType('selectData');

	jsclearType('selectMethod');

	jsclearType('selectDatatype');

	jsclearType('selectTest');

	jsclearType('selectRun');

	jsclearType('selectEnv');

	jsclearType('selectOther');

}
</script>


</head>

<body onload="body_load()">

<h2>Search</h2>


<form id="adv_search_form" action="advSearch.jsp" >

<fieldset>
<legend>Table Selector</legend>



<p/>

<table class="search-table" cellspacing=10 cellpadding=2>
<tr>
<th class="search" >Business Attribute</th>
<th class="search">Condition</th>
<th class="search">Value</th>
</tr>

 
<tr>
<td class="search-table">
	<select name="type0"  id="type0">
	
	<option value="LOB"  selected='selected'> LOB </option>
	</select>	
</td>
<td class="search-table">
	<select name="type0"  id="type0">
	
	 <option value="is"  selected='selected'> is </option>
	 <option value="is-one-of" > is one of </option>
	</select>
</td>
<td class="search-table">
	<select name="opType0"  id="opType0">
	
	<option value="Auto"  selected='selected' > Auto </option>
	
	<option value="Home"   > Home</option>
	<option value="Workers Compensation"   > Worker's Compensation</option>
	<option value="Yacht"   > Yacht</option>
</td>
</tr>

<tr>
<td class="search-table">
	<select name="type0"  id="type0">
	
	<option value="LOB"> LOB </option>
	<option value="Region"  selected='selected'> Region </option>
	</select>	
</td>

<td class="search-table">
	<select name="type0"  id="type0">
	
	 <option value="is"  selected='selected'> is </option>
	 <option value="is-one-of" > is one of </option>
	</select>
</td>
<td class="search-table">
	<select name="opType0"  id="opType0">
	
	<option value="Eastern"  selected='selected' > Eastern </option>
	
	<option value="Central"   > Central</option>
	<option value="Western"   > Western</option>
</td>
</tr>


<tr>
<td class="search-table">
	<select name="type3"  id="type3" >
	
	<option value="LOB"> LOB </option>
	<option value="Region"> Region </option>
	<option value="UW Authority"  selected='selected'> UW Authority </option>
	</select>	
</td>

<td class="search-table">
	<select name="type31"  id="type31">
	
	 <option value="is"  selected='selected'> is </option>
	 <option value="is-one-of" > is one of </option>
	</select>
</td>

<td class="search-table">
	<select name="opType32"  id="opType32">
	
	<option value="RM"  selected='selected' >Regional Manager</option>
	
	<option value="SU"   > Senior Underwriter</option>
	<option value="UW"   > Underwriter</option>
</td>
</tr>


<tr>
<td class="search-table">
	<select name="type3"  id="type3" >
	
	<option value="LOB"> LOB </option>
	<option value="Region"> Region </option>
	<option value="Total Schedule Limit"  selected='selected'> Total Schedule Limit </option>
	</select>	
</td>

<td class="search-table">
	<select name="type31"  id="type31">
	
	 <option value="is more than"  selected='selected'> is more than </option>
	 <option value="is less than" > is less than </option>
	</select>
</td>

<td class="search-table">
	<input name="v1" value="$5M"></input>
</td>
</tr>


</table>



</fieldset>
<input type="submit" value="Search" name="Search" />


<h3>Saved Searches</h3>


&nbsp; <img src="../../images/search.png"/> <a href="#">All Issue Rules - Auto</a><p/>
&nbsp; <img src="../../images/search.png"/> <a href="#">All Renewal Rules - Yacht</a><p/>
&nbsp; <img src="../../images/search.png"/> <a href="#">All Rules &gt; $10M</a><p/>
 


<h3>Save/Edit Search</h3>

<table class="search-table" cellspacing=10 cellpadding=2>
<tr>
<td class="search-table">
Name: <td class="search-table"><input type="text" value="Driver Rules"/>
</tr>
<tr>
<td class="search-table">
Category: <td class="search-table"><input type="text" value="Driver"/>
</tr>
<tr>
<td class="search-table">
Use in Project View:</td> <td class="search-table"><input type="checkbox" value=""/></td>
<td class="search-table"><i> If checked this Search will appear in Tree View Configuration Dialog and can be used for filtering Project Components(Tables)</i></td>
</tr>
<tr>
<td class="search-table">
Use in Table View:</td> <td class="search-table"><input type="checkbox" value=""/></td>
<td class="search-table"><i> If checked this Search will appear in Table View Filter Table combobox and can be used for filtering Rows and Columns</i></td>
</tr>
<tr>
<td><input type="submit" value="Save Search" name="Save Search" /></td>

</table>




<input id="img_action" name="img_action" value="" style="visibility:hidden"/>

</form>



<h2>Search Result</h2>

<input type="submit" value="Search Again" name="Search Again" />
<input type="submit" value="Save Search" name="Save Search" />

<p>
<a href="/webstudio/jsp/showLinks.jsp?&wbPath=C:\___DEV\org.openl.dev_3.1\eclipse\workspace\com.exigen.demo.aig.uw\rules&wbName=UWDemo.xls&wsName=Rules&range=D6"' target='show_app_hidden'>Rules-Issue-EasternZone</a><p>
<table cellspacing=0 cellpadding=1>
<tr>

<td  align=center width=267 bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,0,event)' id='c0x0'>Who can release to Broker</div></td>
<td  align=center valign=bottom width=139 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(1,0,event)' id='c1x0'>Condition</div></td>
<td  align=center valign=bottom width=107 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(2,0,event)' id='c2x0'>Roles</div></td>
<td  align=center valign=bottom width=134 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(3,0,event)' id='c3x0'>Condition</div></td>
<td  align=center valign=bottom width=160 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(4,0,event)' id='c4x0'>Roles</div></td>
<td  align=center valign=bottom width=109 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(5,0,event)' id='c5x0'>Condition</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(6,0,event)' id='c6x0'>Roles</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(7,0,event)' id='c7x0'>Condition</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(8,0,event)' id='c8x0'>Roles</div></td>

<td  align=center width=79 bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 2 2 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(9,0,event)' id='c9x0'>&nbsp;</div></td>
</tr>
<tr>
<td  align=center bgcolor=#ccffff style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,1,event)' id='c0x1'># Lines Quoted:</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(1,1,event)' id='c1x1'>3+</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(2,1,event)' id='c2x1'>1,2,&nbsp;3</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(3,1,event)' id='c3x1'>1-7</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(4,1,event)' id='c4x1'>4,5,6,7</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(5,1,event)' id='c5x1'>1+</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(6,1,event)' id='c6x1'>14,15</div></td>

<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(7,1,event)' id='c7x1'>&nbsp;</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(8,1,event)' id='c8x1'>&nbsp;</div></td>
<td  valign=bottom bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(9,1,event)' id='c9x1'>NLQ</div></td>
</tr>
<tr>
<td  align=center bgcolor=#ccffff style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,2,event)' id='c0x2'>Deviations from Standard Commissions</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(1,2,event)' id='c1x2'>Lower</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(2,2,event)' id='c2x2'>2,&nbsp;3,&nbsp;4,&nbsp;5,&nbsp;6</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(3,2,event)' id='c3x2'>Higher,&nbsp;Lower</div></td>

<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(4,2,event)' id='c4x2'>7</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(5,2,event)' id='c5x2'>Higher</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(6,2,event)' id='c6x2'>14,15</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(7,2,event)' id='c7x2'>&nbsp;</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(8,2,event)' id='c8x2'>&nbsp;</div></td>
<td  valign=bottom bgcolor=#ff9900 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(9,2,event)' id='c9x2'>DSC</div></td>
</tr>
</table>
<p>
<a href="/webstudio/jsp/showLinks.jsp?&wbPath=C:\___DEV\org.openl.dev_3.1\eclipse\workspace\com.exigen.demo.aig.uw\rules&wbName=UWDemo.xls&wsName=Rules&range=D27"' target='show_app_hidden'>Rules-Issue-WesternZone</a><p>
<table cellspacing=0 cellpadding=1>
<tr>

<td  align=center width=267 bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,0,event)' id='c0x0'>Who can release to Broker</div></td>
<td  align=center valign=bottom width=139 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(1,0,event)' id='c1x0'>Condition</div></td>
<td  align=center valign=bottom width=107 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(2,0,event)' id='c2x0'>Roles</div></td>
<td  align=center valign=bottom width=134 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(3,0,event)' id='c3x0'>Condition</div></td>
<td  align=center valign=bottom width=160 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(4,0,event)' id='c4x0'>Roles</div></td>
<td  align=center valign=bottom width=109 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(5,0,event)' id='c5x0'>Condition</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(6,0,event)' id='c6x0'>Roles</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(7,0,event)' id='c7x0'>Condition</div></td>
<td  align=center valign=bottom width=79 bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 2 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(8,0,event)' id='c8x0'>Roles</div></td>

<td  align=center width=79 bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 2 2 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(9,0,event)' id='c9x0'>&nbsp;</div></td>
</tr>
<tr>
<td  align=center bgcolor=#ccffff style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,1,event)' id='c0x1'># Lines Quoted:</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(1,1,event)' id='c1x1'>3+</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(2,1,event)' id='c2x1'>1,2,&nbsp;3</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(3,1,event)' id='c3x1'>1-7</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(4,1,event)' id='c4x1'>4,5,6,7</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(5,1,event)' id='c5x1'>1+</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(6,1,event)' id='c6x1'>14,15</div></td>

<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(7,1,event)' id='c7x1'>&nbsp;</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(8,1,event)' id='c8x1'>&nbsp;</div></td>
<td  valign=bottom bgcolor=#ff9900 style=" border-style: solid solid solid none ; border-width: 1 1 1 0; border-color: 000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(9,1,event)' id='c9x1'>NLQ</div></td>
</tr>
<tr>
<td  align=center bgcolor=#ccffff style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 10; font-weight: bold; color: 000000"><div  onMouseDown='clickCell(0,2,event)' id='c0x2'>Deviations from Standard Commissions</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(1,2,event)' id='c1x2'>Lower</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(2,2,event)' id='c2x2'>2,&nbsp;3,&nbsp;4,&nbsp;5,&nbsp;6</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(3,2,event)' id='c3x2'>Higher,&nbsp;Lower</div></td>

<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(4,2,event)' id='c4x2'>7</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(5,2,event)' id='c5x2'>Higher</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(6,2,event)' id='c6x2'>14,15</div></td>
<td  align=center valign=bottom bgcolor=#ffff99 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(7,2,event)' id='c7x2'>&nbsp;</div></td>
<td  align=center valign=bottom bgcolor=#ffcc00 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(8,2,event)' id='c8x2'>&nbsp;</div></td>
<td  valign=bottom bgcolor=#ff9900 style=" border-style: none solid solid none ; border-width: 0 1 1 0; border-color: #000000 000000 000000 #000000;font-family: Arial; font-size: 12; color: 000000"><div  onMouseDown='clickCell(9,2,event)' id='c9x2'>DSC</div></td>
</tr>
</table>
<p>


</body>
</html>

