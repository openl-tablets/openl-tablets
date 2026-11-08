import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { MessageWarning } from './messageWarnings'
import { ProblemList } from './ProblemList'

const Problems = ({ problems }: { problems: MessageWarning[] }) => <ProblemList problems={problems} />

describe('ProblemList', () => {
    it('tells a single warning as a sentence', () => {
        render(<Problems problems={[{ summary: 'The quote is not closed.', spots: []}]} />)

        expect(screen.getByTestId('localization-cell-problems').tagName).toBe('SPAN')
        expect(screen.getByTestId('localization-cell-problems')).toHaveTextContent('The quote is not closed.')
    })

    it('lists several warnings, the same words twice included', () => {
        const twice = { summary: 'A full-width comma.', spots: []}
        render(<Problems problems={[twice, twice, { summary: 'Placeholders differ.', spots: []}]} />)

        expect([...screen.getByTestId('localization-cell-problems').children].map(item => item.textContent))
            .toEqual(['A full-width comma.', 'A full-width comma.', 'Placeholders differ.'])
    })
})
