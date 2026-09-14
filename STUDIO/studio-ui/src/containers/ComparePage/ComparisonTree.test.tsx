import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ComparisonTree } from './ComparisonTree'
import type { Comparison } from 'types/compare'

vi.mock('react-i18next', () => {
    // The key with the values it was given, so a test can assert what a message is built from.
    const t = (key: string, params?: Record<string, string>) => (params
        ? `${key} ${Object.values(params).join(' ')}`
        : key)
    return { useTranslation: () => ({ t }) }
})

// Ant Design's Tree needs layout jsdom does not run; it stands in as the nested list it draws.
vi.mock('antd', () => {
    const Node = ({ node, onSelect, onExpand }: any) => (
        <li>
            {onExpand && <button data-testid={`expand-${node.key}`} onClick={() => onExpand([])} type="button" />}
            {node.selectable === false
                ? <span>{node.title}</span>
                : <button onClick={() => onSelect([node.key])}>{node.title}</button>}
            {node.children?.length > 0 && (
                <ul>
                    {node.children.map((child: any) => <Node key={child.key} node={child} onSelect={onSelect} />)}
                </ul>
            )}
        </li>
    )
    return {
        Empty: Object.assign(({ description }: any) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 'simple' }),
        Tree: ({ treeData, onSelect, expandedKeys, onExpand }: any) => (
            <ul data-expanded={JSON.stringify(expandedKeys)} data-testid="tree">
                {treeData.map((node: any) => (
                    <Node
                        key={node.key}
                        node={expandedKeys?.includes(node.key) === false ? { ...node, children: []} : node}
                        onExpand={onExpand}
                        onSelect={onSelect}
                    />
                ))}
            </ul>
        ),
    }
})

const COMPARISON: Comparison = {
    id: 'cmp-1',
    identical: false,
    sheets: [
        {
            id: '0',
            name: 'step1',
            type: 'sheet',
            status: 'changed',
            children: [
                { id: '0-0', name: 'Data String phrases', type: 'table', status: 'removed' },
                { id: '0-1', name: 'Data Integer numbers', type: 'table', status: 'equal' },
                {
                    id: '0-2',
                    name: 'Data Customer customers',
                    type: 'table',
                    status: 'changed',
                    changes: [{ property: 'size', first: '5x6', second: '1x1' }, { property: 'name', second: 'Other' }],
                },
            ],
        },
        {
            id: '1',
            name: 'Intro',
            type: 'sheet',
            status: 'equal',
            children: [{ id: '1-0', name: 'Tutorial2', type: 'table', status: 'equal' }],
        },
    ],
}

describe('ComparisonTree', () => {
    it('lists the elements that differ, and their sheets', () => {
        render(<ComparisonTree comparison={COMPARISON} onSelect={vi.fn()} showEqualElements={false} />)

        expect(screen.getByText('step1')).toBeInTheDocument()
        expect(screen.getByText('Data String phrases')).toBeInTheDocument()
        expect(screen.queryByText('Data Integer numbers')).toBeNull()
        // A sheet whose elements are all equal is left out with them.
        expect(screen.queryByText('Intro')).toBeNull()
    })

    it('lists the equal elements when they are asked for', () => {
        render(<ComparisonTree showEqualElements comparison={COMPARISON} onSelect={vi.fn()} />)

        expect(screen.getByText('Data Integer numbers')).toBeInTheDocument()
        expect(screen.getByText('Intro')).toBeInTheDocument()
    })

    it('opens a sheet the equal elements bring back', () => {
        const { rerender } = render(
            <ComparisonTree comparison={COMPARISON} onSelect={vi.fn()} showEqualElements={false} />
        )
        // A sheet whose every element is equal is not listed at all while they are left out.
        expect(screen.queryByText('Intro')).toBeNull()

        rerender(<ComparisonTree showEqualElements comparison={COMPARISON} onSelect={vi.fn()} />)

        // It is listed now, and open, so what it holds is read without opening it by hand.
        expect(screen.getByText('Intro')).toBeInTheDocument()
        expect(screen.getByTestId('tree').getAttribute('data-expanded')).toContain('"1"')
    })

    it('leaves closed what the reader closed', async () => {
        render(<ComparisonTree showEqualElements comparison={COMPARISON} onSelect={vi.fn()} />)

        await userEvent.click(screen.getByTestId('expand-0'))

        expect(screen.getByTestId('tree').getAttribute('data-expanded')).not.toContain('"0"')
        expect(screen.queryByText('Data Customer customers')).toBeNull()
    })

    it('marks an element with what became of it', () => {
        const { container } = render(<ComparisonTree showEqualElements comparison={COMPARISON} onSelect={vi.fn()} />)

        // The same marks the old window used: a file lost, one that reads differently, one that reads
        // the same - and, where a file is gained, one gained.
        const marks = [...container.querySelectorAll('[title]')].map(node => node.getAttribute('title'))
        expect(marks).toContain('status_removed')
        expect(marks).toContain('status_changed')
        expect(marks).toContain('status_equal')
    })

    it('marks an element the second file adds', () => {
        const added: Comparison = {
            ...COMPARISON,
            sheets: [{ ...COMPARISON.sheets[0]!, children: [{ id: '0-3', name: 'New', type: 'table', status: 'added' }]}],
        }

        const { container } = render(<ComparisonTree comparison={added} onSelect={vi.fn()} showEqualElements={false} />)

        const marks = [...container.querySelectorAll('[title]')].map(node => node.getAttribute('title'))
        expect(marks).toContain('status_added')
    })

    it('says what a property of a changed element reads in each file', () => {
        render(<ComparisonTree comparison={COMPARISON} onSelect={vi.fn()} showEqualElements={false} />)

        expect(screen.getByText('change size 5x6 1x1')).toBeInTheDocument()
        // A property only one file states reads as not set in the other.
        expect(screen.getByText('change name change_absent Other')).toBeInTheDocument()
    })

    it('answers with the element that was picked', async () => {
        const onSelect = vi.fn()
        render(<ComparisonTree comparison={COMPARISON} onSelect={onSelect} showEqualElements={false} />)

        await userEvent.click(screen.getByText('Data String phrases'))

        expect(onSelect).toHaveBeenCalledWith('0-0')
    })

    it('says that there is nothing to compare when every element is equal', () => {
        render(
            <ComparisonTree
                comparison={{ ...COMPARISON, sheets: [COMPARISON.sheets[1]!]}}
                onSelect={vi.fn()}
                showEqualElements={false}
            />
        )

        expect(screen.getByText('no_elements')).toBeInTheDocument()
    })
})
