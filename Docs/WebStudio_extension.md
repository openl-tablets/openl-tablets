# Extending OpenL WebStudio UI by external JavaScript

In cases where one needs to add additional functionality to the OpenL WebStudio UI, it can be achieved by providing the URL to the JavaScript
within the `webstudio.javascript.url` property.
This JavaScript will be loaded and executed in the web browser along with the WebStudio UI.

### Google Analytics extension.

Create the `gtag.js` JavaScript file with the following content:
```javascript
/<![CDATA[

<!-- Google tag (gtag.js) -->
var tag = 'G-ABCDEF1234';

// Create a <script> element
var script = document.createElement('script');

// Set the source URL and async attribute
script.src = 'https://www.googletagmanager.com/gtag/js?id=' + tag;
script.async = true;

// Append the <script> element to the document body
document.body.appendChild(script);

window.dataLayer = window.dataLayer || [];
function gtag(){dataLayer.push(arguments);}
gtag('js', new Date());
gtag('config', tag);

//]]>

```

Put the file in the location, where it can be accessed by a browser. For example, it can be located in the root of the web application.

Define the `webstudio.javascript.url` property to this location. E.g. `webstudio.javascript.url=https://example.com/gtag.js` property.
