import { isSafeRedirectUrl } from 'utils/loginRedirect'

const origin = 'http://localhost:8080'

describe('isSafeRedirectUrl', () => {
    it('rejects fetchtrace-sw.js (extension service worker)', () => {
        expect(isSafeRedirectUrl('http://localhost:8080/fetchtrace-sw.js?continue', origin)).toBe(false)
        expect(isSafeRedirectUrl('http://localhost:8080/fetchtrace-sw.js', origin)).toBe(false)
    })

    it('rejects .js files', () => {
        expect(isSafeRedirectUrl('http://localhost:8080/main.js', origin)).toBe(false)
        expect(isSafeRedirectUrl('http://localhost:8080/static/bundle.js', origin)).toBe(false)
    })

    it('rejects .css files', () => {
        expect(isSafeRedirectUrl('http://localhost:8080/styles.css', origin)).toBe(false)
    })

    it('rejects paths containing -sw. (service workers)', () => {
        expect(isSafeRedirectUrl('http://localhost:8080/custom-sw.js', origin)).toBe(false)
        expect(isSafeRedirectUrl('http://localhost:8080/my-sw.v2.js', origin)).toBe(false)
        expect(isSafeRedirectUrl('http://localhost:8080/worker-sw.manifest', origin)).toBe(false)
    })

    it('accepts valid app routes', () => {
        expect(isSafeRedirectUrl('http://localhost:8080/', origin)).toBe(true)
        expect(isSafeRedirectUrl('http://localhost:8080/webstudio/', origin)).toBe(true)
        expect(isSafeRedirectUrl('http://localhost:8080/administration', origin)).toBe(true)
        expect(isSafeRedirectUrl('http://localhost:8080/administration/users', origin)).toBe(true)
        expect(isSafeRedirectUrl('http://localhost:8080/faces/pages/repository', origin)).toBe(true)
    })

    it('accepts relative paths resolved against baseOrigin', () => {
        expect(isSafeRedirectUrl('/', origin)).toBe(true)
        expect(isSafeRedirectUrl('/webstudio/', origin)).toBe(true)
        expect(isSafeRedirectUrl('/administration', origin)).toBe(true)
    })

    it('rejects relative paths that resolve to unsafe targets', () => {
        expect(isSafeRedirectUrl('/fetchtrace-sw.js', origin)).toBe(false)
        expect(isSafeRedirectUrl('/main.js', origin)).toBe(false)
    })

    it('rejects cross-origin URLs', () => {
        expect(isSafeRedirectUrl('https://evil.com/', origin)).toBe(false)
        expect(isSafeRedirectUrl('http://other-host:8080/', origin)).toBe(false)
    })

    it('rejects malformed URLs', () => {
        expect(isSafeRedirectUrl('http://[invalid', origin)).toBe(false)
    })

    it('rejects protocol-relative URLs (open-redirect vector)', () => {
        expect(isSafeRedirectUrl('//evil.com/', origin)).toBe(false)
        expect(isSafeRedirectUrl('//evil.com/path', origin)).toBe(false)
    })

    it('rejects javascript: URLs (open-redirect vector)', () => {
        expect(isSafeRedirectUrl('javascript:alert(1)', origin)).toBe(false)
        expect(isSafeRedirectUrl('javascript:void(0)', origin)).toBe(false)
    })
})
