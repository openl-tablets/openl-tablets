import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { TreeDataNode } from 'antd'
import { RepoFolderPicker, attachChildren } from './RepoFolderPicker'
import { listRepoFolders } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({ listRepoFolders: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('./folderPicker.styles', () => {
    // A stable styles object: a fresh one per render would change the load callbacks and loop the effect.
    const styles = new Proxy({}, { get: () => '' })
    const cx = (...args: unknown[]) => args.filter(Boolean).join(' ')
    return { useStyles: () => ({ styles, cx }) }
})

vi.mock('./sharedStyles', () => {
    const styles = new Proxy({}, { get: () => '' })
    return { useSharedStyles: () => ({ styles }) }
})

interface Node { key: string, title: string, children?: Node[] }

vi.mock('antd', () => {
    // A stable notification object: a fresh one per render would change the load callbacks and loop the effect.
    const notification = { error: vi.fn() }
    return {
        App: { useApp: () => ({ notification }) },
        Spin: () => <div data-testid="spin" />,
        Empty: Object.assign(({ description }: { description: string }) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 0 }),
        Modal: ({ open, children, onOk, onCancel, okButtonProps }: Record<string, unknown>) => (open as boolean) ? (
            <div data-testid="modal">
                {children as never}
                <button data-testid="modal-ok" disabled={(okButtonProps as { disabled?: boolean })?.disabled} onClick={onOk as never}>ok</button>
                <button data-testid="modal-cancel" onClick={onCancel as never}>cancel</button>
            </div>
        ) : null,
        Tree: ({ treeData, loadData, onSelect }: Record<string, unknown>) => {
            const render_ = (nodes: Node[]): React.ReactNode => nodes.map(node => (
                <div key={node.key}>
                    <button data-testid={`pick-${node.key}`} onClick={() => (onSelect as (k: unknown[]) => void)([node.key])}>
                        {node.title}
                    </button>
                    <button data-testid={`expand-${node.key}`} onClick={() => (loadData as (n: Node) => void)(node)}>expand</button>
                    {node.children && render_(node.children)}
                </div>
            ))
            return <div>{render_((treeData as Node[]) ?? [])}</div>
        },
    }
})

const folder = (name: string, path: string) => ({ name, path, type: 'folder' as const, basePath: '' })

describe('RepoFolderPicker', () => {
    beforeEach(() => vi.clearAllMocks())

    it('lists the repository root folders when the dialog opens', async () => {
        vi.mocked(listRepoFolders).mockResolvedValue([folder('rules', 'rules'), folder('team', 'team')])
        render(<RepoFolderPicker open onClose={vi.fn()} onSelect={vi.fn()} repositoryId="design" />)

        await waitFor(() => expect(listRepoFolders).toHaveBeenCalledWith('design'))
        expect(await screen.findByTestId('pick-rules')).toBeTruthy()
        expect(screen.getByTestId('pick-team')).toBeTruthy()
    })

    it('reports the selected folder path only after confirming, then closes', async () => {
        vi.mocked(listRepoFolders).mockResolvedValue([folder('rules', 'rules')])
        const onSelect = vi.fn()
        const onClose = vi.fn()
        render(<RepoFolderPicker open onClose={onClose} onSelect={onSelect} repositoryId="design" />)

        await userEvent.click(await screen.findByTestId('pick-rules'))
        // Selecting a node alone must not report a path — the user confirms with OK.
        expect(onSelect).not.toHaveBeenCalled()
        await userEvent.click(screen.getByTestId('modal-ok'))

        expect(onSelect).toHaveBeenCalledWith('rules')
        expect(onClose).toHaveBeenCalled()
    })

    it('loads a folder\'s children lazily on expand', async () => {
        vi.mocked(listRepoFolders)
            .mockResolvedValueOnce([folder('rules', 'rules')])
            .mockResolvedValueOnce([folder('sub', 'rules/sub')])
        render(<RepoFolderPicker open onClose={vi.fn()} onSelect={vi.fn()} repositoryId="design" />)

        await userEvent.click(await screen.findByTestId('expand-rules'))

        await waitFor(() => expect(listRepoFolders).toHaveBeenCalledWith('design', 'rules'))
        expect(await screen.findByTestId('pick-rules/sub')).toBeTruthy()
    })

    it('strips a leading slash from the confirmed path', async () => {
        vi.mocked(listRepoFolders).mockResolvedValue([folder('rules', '/rules')])
        const onSelect = vi.fn()
        render(<RepoFolderPicker open onClose={vi.fn()} onSelect={onSelect} repositoryId="design" />)

        await userEvent.click(await screen.findByTestId('pick-/rules'))
        await userEvent.click(screen.getByTestId('modal-ok'))

        expect(onSelect).toHaveBeenCalledWith('rules')
    })
})

describe('attachChildren', () => {
    it('attaches children to a nested node by key', () => {
        const tree: TreeDataNode[] = [{ key: 'a', title: 'a', children: [{ key: 'a/b', title: 'b' }]}]
        const next = attachChildren(tree, 'a/b', [{ key: 'a/b/c', title: 'c' }])

        expect(next[0]!.children![0]!.children).toEqual([{ key: 'a/b/c', title: 'c' }])
        // The original tree is not mutated.
        expect(tree[0]!.children![0]!.children).toBeUndefined()
    })
})
