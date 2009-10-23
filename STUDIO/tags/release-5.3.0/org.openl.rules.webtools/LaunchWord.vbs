Dim wdPath, wdName, wdParNum


'MsgBox "Start"
wdPath=wscript.arguments(0)
wdName=wscript.arguments(1)
wdParStart=wscript.arguments(2)
wdParEnd=wscript.arguments(3)

	launch2 wdPath, wdName, wdParStart, wdParEnd
	
    
    
    
    
Sub test1()
'MsgBox Word.ActiveDocument.Paragraphs.Count

' Word.ActiveDocument.Paragraphs(4180).Range.Text
'Set wordApp = getWord
launch "C:\__exigen\customer\SRP", "ReservesDomainAnalysis_5.6.2_DBR.doc", 700

'wDocc.Paragraphs(355).range.Select



End Sub



Function getWord()
    Dim wordApp
    
On Error Resume Next
    Set wordApp = GetObject(, "Word.Application")
On Error GoTo 0

    If IsEmpty(wordApp) Then
        Set wordApp = CreateObject("Word.Application")
    End If
    
    wordApp.Visible = True
    
    Set getWord = wordApp
End Function

Function getDocument(wordApp, wdPath, wdName)
    Dim wb
    
    For Each wd In wordApp.Documents
   ' MsgBox "path=" + wd.Path + " name=" + wd.Name
    
    If wd.Name = wdName Then
        
        If wd.Path = wdPath Then
            Set getDocument = wd
		'	MsgBox wdName + " 1 "   
            Exit Function
        Else
        	'MsgBox wdName + " 2 "   
        	
            MsgBox "Word can not open two Documents with the same name but in different directories!"
        End If
    
    End If
    
    Next
'	MsgBox wdName + " 3 "   
    
    Set getDocument = wordApp.Documents.Open(wdPath + "\" + wdName)
    
End Function

Sub launch(wdPath, wdName, wdPar)
    
    Dim wordApp
    Dim wdDoc
    
    
    
    Set wordApp = getWord()
    Set wdDoc = getDocument(wordApp, wdPath, wdName)

'	MsgBox wdName + " 1 "   
	 
	wordApp.Windows(wdName).Activate
	wordApp.Windows(wdName).setFocus
    
    
    wdDoc.Activate
    
    wdDoc.Paragraphs(wdPar).range.Select


End Sub

Sub launch2(wdPath, wdName, wdParStart, wdParEnd)

    Dim wordApp
    Dim wdDoc
    
    
    
    Set wordApp = getWord()
    Set wdDoc = getDocument(wordApp, wdPath, wdName)
 '   MsgBox wdName + " 4 "   
    
    
'	wordApp.Windows(wdName).Activate
	'wordApp.Windows(wdName).setFocus
    
    
    wdDoc.Activate
    
'    wdDoc.Paragraphs(wdPar).range.Select

	Set myRange = wdDoc.Paragraphs(wdParStart).Range
		myRange.SetRange myRange.Start, wdDoc.Paragraphs(wdParEnd).Range.End
	myRange.Select	
	
	Set wdDoc = Nothing
	Set wordApp = Nothing

End Sub
