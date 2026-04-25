// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom/vitest'
import failOnConsole from 'vitest-fail-on-console'
import { TextEncoder, TextDecoder } from 'util'
import { vi } from 'vitest'


failOnConsole({
    // shouldFailOnWarn stays on so jest-fail-on-console wraps `console.warn`; the
    // `silenceMessage` branch below drops every warn (no failure, no console noise).
    // With shouldFailOnWarn: false the wrapper is skipped entirely and silenceMessage
    // is never invoked for warns, so they would leak through to the real console.
    silenceMessage: (message, methodName) => {
        // ignore warnings
        if (methodName === 'warn') return true
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
        onmessage: ((event: any) => void) | null = null
        otherPort?: FakeMessagePort
        postMessage(data: unknown) {
            const target = this.otherPort
            setTimeout(() => target?.onmessage?.({ data }), 0)
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
    observe = vi.fn()
    unobserve = vi.fn()
    disconnect = vi.fn()
}

// jsdom doesn't implement matchMedia (used by Ant Design responsiveObserver)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        addListener: vi.fn(),
        addEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
        matches: false,
        media: query,
        onchange: null,
        removeListener: vi.fn(),
        removeEventListener: vi.fn(),
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
