// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom'
import failOnConsole from 'jest-fail-on-console'
import { TextEncoder, TextDecoder } from 'util'


failOnConsole({
    shouldFailOnWarn: false,
    skipTest: ({ testPath }) =>
        // EPBDS-15832 keeps this suite's fixes (act-wrapping, setFieldsValue move to
        // useEffect, password mock, notification title rename, per-test spies)
        // on a separate branch. Skip jest-fail-on-console here so this branch can
        // land without waiting on the rebuild.
        /EditUserGroupDetailsWithAccessRights\.test\.tsx$/.test(testPath),
    silenceMessage: (message) => {
        // Matches Ant Design deprecation warnings, capturing any component, deprecated prop, and its suggested replacement.
        if (/^Warning:\s*\[antd:\s*[^\]]+]\s*`[^`]+`\s*is deprecated\.\s*Please use\s*`[^`]+`\s*instead\.?$/.test(message)) return true
        // jsdom emits "Error: Not implemented: navigation (except hash changes)" via
        // console.error whenever `window.location.reload()` / href assignment is
        // reached (e.g. Security.tsx save flow). Location is non-configurable in
        // jsdom, so the call cannot be stubbed at the API layer.
        if (/Not implemented: navigation/.test(message)) return true
        return false
    },
})

// react-router-dom uses TextEncoder (not defined in jsdom)
if (typeof globalThis.TextEncoder === 'undefined') {
    globalThis.TextEncoder = TextEncoder
    globalThis.TextDecoder = TextDecoder as typeof globalThis.TextDecoder
}

// MessageChannel is used by React's scheduler, Ant Design Form (@rc-component/form),
// and others, but jsdom does not implement it. A no-op polyfill silently drops
// messages, which makes React's concurrent scheduler never fire — scheduled state
// updates from async callbacks are queued but never committed, producing
// "The current testing environment is not configured to support act(...)" and
// waitFor timeouts. Deliver messages on the next macrotask instead.
if (typeof globalThis.MessageChannel === 'undefined') {
    class FakeMessagePort {
        onmessage: ((event: { data: unknown }) => void) | null = null
        otherPort!: FakeMessagePort
        postMessage(data: unknown) {
            const target = this.otherPort
            setTimeout(() => target.onmessage?.({ data }), 0)
        }
    }
    globalThis.MessageChannel = class {
        port1: FakeMessagePort
        port2: FakeMessagePort
        constructor() {
            this.port1 = new FakeMessagePort()
            this.port2 = new FakeMessagePort()
            this.port1.otherPort = this.port2
            this.port2.otherPort = this.port1
        }
    } as typeof MessageChannel
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
