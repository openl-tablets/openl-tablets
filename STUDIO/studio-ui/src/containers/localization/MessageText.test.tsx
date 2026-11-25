import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { afterBreaks } from 'testing/pathBreaks'
import { MessageText } from './MessageText'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

describe('MessageText', () => {
    it('wraps a field path of an own text before a dot, a piece marked by a warning included', () => {
        // The piece marked starts with the dot that joins it to the name before it.
        const warning = { summary: 'Unknown field.', spots: [{ start: 9, end: 23 }] }
        const { container } = render(
            <MessageText
                cell={{ kind: 'own', text: 'Set quote.planName.code first.' }}
                nameOf={tag => tag}
                warnings={[warning]}
            />
        )

        expect(screen.getByTestId('localization-text-problem')).toHaveTextContent(/^\.planName\.code$/)
        expect(afterBreaks(container)).toEqual(['.planName', '.code'])
        expect(container).toHaveTextContent(/^Set quote\.planName\.code first\.$/)
    })

    it('wraps a field path of an own text with no warning, and of an inherited one', () => {
        const { container, rerender } = render(
            <MessageText cell={{ kind: 'own', text: 'quote.planName' }} nameOf={tag => tag} warnings={[]} />
        )
        expect(afterBreaks(container)).toEqual(['.planName'])

        rerender(
            <MessageText
                cell={{ kind: 'inherited', text: 'quote.planName', from: '' }}
                nameOf={tag => tag}
                warnings={[]}
            />
        )
        expect(afterBreaks(container)).toEqual(['.planName'])
    })
})
