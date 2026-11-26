import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { afterBreaks } from 'testing/pathBreaks'
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
const styles = {
    usageLink: 'usage-link',
    possibleLink: 'possible-link',
    ruleUsage: 'rule-usage',
    datatypeUsage: 'datatype-usage',
    fieldUsage: 'field-usage',
} as RawTableGridStyles

/** The ink of the paper the table lies on, which the table hands to every cell it draws. */
const INK = '#262626'

describe('RawTableCellText', () => {
    it('marks the piece of the text the compiler resolved, and leaves the rest alone', () => {
        render(<RawTableCellText ink={INK} metaInfo={metaInfo()} styles={styles} text="= DriverRisk(numDUI)" />)

        // The range is over the cell's own text, so exactly those characters are marked.
        expect(screen.getByTestId('cell-usage-0')).toHaveTextContent('DriverRisk')
        expect(screen.getByTestId('cell-usage-0').parentElement).toHaveTextContent('= DriverRisk(numDUI)')
    })

    it('formats the pieces of the text in their fonts, a piece the compiler resolved included', () => {
        // A piece the compiler knows of no kind of its own keeps the fonts of the workbook.
        const parent = metaInfo({ usages: [{ start: 23, end: 29, description: 'Parent', kind: 'other' }] })
        render(
            <RawTableCellText
                ink={INK}
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
        const child = {
            start: 9,
            end: 14,
            description: 'Datatype Child',
            tableId: 't-2',
            module: 'Claims',
            kind: 'datatype' as const
        }
        const type = metaInfo({ usages: [child] })
        render(
            <RawTableCellText
                ink={INK}
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

    it('keeps the colour of a mark on a piece leading to a table', () => {
        const child = {
            start: 9,
            end: 14,
            description: 'Datatype Child',
            tableId: 't-2',
            module: 'Claims',
            kind: 'datatype' as const
        }
        render(
            <RawTableCellText
                ink={INK}
                metaInfo={metaInfo({ usages: [child] })}
                onOpenUsage={vi.fn()}
                runs={[{ text: 'Datatype ' }, { text: 'Child', style: { bold: true, color: '#ff0000' } }]}
                styles={styles}
                text="Datatype Child"
            />
        )

        // A mark is what the author asked the reader to look at, so a link does not paint it over.
        const piece = screen.getByTestId('cell-usage-0').querySelector('span') as HTMLElement
        expect(piece.style.color).toBe('rgb(255, 0, 0)')
        expect(piece.style.textDecoration).toBe('')
    })

    it('draws a piece naming no colour in the ink of the paper, as Excel draws it', () => {
        const runs = [{ text: 'Datatype ', style: { color: '#808080' } }, { text: 'Child', style: { bold: true } }]
        render(<RawTableCellText ink={INK} metaInfo={undefined} runs={runs} styles={styles} text="Datatype Child" />)

        expect(screen.getByText('Child')).toHaveStyle({ color: INK })
    })

    it('draws the text plain where the runs do not spell it', () => {
        render(<RawTableCellText
            ink={INK}
            metaInfo={undefined}
            runs={[{ text: 'other', style: { bold: true } }]}
            styles={styles}
            text="Datatype Child"
        />)

        expect(screen.getByText('Datatype Child').tagName).not.toBe('SPAN')
    })

    /** A cell naming a table the screen follows. */
    const renderLinked = (onOpenUsage: () => void) => render(
        <RawTableCellText
            ink={INK}
            metaInfo={metaInfo()}
            onOpenUsage={onOpenUsage}
            styles={styles}
            text="= DriverRisk(numDUI)"
        />
    )

    it('follows a piece that names a table', async () => {
        const onOpenUsage = vi.fn()
        renderLinked(onOpenUsage)

        await userEvent.click(screen.getByTestId('cell-usage-0'))

        expect(onOpenUsage).toHaveBeenCalledWith(expect.objectContaining({ tableId: 't-9', module: 'Claims' }))
    })

    // The link is a piece of the text, not a <button>, so a path it covers wraps with the text. See EPBDS-17283.
    it('follows a piece that names a table from the keyboard, as a button is followed', async () => {
        const onOpenUsage = vi.fn()
        renderLinked(onOpenUsage)

        const link = screen.getByRole('button', { name: 'DriverRisk' })
        expect(link.tagName).toBe('SPAN')
        await userEvent.tab()
        expect(link).toHaveFocus()
        await userEvent.keyboard('{Enter}')

        expect(onOpenUsage).toHaveBeenCalledTimes(1)
    })

    it('leaves a piece that names no table alone', async () => {
        const onOpenUsage = vi.fn()
        const noTable = metaInfo({
            usages: [{ start: 0, end: 3, description: 'int', kind: 'other' }],
        })
        render(<RawTableCellText ink={INK} metaInfo={noTable} onOpenUsage={onOpenUsage} styles={styles} text="int" />)

        await userEvent.click(screen.getByTestId('cell-usage-0'))

        expect(onOpenUsage).not.toHaveBeenCalled()
    })

    it.each([
        ['follows the key of a message to its texts', 'greeting', 'button'],
        ['leaves alone a key an external resource gives, which has no texts to lead to', undefined, null],
    ])('%s', async (_, messageKey, role) => {
        const onOpenUsage = vi.fn()
        const key = metaInfo({
            usages: [{ start: 7, end: 15, description: 'greeting = Hi', ...(messageKey ? { messageKey } : {}), kind: 'message' }],
        })
        render(
            <RawTableCellText ink={INK} metaInfo={key} onOpenUsage={onOpenUsage} styles={styles} text='= msg("greeting")' />
        )

        const piece = screen.getByTestId('cell-usage-0')
        await userEvent.click(piece)

        expect(piece.getAttribute('role')).toBe(role)
        expect(onOpenUsage).toHaveBeenCalledTimes(messageKey ? 1 : 0)
    })

    it('stars the cell a decision table returns, as the Editor did', () => {
        render(<RawTableCellText ink={INK} metaInfo={{ returnCell: true }} styles={styles} text="RET1" />)

        expect(screen.getByTestId('cell-return')).toBeInTheDocument()
    })

    it('wraps a field path before a dot where a piece of the text ends, as the whole text would', () => {
        // `driver` is resolved, and the dot after it starts the text that follows the piece.
        const driver = metaInfo({ usages: [{ start: 0, end: 6, description: 'Driver driver', kind: 'datatype' }] })
        const { container } = render(
            <RawTableCellText
                ink={INK}
                metaInfo={driver}
                runs={[{ text: 'driver.vehicle', style: { bold: true } }, { text: '.year', style: {} }]}
                styles={styles}
                text="driver.vehicle.year"
            />
        )

        expect(screen.getByTestId('cell-usage-0').querySelector('wbr')).toBeNull()
        expect(afterBreaks(container)).toEqual(['.vehicle', '.year'])
        expect(container).toHaveTextContent(/^driver\.vehicle\.year$/)
    })

    it('shows the text as it stands when the compiler knows nothing about the cell', () => {
        const { container } = render(<RawTableCellText ink={INK} metaInfo={undefined} styles={styles} text="plain" />)

        expect(container).toHaveTextContent('plain')
        expect(screen.queryByTestId('cell-usage-0')).toBeNull()
    })
    it('draws a piece naming a rule, a datatype or a field in the colour of its kind, a link or not', () => {
        const usages: NonNullable<NonNullable<RawTableCell['metaInfo']>['usages']> = [
            { start: 2, end: 12, description: 'Rules Double DriverRisk(Driver d)', tableId: 't-9', module: 'M', kind: 'rule' },
            { start: 13, end: 19, description: 'Datatype Driver', kind: 'datatype' },
            { start: 20, end: 23, description: 'Field age', tableId: 't-2', module: 'M', kind: 'field' },
            { start: 24, end: 29, description: 'Data rates', tableId: 't-3', module: 'M', kind: 'data' },
        ]
        render(
            <RawTableCellText
                ink={INK}
                metaInfo={{ usages }}
                onOpenUsage={vi.fn()}
                runs={[{ text: '= DriverRisk(Driver.age rates)', style: { color: '#808080' } }]}
                styles={styles}
                text="= DriverRisk(Driver.age rates)"
            />
        )

        expect(screen.getByTestId('cell-usage-0')).toHaveClass('usage-link', 'rule-usage')
        // A piece leading nowhere is drawn in its kind all the same, and not as a link.
        expect(screen.getByTestId('cell-usage-1')).toHaveClass('datatype-usage')
        expect(screen.getByTestId('cell-usage-1')).not.toHaveAttribute('role')
        expect(screen.getByTestId('cell-usage-2')).toHaveClass('usage-link', 'field-usage')
        // A data table is a link in the colour of a link.
        expect(screen.getByTestId('cell-usage-3')).toHaveClass('usage-link')
        expect(screen.getByTestId('cell-usage-3')).not.toHaveClass('rule-usage', 'datatype-usage', 'field-usage')
        // The colour of the workbook gives way to the colour of the kind.
        for (const index of [0, 1, 2]) {
            const run = screen.getByTestId(`cell-usage-${index}`).querySelector('span') as HTMLElement
            expect(run.style.color).toBe('')
        }
    })
})
