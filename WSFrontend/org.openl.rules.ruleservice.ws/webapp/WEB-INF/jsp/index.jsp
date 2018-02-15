<%@ page import="org.openl.rules.ruleservice.servlet.controller.ServicesController" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>OpenL Tablets Web Services</title>
    <base href="${pageContext.request.contextPath}/"/>
    <style>
        body {
            margin: 0;
            color: #444;
            font-family: verdana, helvetica, arial, sans-serif;
            font-size: 12px;
        }

        h2 {
            font-weight: normal;
            font-size: 16px;
            color: #28b;
            margin: 29px 0 16px;
        }

        h3 {
            font-weight: normal;
            font-size: 14px;
            margin: 3px 0;
        }

        a {
            color: #0078D0;
            margin-right: 10px;
        }

        #header {
            border-bottom: 1px solid #ccc;
            font-family: georgia, verdana, helvetica, arial, sans-serif;
            font-size: 20px;
            color: #777;
            padding: 11px 15px;
        }

        #main {
            padding: 0 20px 40px;
            color: #444;
            white-space: nowrap;
        }

        #footer {
            border-top: 1px solid #ccc;
            font-size: 11px;
            color: #666;
            padding: 11px;
            text-align: center;
            background: #fff;
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
        }

        #footer a {
            color: #666;
            text-decoration: none;
        }

        .note {
            color: #9a9a9a;
            font-size: 10px;
            margin: 3px 0;
        }

        #main > div {
            border-bottom: #cccccc dotted 1px;
            padding: 10px 0;

        }

        #main > div:last-child {
            border: 0;
        }
    </style>
</head>

<body>
<div id="header">OpenL Tablets Web Services</div>
<div id="main"></div>
<div id="footer">&#169; 2018 <a href="http://openl-tablets.org" target="_blank">OpenL Tablets</a></div>
<script>
    // Get JSON of available services
    var services = <%= ServicesController.getServices(request) %>;

    // The block for rendering of the available services
    var mainBlock = document.getElementById("main");

    if (Array.isArray(services) && services.length > 0) {
        mainBlock.innerHTML = "<h2>Available services:</h2>";
        services.forEach(function (service) {
            var html = createServiceHtml(service);
            var el = document.createElement("DIV");
            el.innerHTML = html;
            mainBlock.appendChild(el);
        });
        mainBlock.addEventListener('click', function (event) {
            var button = event.target || event.srcElement;
            if (button.className == "expand-button") {
                // Expand the node
                button.className = "collapse-button";
            } else if (button.className == "collapse-button") {
                // Collapse the node
                button.className = "expand-button";
            }
        })
    } else {
        mainBlock.innerHTML = "<h2>There are no available services</h2>";
    }

    // Creating innerHTML of one service
    function createServiceHtml(service) {
        var html = "";
        // Name
        html += "<h3>" + service.name + "</h3>";
        // Date and time
        html += "<div class='note'>Started time: " + new Date(service.startedTime).toLocaleString() + "</div>";
        // URLs
        service.serviceResources.forEach(function (resource) {
            html += "<a href='" + resource.url + "'\>" + resource.name + "</a>";
        });
        return html;
    }
</script>
</body>
</html>
