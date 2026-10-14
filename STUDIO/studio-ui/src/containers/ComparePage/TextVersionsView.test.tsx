import { render, screen } from '@testing-library/react'
import { TextVersionsView } from './TextVersionsView'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => ({
    Alert: ({ title, showIcon: _showIcon, type: _type, ...rest }: any) => (
        <div role="alert" {...rest}>{title}</div>
    ),
    Empty: Object.assign(({ description }: any) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 'simple' }),
    Spin: ({ description }: any) => <div role="status">{description}</div>,
}))

describe('TextVersionsView', () => {
    it('reads both versions of the file once and shows what the second one changes', async () => {
        const read = vi.fn(async (): Promise<[string, string]> => ['one\ntwo', 'one\nthree'])

        render(<TextVersionsView read={read} />)

        const diff = (await screen.findByTestId('compare-text-diff')).textContent
        expect(diff).toContain('- two')
        expect(diff).toContain('+ three')
        expect(read).toHaveBeenCalledTimes(1)
        // Versions with no names are not told apart above the lines.
        expect(screen.queryByTestId('compare-text-legend')).toBeNull()
    })

    it('tells which version a removed and an added line comes from', async () => {
        const read = vi.fn(async (): Promise<[string, string]> => ['one', 'two'])

        render(<TextVersionsView read={read} titles={{ first: 'Base version', second: 'Their version' }} />)

        await screen.findByTestId('compare-text-diff')
        expect(screen.getByTestId('compare-text-legend')).toHaveTextContent('-Base version+Their version')
    })

    it('names the second version first when it is shown on the left', async () => {
        const read = vi.fn(async (): Promise<[string, string]> => ['one', 'two'])

        render(<TextVersionsView secondOnLeft read={read} titles={{ first: 'Base version', second: 'Your version' }} />)

        await screen.findByTestId('compare-text-diff')
        expect(screen.getByTestId('compare-text-legend')).toHaveTextContent('+Your version-Base version')
    })

    it('does not compare binary content line by line', async () => {
        // A NUL character among the first characters tells binary content, such as a library.
        const read = vi.fn(async (): Promise<[string, string]> => ['PK\u0003\u0004\u0000\u0000', 'text'])

        render(<TextVersionsView read={read} titles={{ first: 'Base version', second: 'Their version' }} />)

        expect(await screen.findByTestId('compare-not-text')).toHaveTextContent('not_text')
        expect(screen.queryByTestId('compare-text-diff')).toBeNull()
        // Nor does it tell which version the lines come from, as no line is shown.
        expect(screen.queryByTestId('compare-text-legend')).toBeNull()
    })

    it('says why the versions could not be read', async () => {
        const read = vi.fn(async (): Promise<[string, string]> => {
            throw new Error('The merge is over')
        })

        render(<TextVersionsView read={read} />)

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('The merge is over')
    })
})
