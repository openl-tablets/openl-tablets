import React, { useLayoutEffect, useRef } from 'react'
import { joinMarks, type MarkInks, markedRuns } from 'components/textMarks'
import type { RawTextMark, RawTextMarkColor } from 'types/tables'

/** Where the selection of a field stands in its text: from the first character it holds to after the last one. */
export interface TextSelection {
    from: number
    to: number
}

interface MarkedTextFieldProps {
    value: string
    marks: RawTextMark[]
    /** Whether the text takes several lines, where Enter writes a line of its own. */
    multiline: boolean
    /** The colour each mark is drawn in. */
    inks: MarkInks
    /** Where the selection is put when the marks are changed from outside the field. */
    selection: TextSelection | null
    onChange: (value: string, marks: RawTextMark[]) => void
    /** Keeps what was written and closes the cell. */
    onCommit: () => void
    /** Leaves the cell as it was. */
    onCancel: () => void
    /** Asked for the room to write several lines, which Alt+Enter asks for from a one-line field. */
    onSwitch?: (() => void) | undefined
    className?: string
    /** The element written in, which the marks bar reads the selection from. */
    fieldRef: React.RefObject<HTMLDivElement | null>
}

/** The attribute a marked piece of the text names its colour by. */
const MARK = 'data-mark'

/** The text of a field and its marks, read from the pieces it shows. */
export const readField = (field: HTMLElement): { value: string, marks: RawTextMark[] } => {
    let value = ''
    const marks: RawTextMark[] = []
    // The last node of the field, however deep: a browser leaves a <br> there in a field it emptied.
    let last: Node | null = field.lastChild
    while (last?.lastChild != null) {
        last = last.lastChild
    }
    const walk = (node: Node, color: RawTextMarkColor | null) => {
        if (node.nodeType === Node.TEXT_NODE) {
            const text = node.textContent ?? ''
            if (color !== null && text.length > 0) {
                marks.push({ from: value.length, to: value.length + text.length, color })
            }
            value += text
        } else if (node instanceof HTMLBRElement) {
            // A line break is written as a text of its own; a <br> is what a browser leaves in a field it emptied.
            value += node === last ? '' : '\n'
        } else if (node instanceof HTMLElement) {
            const own = node.getAttribute(MARK) as RawTextMarkColor | null
            node.childNodes.forEach(child => walk(child, own ?? color))
        }
    }
    field.childNodes.forEach(child => walk(child, null))
    return { value, marks: joinMarks(marks) }
}

/** Draws a text and its marks into a field, each marked piece bold in the ink of its mark. */
export const drawField = (field: HTMLElement, value: string, marks: readonly RawTextMark[], inks: MarkInks) => {
    field.replaceChildren(...markedRuns(value, undefined, marks, inks).map(run => {
        if (run.mark === undefined) {
            return document.createTextNode(run.text)
        }
        const marked = document.createElement('span')
        marked.setAttribute(MARK, run.mark)
        marked.style.fontWeight = 'bold'
        marked.style.color = inks[run.mark]
        marked.textContent = run.text
        return marked
    }))
}

/** Where a point of the selection stands in the text of a field. */
const offsetOf = (field: HTMLElement, node: Node, offset: number): number => {
    const before = document.createRange()
    before.selectNodeContents(field)
    before.setEnd(node, offset)
    const holder = document.createElement('div')
    holder.append(before.cloneContents())
    return readField(holder).value.length
}

/** Where the selection of a field stands in its text, or null when the selection is not in it. */
export const selectionIn = (field: HTMLElement): TextSelection | null => {
    const selection = window.getSelection()
    const range = selection !== null && selection.rangeCount > 0 ? selection.getRangeAt(0) : null
    if (range === null || !field.contains(range.startContainer) || !field.contains(range.endContainer)) {
        return null
    }
    const from = offsetOf(field, range.startContainer, range.startOffset)
    const to = offsetOf(field, range.endContainer, range.endOffset)
    return { from: Math.min(from, to), to: Math.max(from, to) }
}

/** The text node and the offset in it where a point of the text of a field stands. */
const pointAt = (field: HTMLElement, at: number): [Node, number] => {
    const texts = document.createTreeWalker(field, NodeFilter.SHOW_TEXT)
    let left = at
    while (texts.nextNode() !== null) {
        const length = texts.currentNode.textContent?.length ?? 0
        if (left <= length) {
            return [texts.currentNode, left]
        }
        left -= length
    }
    return [field, field.childNodes.length]
}

/** Puts the selection of a field over a piece of its text. */
export const selectIn = (field: HTMLElement, { from, to }: TextSelection) => {
    const range = document.createRange()
    range.setStart(...pointAt(field, from))
    range.setEnd(...pointAt(field, to))
    const selection = window.getSelection()
    selection?.removeAllRanges()
    selection?.addRange(range)
}

/** Writes a text where the selection stands, in the piece the selection starts in. */
const insertText = (text: string) => {
    const selection = window.getSelection()
    const range = selection !== null && selection.rangeCount > 0 ? selection.getRangeAt(0) : null
    if (range === null) {
        return
    }
    range.deleteContents()
    const written = document.createTextNode(text)
    range.insertNode(written)
    range.setStartAfter(written)
    range.collapse(true)
    selection?.removeAllRanges()
    selection?.addRange(range)
}

/**
 * A field the text of a cell is written in together with its marks, each marked piece shown bold in its colour.
 *
 * <p>Writing inside a marked piece keeps it marked. The text is written plain: what is pasted loses its fonts, and
 * the keys that format a text elsewhere format nothing here.
 */
export const MarkedTextField: React.FC<MarkedTextFieldProps> = ({
    value, marks, multiline, inks, selection, onChange, onCommit, onCancel, onSwitch, className, fieldRef,
}) => {
    // What the field reported last, which it shows already: only marks changed from outside are drawn again.
    const reported = useRef<{ value: string, marks: RawTextMark[] } | null>(null)

    useLayoutEffect(() => {
        const field = fieldRef.current
        if (field === null || (reported.current?.value === value && reported.current.marks === marks)) {
            return
        }
        drawField(field, value, marks, inks)
        reported.current = { value, marks }
        field.focus()
        selectIn(field, selection ?? { from: value.length, to: value.length })
    }, [fieldRef, inks, marks, selection, value])

    const changed = () => {
        const field = fieldRef.current
        if (field !== null) {
            const read = readField(field)
            reported.current = read
            onChange(read.value, read.marks)
        }
    }

    /** The keys of the other fields of a cell: Enter keeps what was written, Escape leaves the cell as it was. */
    const keys = (event: React.KeyboardEvent) => {
        const formatting = (event.ctrlKey || event.metaKey) && ['b', 'i', 'u'].includes(event.key.toLowerCase())
        if (formatting) {
            event.preventDefault()
        } else if (event.key === 'Enter' && event.altKey && !multiline) {
            event.preventDefault()
            onSwitch?.()
        } else if (event.key === 'Enter' && (multiline ? event.ctrlKey || event.metaKey : true)) {
            event.preventDefault()
            onCommit()
        } else if (event.key === 'Enter') {
            event.preventDefault()
            insertText('\n')
            changed()
        } else if (event.key === 'Escape') {
            onCancel()
        } else if ((event.key === 'F2' || event.key === 'F3') && fieldRef.current !== null) {
            // The two ends of what the cell already holds, as in the other fields of a cell.
            event.preventDefault()
            const at = event.key === 'F2' ? 0 : readField(fieldRef.current).value.length
            selectIn(fieldRef.current, { from: at, to: at })
        }
    }

    return (
        <div
            ref={fieldRef}
            contentEditable
            suppressContentEditableWarning
            aria-multiline={multiline}
            className={className}
            data-testid="table-cell-marked-input"
            onBlur={() => onCommit()}
            onInput={changed}
            onKeyDown={keys}
            role="textbox"
            onPaste={event => {
                event.preventDefault()
                insertText(event.clipboardData.getData('text/plain'))
                changed()
            }}
        />
    )
}
