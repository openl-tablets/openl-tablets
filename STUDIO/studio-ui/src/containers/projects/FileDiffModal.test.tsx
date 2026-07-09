import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FileDiffModal } from './FileDiffModal'
import { getFileContent } from '../../services/files'
import { getProjectRevisions } from '../../services/repositories'

vi.mock('../../services/files', () => ({ getFileContent: vi.fn() }))
vi.mock('../../services/repositories', () => ({ getProjectRevisions: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }), cx: (...a: unknown[]) => a.filter(Boolean).join(' ') }),
}))

vi.mock('antd', () => {
    const Modal = ({ open, children, title }: Record<string, unknown>) =>
        open ? <div role="dialog"><span>{title as never}</span>{children as never}</div> : null
    const Select = ({ value, options, onChange, ...rest }: Record<string, unknown>) => {
        const { 'data-testid': testId } = rest as { 'data-testid'?: string }
        return (
            <select data-testid={testId} onChange={e => (onChange as (v: string) => void)(e.target.value)} value={value as string}>
                {(options as { value: string, label: string }[]).map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
        )
    }
    const Skeleton = () => <div>loading</div>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const Space = ({ children, className }: Record<string, unknown>) => <div className={className as string}>{children as never}</div>
    return { Alert, Empty, Modal, Select, Skeleton, Space }
})

const revisions = [
    { revisionNo: 'rev2', shortRevisionNo: 'rev2s', createdAt: '2024-01-02', fullComment: 'second', author: { displayName: 'jane' }, deleted: false, technicalRevision: false },
    { revisionNo: 'rev1', shortRevisionNo: 'rev1s', createdAt: '2024-01-01', fullComment: 'first', author: { displayName: 'john' }, deleted: false, technicalRevision: false },
]

const page = (content: typeof revisions) => ({ content, pageNumber: 0, pageSize: 100, numberOfElements: content.length, total: content.length })

async function renderModal() {
    await act(async () => {
        render(<FileDiffModal open branch="main" onClose={vi.fn()} path="rules.xml" projectId="p1" projectName="Proj" repositoryId="repo1" />)
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('FileDiffModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectRevisions).mockResolvedValue(page(revisions))
    })

    it('renders a line diff between the two newest revisions', async () => {
        vi.mocked(getFileContent)
            .mockResolvedValueOnce('a\nb\nc') // from = rev1 (older, preselected as revisions[1])
            .mockResolvedValueOnce('a\nB\nc') // to = rev2 (newest, revisions[0])
        await renderModal()

        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith('repo1', 'Proj', 'main', expect.anything()))
        await waitFor(() => expect(screen.getByTestId('file-diff-body')).toBeInTheDocument())
        // from=rev1, to=rev2 fetched
        expect(getFileContent).toHaveBeenCalledWith('p1', 'rules.xml', 'rev1')
        expect(getFileContent).toHaveBeenCalledWith('p1', 'rules.xml', 'rev2')
    })

    it('shows an "identical" note when the revisions match', async () => {
        vi.mocked(getFileContent).mockResolvedValue('same\ncontent')
        await renderModal()

        await waitFor(() => expect(screen.getByTestId('file-diff-identical')).toBeInTheDocument())
    })

    it('shows a no-history note when there are fewer than two revisions', async () => {
        vi.mocked(getProjectRevisions).mockResolvedValue(page([revisions[0]!]))
        await renderModal()

        await waitFor(() => expect(screen.getByTestId('file-diff-no-history')).toBeInTheDocument())
    })

    it('shows a too-large warning instead of rendering a large diff', async () => {
        const largeText = Array.from({ length: 4_001 }, (_, index) => `line-${index}`).join('\n')
        vi.mocked(getFileContent)
            .mockResolvedValueOnce(largeText)
            .mockResolvedValueOnce('')

        await renderModal()

        await waitFor(() => expect(screen.getByTestId('file-diff-too-large')).toBeInTheDocument())
        expect(screen.queryByTestId('file-diff-body')).toBeNull()
    })

    it('surfaces an error when revisions fail to load', async () => {
        vi.mocked(getProjectRevisions).mockRejectedValue(new Error('boom'))
        await renderModal()

        await waitFor(() => expect(screen.getByTestId('file-diff-error')).toBeInTheDocument())
    })
})
