import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { FileTree } from './FileTree'
import type { FsNode } from '../../types/files'

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ i18n: { language: 'en', resolvedLanguage: 'en' }, t: (key: string) => key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }), cx: (...a: unknown[]) => a.filter(Boolean).join(' ') }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
}))

vi.mock('@ant-design/icons', () => {
    const Icon = () => null
    return {
        DeleteOutlined: Icon,
        EditOutlined: Icon,
        FileExcelOutlined: Icon,
        FileOutlined: Icon,
        FileTextOutlined: Icon,
        FolderOpenOutlined: Icon,
        FolderOutlined: Icon,
        PlusOutlined: Icon,
    }
})

interface Node {
    key: string
    isLeaf?: boolean
    title?: unknown
    children?: Node[]
}

vi.mock('antd', () => {
    const renderNodes = (
        nodes: Node[],
        onSelect: ((keys: string[], info: { node: { key: string, isLeaf?: boolean } }) => void) | undefined,
        selected: string[]
    ): unknown =>
        nodes.map(node => (
            <div key={node.key}>
                <button
                    data-selected={selected.includes(node.key) || undefined}
                    data-testid={`file-node-${node.key}`}
                    onClick={() => onSelect?.([node.key], { node: { key: node.key, isLeaf: !!node.isLeaf } })}
                >
                    {node.title as never}
                </button>
                {node.children ? (renderNodes(node.children, onSelect, selected) as never) : null}
            </div>
        ))
    const Tree = ({ treeData, onSelect, selectedKeys }: Record<string, unknown>) => (
        <div>{renderNodes(treeData as Node[], onSelect as never, (selectedKeys as string[]) ?? []) as never}</div>
    )
    const Empty = ({ description }: Record<string, unknown>) => <div>{description as never}</div>
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Skeleton = () => <div>skeleton</div>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    return { Empty, Skeleton, Tooltip, Tree }
})

const node = (path: string, size?: number): FsNode => ({
    path,
    name: path.slice(path.lastIndexOf('/') + 1),
    basePath: '',
    type: 'file',
    ...(size !== undefined ? { size } : {}),
})

const files: FsNode[] = [node('Pricing.xlsx', 86016), node('rules/Second.xlsx')]
const formatSize = (bytes: number) => new Intl.NumberFormat('en', {
    style: 'unit',
    unit: 'kilobyte',
    unitDisplay: 'short',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
}).format(bytes / 1024)

describe('FileTree', () => {
    it('renders files, folders and sizes', () => {
        render(<FileTree files={files} projectId="p1" />)

        expect(screen.getByTestId('files-p1')).toBeTruthy()
        expect(screen.getByText('Pricing.xlsx')).toBeTruthy()
        expect(screen.getByText('Second.xlsx')).toBeTruthy()
        // 86016 bytes is shown right of the name with locale-aware unit formatting.
        expect(screen.getByText(formatSize(86016))).toBeTruthy()
    })

    it('selects a leaf file by path', async () => {
        const onSelectFile = vi.fn()
        render(<FileTree files={files} onSelectFile={onSelectFile} projectId="p1" />)

        await userEvent.click(screen.getByTestId('file-node-rules/Second.xlsx'))

        expect(onSelectFile).toHaveBeenCalledWith('rules/Second.xlsx')
    })

    it('selects a folder by path', async () => {
        const onSelectFile = vi.fn()
        render(<FileTree files={files} onSelectFile={onSelectFile} projectId="p1" />)

        await userEvent.click(screen.getByTestId('file-node-rules'))

        expect(onSelectFile).toHaveBeenCalledWith('rules')
    })

    it('highlights the currently selected file', () => {
        render(<FileTree files={files} projectId="p1" selectedPath="Pricing.xlsx" />)

        expect(screen.getByTestId('file-node-Pricing.xlsx').getAttribute('data-selected')).toBe('true')
    })

    it('filters files by path', () => {
        render(<FileTree files={files} filter="second" projectId="p1" />)

        expect(screen.queryByText('Pricing.xlsx')).toBeNull()
        expect(screen.getByText('Second.xlsx')).toBeTruthy()
    })

    it('marks files with pending local changes', () => {
        render(
            <FileTree
                changes={new Map([['Pricing.xlsx', 'modified']])}
                files={files}
                projectId="p1"
            />
        )

        expect(screen.getByTestId('file-change-modified-Pricing.xlsx')).toBeTruthy()
    })
})
