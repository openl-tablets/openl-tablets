// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom/vitest'
import failOnConsole from 'vitest-fail-on-console'
import { vi } from 'vitest'

failOnConsole()

// jsdom doesn't implement ResizeObserver (used by @rc-component/resize-observer via Ant Design)
globalThis.ResizeObserver = class {
    observe = vi.fn()
    unobserve = vi.fn()
    disconnect = vi.fn()
}

// jsdom doesn't implement matchMedia (used by Ant Design responsiveObserver)
window.matchMedia = vi.fn((query: string) => ({
    addListener: vi.fn(),
    addEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
    matches: false,
    media: query,
    onchange: null,
    removeListener: vi.fn(),
    removeEventListener: vi.fn(),
})) as typeof window.matchMedia

// jsdom doesn't implement getComputedStyle with pseudoElt (used by Ant Design Modal for scrollbar)
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = (elt: Element, pseudoElt?: string | null) =>
    pseudoElt
        ? ({ getPropertyValue: () => '', getPropertyPriority: () => '', cssText: '', length: 0, parentRule: null } as unknown as CSSStyleDeclaration)
        : originalGetComputedStyle.call(window, elt)
