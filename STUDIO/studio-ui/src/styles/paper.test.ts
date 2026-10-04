import { describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { paperToken } from './paper'

describe('paperToken', () => {
    it('is the token of Ant Design\'s own light appearance: white paper and black ink', () => {
        const light = antdTheme.getDesignToken()

        expect(paperToken()).toMatchObject({
            colorBgContainer: '#ffffff',
            colorText: light.colorText,
            colorPrimary: light.colorPrimary,
            colorWarningBg: light.colorWarningBg,
        })
    })

    it('is worked out once and handed out the same after that', () => {
        expect(paperToken()).toBe(paperToken())
    })
})
