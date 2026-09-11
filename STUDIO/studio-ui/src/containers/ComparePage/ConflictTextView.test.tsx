import React from 'react'
import { act, render, screen } from '@testing-library/react'
import { ConflictTextView } from './ConflictTextView'
import { getConflictFileText } from 'services/compare'

vi.mock('services/compare', () => ({ getConflictFileText: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => ({
    Alert: ({ message, showIcon: _showIcon, type: _type, ...rest }: any) => (
        <div role="alert" {...rest}>{message}</div>
    ),
    Empty: Object.assign(({ description }: any) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 'simple' }),
    Spin: ({ description }: any) => <div role="status">{description}</div>,
}))

describe('ConflictTextView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    const renderView = async () => {
        await act(async () => {
            render(<ConflictTextView path="rules/notes.txt" projectId="p1" />)
        })
    }

    it('reads both versions of the file and shows what differs', async () => {
        vi.mocked(getConflictFileText).mockImplementation(async (_project, _path, side) =>
            (side === 'THEIRS' ? 'one\ntwo' : 'one\nthree'))

        await renderView()

        expect(getConflictFileText).toHaveBeenCalledWith('p1', 'rules/notes.txt', 'THEIRS')
        expect(getConflictFileText).toHaveBeenCalledWith('p1', 'rules/notes.txt', 'OURS')
        // The version being merged in is read first, so the diff reads as what the merge would bring.
        const diff = screen.getByTestId('compare-text-diff').textContent
        expect(diff).toContain('- two')
        expect(diff).toContain('+ three')
    })

    it('says why the versions could not be read', async () => {
        vi.mocked(getConflictFileText).mockRejectedValue(new Error('The merge is over'))

        await renderView()

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('The merge is over')
    })
})
