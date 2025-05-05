<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>OpenL Studio</title>
    <link rel="icon" href="${pageContext.request.contextPath}/icons/favicon.ico?v=studio" sizes="48x48" />
    <link rel="icon" href="${pageContext.request.contextPath}/favicon.svg?v=studio" sizes="any" type="image/svg+xml"/>
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/apple-touch-icon.png?v=studio"/>
    <link rel="manifest" href="${pageContext.request.contextPath}/icons/site.webmanifest?v=studio" />
</head>
<body>
    <div id="appRoot" style="height: 48.5px"></div>
    <script type="module" src="${reactUiRoot}/main.js"></script>
</body>
</html>
