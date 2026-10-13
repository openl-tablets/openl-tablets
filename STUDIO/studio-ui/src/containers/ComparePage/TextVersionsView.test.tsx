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
    })

    it('says why the versions could not be read', async () => {
        const read = vi.fn(async (): Promise<[string, string]> => {
            throw new Error('The merge is over')
        })

        render(<TextVersionsView read={read} />)

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('The merge is over')
    })
})
