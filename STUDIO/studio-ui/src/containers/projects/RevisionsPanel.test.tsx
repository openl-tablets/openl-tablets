import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RevisionsPanel } from './RevisionsPanel'
import { getProjectRevisions, openProjectRevision } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectRevisions: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => error instanceof Error && error.message === 'modified'),
    openProjectRevision: vi.fn(),
    REVISIONS_PAGE_SIZE: 20,
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('../../components/SearchInput', () => ({
    SearchInput: ({ onChange, value, ...rest }: Record<string, unknown>) =>
        <input onChange={onChange as never} value={value as never} {...rest} />,
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
}))

vi.mock('antd', () => {
    const domProps = (props: unknown): Record<string, unknown> => {
        if (!props || typeof props !== 'object') {
            return {}
        }
        const { danger, ...dom } = props as Record<string, unknown>
        void danger
        return dom
    }
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { loading, size, title, type, ...dom } = rest
        void loading; void size; void title; void type
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Tag = ({ children, color, ...rest }: Record<string, unknown>) => {
        void color
        return <span {...rest}>{children as never}</span>
    }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Modal = ({
        cancelButtonProps,
        children,
        okButtonProps,
        okText,
        onCancel,
        onOk,
        open,
        title,
    }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <h4>{title as never}</h4>
            <div>{children as never}</div>
            <button {...domProps(cancelButtonProps)} onClick={onCancel as never}>cancel</button>
            <button {...domProps(okButtonProps)} onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    const Skeleton = () => <div>loading</div>
    const Alert = ({ title }: Record<string, unknown>) => <div>{title as never}</div>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Switch = ({ checked, onChange, ...rest }: Record<string, unknown>) => {
        const { size, ...dom } = rest
        void size
        return <input checked={checked as boolean} onChange={e => (onChange as (v: boolean) => void)(e.target.checked)} role="switch" type="checkbox" {...dom} />
    }
    const notification = { error: vi.fn(), success: vi.fn() }
    return { Alert, Button, Skeleton, Empty, Modal, Switch, Tag, Tooltip, notification }
})

const REVS = [
    { revisionNo: 'abcdef1234', shortRevisionNo: 'abcdef1', createdAt: '2024-01-02T00:00:00Z', fullComment: 'Second', author: { displayName: 'jane' }, deleted: false, technicalRevision: false },
    { revisionNo: '0987654321', shortRevisionNo: '0987654', createdAt: '2024-01-01T00:00:00Z', fullComment: 'First', author: { displayName: 'john' }, deleted: false, technicalRevision: false },
]

const page = (content: typeof REVS, total = content.length, pageSize = 20) => ({
    content,
    pageNumber: 0,
    pageSize,
    numberOfElements: content.length,
    total,
})

function deferred<T>() {
    let resolve!: (value: T) => void
    let reject!: (reason?: unknown) => void
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise
        reject = rejectPromise
    })
    return { promise, resolve, reject }
}

async function renderPanel(props: {
    currentRevision?: string | null
    onOpened?: () => void
    searchable?: boolean
} = {}) {
    await act(async () => {
        render(
            <RevisionsPanel
                branch="main"
                currentRevision={props.currentRevision === undefined ? REVS[0]!.revisionNo : props.currentRevision}
                onOpened={props.onOpened ?? vi.fn()}
                projectId="p1"
                projectName="Proj"
                repositoryId="repo1"
                searchable={props.searchable}
            />
        )
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('RevisionsPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectRevisions).mockImplementation((_repo, _name, _branch, query) => {
            const search = query?.search
            const content = search ? REVS.filter(revision => revision.fullComment.includes(search)) : REVS
            return Promise.resolve(page(content))
        })
        vi.mocked(openProjectRevision).mockResolvedValue()
    })

    it('lists revisions from the history endpoint', async () => {
        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith('repo1', 'Proj', 'main', expect.anything()))
        expect(screen.getByTestId('revisions-p1')).toBeTruthy()
        expect(screen.getByText('Second')).toBeTruthy()
    })

    it('opens a chosen revision for viewing', async () => {
        const onOpened = vi.fn()
        await renderPanel({ onOpened })
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        // The project revision is current and has no Open button; only older revisions can be opened.
        expect(screen.queryByTestId('revision-open-abcdef1234')).toBeNull()
        await userEvent.click(screen.getByTestId('revision-open-0987654321'))

        await waitFor(() => expect(openProjectRevision).toHaveBeenCalledWith('p1', '0987654321', {}))
        await waitFor(() => expect(onOpened).toHaveBeenCalled())
    })

    it('confirms discarding changes before forcing a revision open', async () => {
        const onOpened = vi.fn()
        vi.mocked(openProjectRevision)
            .mockRejectedValueOnce(new Error('modified'))
            .mockResolvedValueOnce(undefined)
        await renderPanel({ onOpened })
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('revision-open-0987654321'))

        await waitFor(() => expect(screen.getByText('browser.close_discard_title')).toBeTruthy())
        expect(screen.getByText('browser.open_revision_discard_warning')).toBeTruthy()
        expect(screen.getByText('browser.open_revision_discard_confirm_unsafe')).toBeTruthy()
        expect(openProjectRevision).toHaveBeenCalledWith('p1', '0987654321', {})

        await userEvent.click(screen.getByTestId('revision-discard-open-confirm'))

        await waitFor(() => expect(openProjectRevision).toHaveBeenLastCalledWith(
            'p1',
            '0987654321',
            { discardChanges: true }
        ))
        await waitFor(() => expect(onOpened).toHaveBeenCalled())
    })

    it('does not offer Open for technical revisions', async () => {
        vi.mocked(getProjectRevisions).mockResolvedValue(page([
            REVS[0]!,
            { ...REVS[1]!, technicalRevision: true },
        ]))

        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        expect(screen.getByText('browser.history.technical')).toBeTruthy()
        expect(screen.queryByTestId('revision-open-0987654321')).toBeNull()
        expect(openProjectRevision).not.toHaveBeenCalled()
    })

    it('offers no open action on the current revision even when a technical revision is newest', async () => {
        vi.mocked(getProjectRevisions).mockResolvedValue(page([
            {
                revisionNo: 'technical123',
                shortRevisionNo: 'technic',
                createdAt: '2024-01-03T00:00:00Z',
                fullComment: 'Technical update',
                author: { displayName: 'system' },
                deleted: false,
                technicalRevision: true,
            },
            REVS[0]!,
            REVS[1]!,
        ]))

        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        expect(screen.queryByTestId('revision-open-abcdef1234')).toBeNull()
        expect(screen.getByTestId('revision-open-0987654321')).toBeTruthy()
    })

    it('collapses long commit messages', async () => {
        const comment = [
            'First line',
            'Second line',
            'Third line',
            'Fourth line with details that should stay hidden until expanded',
        ].join('\n')
        vi.mocked(getProjectRevisions).mockResolvedValue(page([
            { ...REVS[0]!, revisionNo: 'long', fullComment: comment },
            REVS[1]!,
        ]))

        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('revision-comment-long')).toBeTruthy())

        // Only the first line — the subject — shows until the message is expanded.
        expect(screen.getByTestId('revision-comment-long').textContent).toContain('browser.commit.show_more')
        expect(screen.getByTestId('revision-comment-long').textContent).not.toContain('Second line')
        expect(screen.getByTestId('revision-comment-long').textContent).not.toContain('Fourth line')

        await userEvent.click(screen.getByTestId('revision-comment-long-toggle'))

        expect(screen.getByTestId('revision-comment-long').textContent).toContain('Fourth line')
        expect(screen.getByTestId('revision-comment-long').textContent).toContain('browser.commit.show_less')
    })

    it('searches the history server-side', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByText('Second')).toBeTruthy())

        await userEvent.type(screen.getByTestId('revisions-search-p1'), 'First')

        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith(
            'repo1',
            'Proj',
            'main',
            expect.objectContaining({ search: 'First' })
        ))
        await waitFor(() => expect(screen.queryByText('Second')).toBeNull())
        expect(screen.getByText('First')).toBeTruthy()
    })

    it('keeps the newest matching revision openable under a search', async () => {
        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())
        // 'First' matches only the older revision, excluding the HEAD ("Second"). The lone match then
        // sits at index 0 but is not the HEAD, so it must stay openable rather than be treated as current.
        await userEvent.type(screen.getByTestId('revisions-search-p1'), 'First')
        await waitFor(() => expect(screen.queryByText('Second')).toBeNull())
        await waitFor(() => expect(screen.getByTestId('revision-open-0987654321')).toBeTruthy())
    })

    it('requests technical revisions when the toggle is on', async () => {
        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('revisions-tech'))

        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith('repo1', 'Proj', 'main', expect.objectContaining({ techRevs: true })))
    })

    it('hides search, technical revisions and paging for non-searchable repositories', async () => {
        vi.mocked(getProjectRevisions).mockResolvedValue(page(REVS, 4, 2))

        await renderPanel({ searchable: false })
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith(
            'repo1',
            'Proj',
            'main',
            expect.objectContaining({ techRevs: false })
        ))
        expect(vi.mocked(getProjectRevisions).mock.calls[0]?.[3]).not.toHaveProperty('search')

        expect(screen.queryByTestId('revisions-search-p1')).toBeNull()
        expect(screen.queryByTestId('revisions-tech')).toBeNull()
        expect(screen.queryByTestId('revisions-load-more')).toBeNull()
    })

    it('does not offer a restore action', async () => {
        await renderPanel()
        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())

        expect(screen.queryByTestId('revision-restore-0987654321')).toBeNull()
    })

    it('does not append a stale page after the search changes', async () => {
        const stalePage = deferred<ReturnType<typeof page>>()
        vi.mocked(getProjectRevisions).mockImplementation((_repo, _name, _branch, query) => {
            if (query?.page === 1) {
                return stalePage.promise
            }
            if (query?.search === 'First') {
                return Promise.resolve(page([REVS[1]!]))
            }
            return Promise.resolve(page([REVS[0]!], 2, 1))
        })
        await renderPanel()
        await waitFor(() => expect(screen.getByText('Second')).toBeTruthy())

        await userEvent.click(screen.getByTestId('revisions-load-more'))
        await userEvent.type(screen.getByTestId('revisions-search-p1'), 'First')

        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith(
            'repo1',
            'Proj',
            'main',
            expect.objectContaining({ search: 'First' })
        ))
        await waitFor(() => expect(screen.queryByText('Second')).toBeNull())

        await act(async () => {
            stalePage.resolve(page([{
                revisionNo: 'stale',
                shortRevisionNo: 'stale',
                createdAt: '2024-01-03T00:00:00Z',
                fullComment: 'Stale page',
                author: { displayName: 'old' },
                deleted: false,
                technicalRevision: false,
            }]))
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.getByText('First')).toBeTruthy()
        expect(screen.queryByText('Stale page')).toBeNull()
    })
})
