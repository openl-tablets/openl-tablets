<HTML>
<HEAD>

<TITLE>
IDAT
</TITLE>
</HEAD>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<%
    org.openl.rules.ui.ProjectIndexer indexer = studio.getModel().getIndexer();
    boolean hasLetters = (indexer != null) && (indexer.getLetters().length > 0);

    if (hasLetters) {
%>

<FRAMESET rows="37,*" title="" onLoad="top.loadFrames()">
<FRAME src="indexLetters.jsp" name="letters" title="Index Letters">
<FRAME src="allIndex.jsp" name="allIndex" title="Search Index">

</FRAMESET>
<NOFRAMES>
<H2>
Frame Alert</H2>

<P>
This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
<BR>
Link to<A HREF="overview-summary.html">Non-frame version.</A>
</NOFRAMES>

<%
    } else {
%>
<BODY>
<P>
There are no available index results.
</BODY>
<%
    }
%>
