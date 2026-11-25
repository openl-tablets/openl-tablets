import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
    BinaryOutlined,
    CodeTagOutlined,
    Css3Outlined,
    GroovyOutlined,
    JsonOutlined,
    MavenOutlined,
    TranslateOutlined,
    TypeScriptOutlined,
} from './formatGlyphs'

describe('formatGlyphs', () => {
    it('draws the braces of JSON and the two gears of a binary file in the colour given', () => {
        const { container } = render(<><JsonOutlined style={{ color: 'gold' }} /><BinaryOutlined /></>)
        const [json, binary] = container.querySelectorAll('.anticon')

        expect(json).toHaveStyle({ color: 'rgb(255, 215, 0)' })
        expect(json?.querySelectorAll('path')).toHaveLength(3)
        expect(json?.querySelector('circle')).toBeInTheDocument()
        // The same gear twice, the second one smaller.
        const gears = [...binary!.querySelectorAll('path')]
        expect(gears).toHaveLength(2)
        expect(gears[0]?.getAttribute('d')).toBe(gears[1]?.getAttribute('d'))
        expect(gears.map(gear => gear.getAttribute('transform'))).toEqual(['translate(42 42) scale(0.66)', 'translate(504 504) scale(0.5)'])
    })

    it('draws the character and the letter of translation', () => {
        const { container } = render(<TranslateOutlined />)

        expect(container.querySelectorAll('.anticon path')).toHaveLength(2)
    })

    it('draws the m of Maven in one line', () => {
        const { container } = render(<MavenOutlined />)

        expect(container.querySelectorAll('.anticon path')).toHaveLength(1)
    })
    it('draws the brackets of code in one line and the star of Groovy with its sweep', () => {
        const { container } = render(<><CodeTagOutlined /><GroovyOutlined /></>)
        const [code, groovy] = container.querySelectorAll('.anticon')

        expect(code?.querySelectorAll('path')).toHaveLength(1)
        expect(groovy?.querySelectorAll('path')).toHaveLength(2)
    })
    it('draws the letters of TypeScript and the shield of CSS', () => {
        const { container } = render(<><TypeScriptOutlined /><Css3Outlined /></>)
        const [typescript, css] = container.querySelectorAll('.anticon')

        expect(typescript?.querySelectorAll('path')).toHaveLength(2)
        expect(css?.querySelectorAll('path')).toHaveLength(2)
    })
})
