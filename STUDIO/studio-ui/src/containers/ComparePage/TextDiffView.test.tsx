import React from 'react'
import { render, screen } from '@testing-library/react'
import { TextDiffView } from './TextDiffView'
import { MAX_DIFF_LINES } from 'utils/lineDiff'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => ({
    Empty: Object.assign(({ description }: any) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 'simple' }),
}))

describe('TextDiffView', () => {
    it('marks what the second version adds and what the first version had', () => {
        render(<TextDiffView first={'one\ntwo'} second={'one\nthree'} />)

        const lines = screen.getByTestId('compare-text-diff').textContent
        expect(lines).toContain('- two')
        expect(lines).toContain('+ three')
        // A line that reads the same in both is kept, so a change is read in its own place.
        expect(lines).toContain('  one')
    })

    it('says that the two versions read the same', () => {
        render(<TextDiffView first={'one\ntwo'} second={'one\ntwo'} />)

        expect(screen.queryByTestId('compare-text-diff')).toBeNull()
        expect(screen.getByText('identical')).toBeInTheDocument()
    })

    it('says when the versions are too large to compare here', () => {
        const long = Array.from({ length: MAX_DIFF_LINES }, (_, line) => `line ${line}`).join('\n')

        render(<TextDiffView first={long} second={`${long}\nmore`} />)

        expect(screen.getByText('too_large')).toBeInTheDocument()
    })
})
