# Extending OpenL Tablets WebStudio UI by External JavaScript

To extend the OpenL Tablets WebStudio UI functionality, in the `webstudio.javascript.url` property, provide the URL to the corresponding JavaScript.
This JavaScript will be loaded and executed in a web browser along with the OpenL Tablets WebStudio UI.

### Google Analytics Extension

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

Save the file in the location that can be accessed by a browser, for example, in the root of the web application.

Set the `webstudio.javascript.url` property to this location, for example, as follows:
`webstudio.javascript.url=https://example.com/gtag.js`
