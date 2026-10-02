# Extending OpenL Studio UI by External JavaScript

To extend the OpenL Studio UI functionality, provide the URL of a JavaScript file in the `webstudio.javascript.url` property.
The browser loads and runs this JavaScript along with the OpenL Studio UI.

### Google Analytics Extension

Create the `gtag.js` JavaScript file with the following content:

```javascript
// Google tag (gtag.js)
const tag = 'G-ABCDEF1234';

const script = document.createElement('script');
script.src = `https://www.googletagmanager.com/gtag/js?id=${tag}`;
script.async = true;
document.body.appendChild(script);

window.dataLayer = window.dataLayer || [];
function gtag() {
    window.dataLayer.push(arguments);
}
gtag('js', new Date());
gtag('config', tag);
```

Save the file in the location that can be accessed by a browser, for example, in the root of the web application.

Set the `webstudio.javascript.url` property to this location, for example, as follows:
`webstudio.javascript.url=https://example.com/gtag.js`
