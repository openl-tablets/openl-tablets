function jsCheck(id)
{
 el=document.getElementById(id);
 el.checked='checked';
}

function jsUnCheck(id)
{
 el=document.getElementById(id);
 el.checked='';
}


function jsCheckGroup(baseID, n)
{
  for(i=0; i < n; ++i)
  {
  jsCheck(baseID + i);
  }

}
