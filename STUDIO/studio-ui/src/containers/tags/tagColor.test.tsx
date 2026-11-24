import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { tagColorsOf, TagTypeMark } from './tagColor'
import { TagTypeOrder } from './tagOrder'

describe('tagColorsOf', () => {
    const types = ['Domain', 'LOB', 'Region', 'Product', 'Country', 'State', 'Team', 'BLOB']

    it('gives the configured tag types different colors in their order, repeating past the seventh', () => {
        const colors = types.map(type => tagColorsOf(types)(type))

        expect(new Set(colors.slice(0, 7)).size).toBe(7)
        expect(colors[7]).toBe(colors[0])
    })

    it('uses no warning or error hue', () => {
        for (const type of types) {
            expect(['red', 'volcano', 'orange', 'gold']).not.toContain(tagColorsOf(types)(type))
        }
    })

    it('matches a tag type regardless of case', () => {
        expect(tagColorsOf(types)('lob')).toBe(tagColorsOf(types)('LOB'))
    })

    it('makes a tag type that is not configured gray', () => {
        expect(tagColorsOf(types)('Home-made')).toBe('default')
        expect(tagColorsOf([])('Domain')).toBe('default')
    })
})

describe('TagTypeMark', () => {
    it('draws the tag icon in the color of the type', () => {
        render(
            <TagTypeOrder value={['Domain']}>
                <TagTypeMark testId="mark" type="Domain" />
            </TagTypeOrder>
        )

        expect(screen.getByTestId('mark').style.color).not.toBe('')
    })

    it('draws the tag icon of a type that is not configured in gray', () => {
        render(
            <TagTypeOrder value={['Domain']}>
                <TagTypeMark testId="configured" type="Domain" />
                <TagTypeMark testId="undefined" type="Region" />
            </TagTypeOrder>
        )

        expect(screen.getByTestId('undefined').style.color).not.toBe('')
        expect(screen.getByTestId('undefined').style.color).not.toBe(screen.getByTestId('configured').style.color)
    })
})
