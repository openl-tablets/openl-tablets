import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { RawTableCellText } from './RawTableCellText'
import type { RawTableGridStyles } from './RawTableGrid.styles'

const metaInfo = (over: Partial<NonNullable<RawTableCell['metaInfo']>> = {}): RawTableCell['metaInfo'] => ({
    usages: [{
        start: 2,
        end: 12,
        description: 'Rules Double DriverRisk(int dui)',
        tableId: 't-9',
        module: 'Claims',
        kind: 'rule',
    }],
    ...over,
})

// The table reads its styles once and hands them to every cell it draws.
const styles = { usageLink: 'usage-link' } as RawTableGridStyles

describe('RawTableCellText', () => {
    it('marks the piece of the text the compiler resolved, and leaves the rest alone', () => {
        render(<RawTableCellText metaInfo={metaInfo()} styles={styles} text="= DriverRisk(numDUI)" />)

        // The range is over the cell's own text, so exactly those characters are marked.
        expect(screen.getByTestId('cell-usage-0')).toHaveTextContent('DriverRisk')
        expect(screen.getByTestId('cell-usage-0').parentElement).toHaveTextContent('= DriverRisk(numDUI)')
    })

    it('follows a piece that names a table', async () => {
        const onOpenUsage = vi.fn()
        render(<RawTableCellText metaInfo={metaInfo()} onOpenUsage={onOpenUsage} styles={styles} text="= DriverRisk(numDUI)" />)

        await userEvent.click(screen.getByTestId('cell-usage-0'))

        expect(onOpenUsage).toHaveBeenCalledWith(expect.objectContaining({ tableId: 't-9', module: 'Claims' }))
    })

    it('leaves a piece that names no table alone', async () => {
        const onOpenUsage = vi.fn()
        const noTable = metaInfo({
            usages: [{ start: 0, end: 3, description: 'int', kind: 'other' }],
        })
        render(<RawTableCellText metaInfo={noTable} onOpenUsage={onOpenUsage} styles={styles} text="int" />)

        await userEvent.click(screen.getByTestId('cell-usage-0'))

        expect(onOpenUsage).not.toHaveBeenCalled()
    })

    it('stars the cell a decision table returns, as the Editor did', () => {
        render(<RawTableCellText metaInfo={{ returnCell: true }} styles={styles} text="RET1" />)

        expect(screen.getByTestId('cell-return')).toBeInTheDocument()
    })

    it('shows the text as it stands when the compiler knows nothing about the cell', () => {
        const { container } = render(<RawTableCellText metaInfo={undefined} styles={styles} text="plain" />)

        expect(container).toHaveTextContent('plain')
        expect(screen.queryByTestId('cell-usage-0')).toBeNull()
    })
})
