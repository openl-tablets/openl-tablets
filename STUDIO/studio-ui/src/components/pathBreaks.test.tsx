import React from 'react'
import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { withPathBreaks } from './pathBreaks'

/** The text drawn, with a `|` where a line may break. */
const drawn = (node: React.ReactNode): string => {
    const { container } = render(<div>{node}</div>)
    const element = container.firstElementChild as HTMLElement
    return Array.from(element.childNodes)
        .map(child => (child.nodeName === 'WBR' ? '|' : child.textContent))
        .join('')
}

describe('withPathBreaks', () => {
    it('breaks a field path before each dot between its parts, never before an index', () => {
        expect(drawn(withPathBreaks('_res_.quoteCalculations[0].coverageDetail.requiredParticipationPercent')))
            .toBe('_res_|.quoteCalculations[0]|.coverageDetail|.requiredParticipationPercent')
    })

    it('breaks after a call and after a name ending in a digit', () => {
        expect(drawn(withPathBreaks('policy.getDrivers().length'))).toBe('policy|.getDrivers()|.length')
        expect(drawn(withPathBreaks('address2.city'))).toBe('address2|.city')
    })

    it('leaves a number, a sentence and an ellipsis whole', () => {
        expect(withPathBreaks('1105.25')).toBe('1105.25')
        expect(withPathBreaks('Version 7.1.0 is out. Read on...')).toBe('Version 7.1.0 is out. Read on...')
        expect(withPathBreaks('.hidden')).toBe('.hidden')
    })

    // A prose text of the messages or a value of a run would otherwise break as `e` and `.g.` at the end of a line.
    it('leaves an abbreviation whole, and a name of one letter with the dot before it', () => {
        expect(withPathBreaks('Use e.g. quote, i.e. the policy of the U.S.A.'))
            .toBe('Use e.g. quote, i.e. the policy of the U.S.A.')
        expect(drawn(withPathBreaks('point.x.value'))).toBe('point.x|.value')
    })

    it('breaks a path of names in other alphabets, a name ending in a vowel sign included', () => {
        expect(drawn(withPathBreaks('полис.водитель'))).toBe('полис|.водитель')
        expect(drawn(withPathBreaks('पॉलिसी.ड्राइवर'))).toBe('पॉलिसी|.ड्राइवर')
        expect(drawn(withPathBreaks('café.menu'))).toBe('café|.menu')
    })

    it('breaks a path of Java names holding a dollar sign', () => {
        expect(drawn(withPathBreaks('Outer$.$inner'))).toBe('Outer$|.$inner')
    })

    it('adds no character to the text', () => {
        const { container } = render(<span>{withPathBreaks('quote.coverageDetail.regionCode')}</span>)

        expect(container.textContent).toBe('quote.coverageDetail.regionCode')
        expect(container.querySelectorAll('wbr')).toHaveLength(2)
    })

    it('breaks a piece where the whole text would', () => {
        const text = 'quote.coverageDetail'

        // The dot that starts the piece joins it to the name before it, which another piece holds.
        expect(drawn(withPathBreaks(text, 5))).toBe('|.coverageDetail')
        expect(withPathBreaks(text, 0, 5)).toBe('quote')
        expect(withPathBreaks(text, 6, 14)).toBe('coverage')
    })

    it('draws an empty text as it is', () => {
        expect(withPathBreaks('')).toBe('')
    })
})
