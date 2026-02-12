// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom'
import { TextEncoder, TextDecoder } from 'util'

// react-router-dom uses TextEncoder (not defined in jsdom)
if (typeof globalThis.TextEncoder === 'undefined') {
    globalThis.TextEncoder = TextEncoder
    globalThis.TextDecoder = TextDecoder as typeof globalThis.TextDecoder
}

// MessageChannel is used by @rc-component/form (Ant Design Form) but is not available in jsdom
if (typeof globalThis.MessageChannel === 'undefined') {
    globalThis.MessageChannel = class MessageChannel {
        port1 = { onmessage: null, postMessage: () => {} }
        port2 = { onmessage: null, postMessage: () => {} }
    }
}

// jsdom doesn't implement ResizeObserver (used by @rc-component/resize-observer via Ant Design)
global.ResizeObserver = class ResizeObserver {
    observe = jest.fn()
    unobserve = jest.fn()
    disconnect = jest.fn()
}

// jsdom doesn't implement matchMedia (used by Ant Design responsiveObserver)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query: string) => ({
        addListener: jest.fn(),
        addEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
        matches: false,
        media: query,
        onchange: null,
        removeListener: jest.fn(),
        removeEventListener: jest.fn(),
    })),
})

// jsdom doesn't implement getComputedStyle with pseudoElt (used by Ant Design Modal for scrollbar)
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = function (
    elt: Element,
    pseudoElt?: string | null
): CSSStyleDeclaration {
    if (pseudoElt) {
        return {
            getPropertyValue: () => '',
            getPropertyPriority: () => '',
            width: '0px',
            height: '0px',
            length: 0,
            parentRule: null,
            cssText: '',
        } as unknown as CSSStyleDeclaration
    }
    return originalGetComputedStyle.call(window, elt)
}
