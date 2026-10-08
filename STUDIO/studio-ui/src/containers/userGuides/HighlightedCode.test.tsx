import { render, waitFor } from '@testing-library/react'
import { errorHandler } from 'utils/errorHandling'
import { HighlightedCode } from './HighlightedCode'

vi.mock('./codeHighlight', () => {
    throw new Error('The chunk did not load')
})

vi.mock('utils/errorHandling', () => ({ errorHandler: { logError: vi.fn() } }))

describe('HighlightedCode', () => {
    it('keeps the code as plain text when the grammars cannot be loaded', async () => {
        const { container } = render(<HighlightedCode code={'public class Rules {}\n'} language="java" />)

        await waitFor(() => expect(errorHandler.logError).toHaveBeenCalled())
        const code = container.querySelector('pre code.language-java')
        expect(code?.textContent).toBe('public class Rules {}\n')
        expect(code?.querySelector('span')).toBeNull()
    })
})
