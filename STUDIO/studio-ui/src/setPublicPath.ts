declare let __webpack_public_path__: string

// This file sets the public path for Webpack to load assets correctly.
const url = new URL(document.baseURI || '/', window.location.href)
__webpack_public_path__ = `${url.pathname}js/`
