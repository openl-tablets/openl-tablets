import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LocalChangesSummary } from './LocalChangesSummary'

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, options?: { count?: number }) => options?.count === undefined ? key : `${key}:${options.count}`,
    }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => {
    const Icon = () => null
    return {
        DeleteOutlined: Icon,
        DownOutlined: Icon,
        EditOutlined: Icon,
        PlusOutlined: Icon,
        RightOutlined: Icon,
    }
})

vi.mock('antd', () => {
    const Button = ({ block: _block, children, icon, type: _type, ...props }: Record<string, unknown>) => (
        <button type="button" {...props}>
            {icon as never}
            {children as never}
        </button>
    )
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    return { Button, Tooltip }
})

describe('LocalChangesSummary', () => {
    it('does not render without pending changes', () => {
        render(<LocalChangesSummary changes={[]} />)

        expect(screen.queryByTestId('local-changes')).toBeNull()
    })

    it('shows deleted files after expanding local changes', async () => {
        render(
            <LocalChangesSummary
                changes={[
                    { path: 'rules/Removed.xlsx', type: 'deleted' },
                    { path: 'rules/Edited.xlsx', type: 'modified' },
                ]}
            />
        )

        expect(screen.getByText('browser.files.local_changes:2')).toBeTruthy()
        expect(screen.queryByText('rules/Removed.xlsx')).toBeNull()

        await userEvent.click(screen.getByTestId('local-changes-toggle'))

        expect(screen.getByTestId('local-change-deleted-rules/Removed.xlsx')).toBeTruthy()
        expect(screen.getByText('rules/Removed.xlsx')).toBeTruthy()
        expect(screen.getByText('rules/Edited.xlsx')).toBeTruthy()
    })
})
