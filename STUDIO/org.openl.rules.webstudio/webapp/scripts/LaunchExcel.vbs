Dim wbPath, wbName , wsName , range

wbPath=wscript.arguments(0)
wbName=wscript.arguments(1)
wsName=wscript.arguments(2)
range=wscript.arguments(3)

	launch wbPath, wbName, wsName, range
	
Function getExcel()
    Dim excelApp
    
On Error Resume Next
    Set excelApp = GetObject(, "Excel.Application")
On Error GoTo 0

    If IsEmpty(excelApp) Then
    	Set excelApp = CreateObject("Excel.Application")
    End If
    
    excelApp.Visible = True
    
    Set getExcel = excelApp
End Function

Function getWorkbook(excelApp , wbPath, wbName )
    Dim wb
    
    For Each wb In excelApp.Workbooks
    ' MsgBox "path=" + wb.Path + " name=" + wb.Name
    
    If wb.Name = wbName Then
        
        If wb.Path = wbPath Then
            Set getWorkbook = wb
            Exit Function
        Else
            MsgBox "Excel can not open two WorkBooks with the same name but in different directories!"
        End If
    
    End If
    
    Next 
    
    Set getWorkbook = excelApp.Workbooks.Open(wbPath + "\" + wbName)
    
End Function

Sub launch(wbPath, wbName , wsName , range )
    
    Dim excelApp
    Dim wb
    Dim ws
    
    
    Set excelApp = getExcel()
    Set wb = getWorkbook(excelApp, wbPath, wbName)
    
    
    
    If wsName = "1" Then
       Set ws = wb.Worksheets(1)
    Else
       Set ws = wb.Worksheets(wsName)
    End If


    If ws.Visible Then
        ws.Activate
        ws.range(range).Activate
    Else
        MsgBox "Selected sheet is hidden", vbSystemModal
    End If

End Sub

