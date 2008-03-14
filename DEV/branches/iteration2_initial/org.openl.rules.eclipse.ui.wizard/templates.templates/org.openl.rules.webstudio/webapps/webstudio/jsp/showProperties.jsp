

   		<table class="data" border="0" width="718" cellspacing="0">
          <tr>
            <td class="column" width="16%">&nbsp;Property_Name&nbsp;</td>
            <td class="column" width="22%">Value</td>
            <td class="column">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Rule Name</td>
            <td class="rowHeightEven"><input name="property.name" type="text" value="<%=studio.getModel().showProperty(elementID, "name")%>" maxlength="40" size="40"></td>
            <td class="rowHeightEven">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Display Name</td>
            <td class="rowHeightOdd"><input name="property.display" type="text" value="<%=studio.getModel().showProperty(elementID, "display")%>" maxlength="40" size="40"></td>
            <td class="rowHeightOdd">&nbsp;</td>			
          </tr>
          <tr>
            <td class="columnNEven">Rule Description</td>
            <td class="rowHeightEven">
            <textarea name="property.description" cols="40">  <%=studio.getModel().showProperty(elementID, "description")%>" </textarea>
            </td>
            <td class="rowHeightEven">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Category</td>
            <td class="rowHeightOdd">
            <input name="property.category" type="text" value="<%=studio.getModel().showProperty(elementID, "category")%>" maxlength="40" size="40"/>
            </td>
            <td class="rowHeightOdd">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Active?</td>
            <td class="rowHeightEven"><input name="property.active" type="text" value="<%=studio.getModel().showProperty(elementID, "category")%>"/></td>
            <td class="rowHeightEven">&nbsp;</td>						
          </tr>
          <tr>
            <td class="column1">Effective Date</td>
            <td class="rowHeightOdd"><input name="property.effectiveDate" type="text" value="<%=studio.getModel().showProperty(elementID, "effectiveDate")%>" maxlength="20" size="20"></td>
            <td class="rowHeightOdd">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Created By</td>
            <td class="rowHeightEven"><input name="property.createdBy" type="text" value="<%=studio.getModel().showProperty(elementID, "createdBy")%>" maxlength="20" size="20"></td>
            <td class="rowHeightEven">&nbsp;</td>						
          </tr>
          <tr>
            <td class="column1">Create On</td>
            <td class="rowHeightOdd"><input name="property.createdOn" type="text" value="<%=studio.getModel().showProperty(elementID, "createdOn")%>" maxlength="20" size="20"></td>
            <td class="rowHeightOdd">&nbsp;</td>			
          </tr>
          <tr>
            <td class="column1">Modified By</td>
            <td class="rowHeightEven"><input name="property.modifiedBy" type="text" value="<%=studio.getModel().showProperty(elementID, "modifiedBy")%>" maxlength="20" size="20"></td>
            <td class="rowHeightEven">&nbsp;</td>						
          </tr>
          <tr>
            <td class="column1">Modified On</td>
            <td class="rowHeightOddLast"><input name="property.modifiedOn" type="text" value="<%=studio.getModel().showProperty(elementID, "modifiedOn")%>" maxlength="20" size="20"></td>
            <td class="rowHeightOddLast">&nbsp;</td>			
          </tr>
        </table>
