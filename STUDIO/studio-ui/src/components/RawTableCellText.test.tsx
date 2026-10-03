import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { paperToken } from 'styles/paper'
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

    it('formats the pieces of the text in their fonts, a piece the compiler resolved included', () => {
        const parent = metaInfo({ usages: [{ start: 23, end: 29, description: 'Datatype Parent', kind: 'datatype' }]})
        render(
            <RawTableCellText
                metaInfo={parent}
                styles={styles}
                text="Datatype Child extends Parent"
                runs={[
                    { text: ' Datatype', style: { color: '#808080' } },
                    { text: ' Child ', style: { bold: true } },
                    { text: 'extends Parent ', style: { color: '#808080' } },
                ]}
            />
        )

        // The runs carry the spaces around the value, which the text shown leaves out.
        const usage = screen.getByTestId('cell-usage-0')
        expect(usage).toHaveTextContent('Parent')
        expect((usage.querySelector('span') as HTMLElement).style.color).toBe('rgb(128, 128, 128)')
        const bold = screen.getByText('Child')
        expect(bold.style.fontWeight).toBe('bold')
    })

    it('draws a piece leading to a table as a link, whatever colour the workbook gives its text', () => {
        const child = { start: 9, end: 14, description: 'Datatype Child', tableId: 't-2', module: 'Claims',
            kind: 'datatype' as const }
        const type = metaInfo({ usages: [child]})
        render(
            <RawTableCellText
                metaInfo={type}
                onOpenUsage={vi.fn()}
                runs={[{ text: 'Datatype ', style: { color: '#808080' } }, { text: 'Child', style: { bold: true } }]}
                styles={styles}
                text="Datatype Child"
            />
        )

        // The piece keeps its font, and takes the colour and the underline of the link.
        const piece = screen.getByTestId('cell-usage-0').querySelector('span') as HTMLElement
        expect(piece.style.fontWeight).toBe('bold')
        expect(piece.style.color).toBe('')
        expect(piece.style.textDecoration).toBe('')
        expect(screen.getByText('Datatype').style.color).toBe('rgb(128, 128, 128)')
    })

    it('draws a piece naming no colour in the ink of the paper, as Excel draws it', () => {
        const runs = [{ text: 'Datatype ', style: { color: '#808080' } }, { text: 'Child', style: { bold: true } }]
        render(<RawTableCellText metaInfo={undefined} runs={runs} styles={styles} text="Datatype Child" />)

        // The table lies on the paper of a workbook whatever the theme, and the piece is written in its ink.
        expect(screen.getByText('Child')).toHaveStyle({ color: paperToken().colorText })
    })

    it('draws the text plain where the runs do not spell it', () => {
        render(<RawTableCellText
            metaInfo={undefined}
            runs={[{ text: 'other', style: { bold: true } }]}
            styles={styles}
            text="Datatype Child"
        />)

        expect(screen.getByText('Datatype Child').tagName).not.toBe('SPAN')
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
