import { Children, cloneElement, isValidElement, useId, type ReactElement, type ReactNode } from 'react'
import { useStyles } from './FieldRow.styles'

interface FieldRowProps {
    /** Field label; the colon is added by the style, exactly as the administration forms do. */
    label: string
    /** Id of the control this row labels. Generated when the control does not bring its own. */
    htmlFor?: string
    /** Marks the field as required with a red asterisk to the left of the label. */
    required?: boolean
    /** Aligns the label with the top of a multi-line control instead of centring it. */
    alignTop?: boolean
    /** Width of the label column in pixels. Defaults to 110; widen it for forms with longer labels. */
    labelWidth?: number
    /** The input control this row labels. */
    children: ReactNode
}

/** The field name a control is submitted and remembered under, derived from its label. */
const fieldName = (label: string): string => label.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')

// The native elements a `<label for>` may point at. Anything else — a `<span>` or `<div>` a read-only
// row shows its value in — is not a form field, so the label is left without a `for` that the browser
// (and assistive tech) would flag as pointing at a non-labelable element.
const LABELABLE_TAGS = new Set(['input', 'select', 'textarea', 'button', 'meter', 'output', 'progress'])

/**
 * The control this row labels, carrying the id the label points at and a name derived from the label.
 * A control that brings its own id or name keeps it. A plain display element (a value shown as text) is
 * left alone and reported as unlabelable, so the row does not bind its label to a non-form element.
 */
const identify = (children: ReactNode, id: string, name: string): { control: ReactNode, controlId?: string } => {
    // The first element is the control; anything after it (a validation message, a hint) follows it.
    const parts = Children.toArray(children)
    const index = parts.findIndex(isValidElement)
    const control = parts[index]
    if (index === -1 || !isValidElement(control)) {
        return { control: children }
    }
    const props = control.props as { id?: string, name?: string }
    // A control that brings its own id keeps it, and the label points at that one.
    if (props.id) {
        return { control: children, controlId: props.id }
    }
    // A read-only value rendered as a bare span/div is not a form control; label it by proximity only.
    if (typeof control.type === 'string' && !LABELABLE_TAGS.has(control.type)) {
        return { control: children }
    }
    parts[index] = cloneElement(control as ReactElement<{ id?: string, name?: string }>, {
        id,
        ...(props.name ? {} : { name }),
    })
    return { control: parts, controlId: id }
}

/**
 * Horizontal labeled field row. Shows a right-aligned label with an optional required asterisk on its
 * left, followed by the control. The colons stay aligned across rows, matching the administration form
 * screens.
 *
 * The label is a real `label` element bound to its control: the control is given an id when it does not
 * carry one, so clicking the label focuses the field and assistive technology reads the two as one.
 */
export const FieldRow = ({ label, htmlFor, required = false, alignTop = false, labelWidth, children }: FieldRowProps) => {
    const { styles, cx } = useStyles()
    const generatedId = useId()
    const identified = htmlFor ? { control: children, controlId: htmlFor } : identify(children, generatedId, fieldName(label))
    const { control, controlId } = identified

    return (
        <div className={cx(styles.row, alignTop && styles.rowTop)}>
            <label
                className={cx(styles.label, alignTop && styles.labelTop, required && styles.required)}
                htmlFor={controlId}
                style={labelWidth === undefined ? undefined : { width: labelWidth }}
            >
                {label}
            </label>
            <div className={styles.control}>{control}</div>
        </div>
    )
}
