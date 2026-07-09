import React from 'react'
import { Button, Input } from 'antd'
import { CloseOutlined, PlusOutlined } from '@ant-design/icons'
import { useStyles } from './StringListEditor.styles'

interface StringListEditorProps {
    values: string[]
    onChange: (values: string[]) => void
    addLabel: string
    removeLabel: string
    placeholder?: string
    mono?: boolean
    'data-testid'?: string
}

/**
 * Edits an ordered list of short strings as one input per line, each with a remove control, plus an
 * "add" affordance. Used for descriptor lists (sources, method-filter patterns) where each entry is
 * a single value and inline per-line editing reads better than a free-text area.
 */
export const StringListEditor: React.FC<StringListEditorProps> = ({
    values,
    onChange,
    addLabel,
    removeLabel,
    placeholder,
    mono,
    'data-testid': testId,
}) => {
    const { styles, cx } = useStyles()
    const setAt = (index: number, value: string) => onChange(values.map((entry, i) => (i === index ? value : entry)))
    const removeAt = (index: number) => onChange(values.filter((_, i) => i !== index))
    const add = () => onChange([...values, ''])

    return (
        <div data-testid={testId}>
            {values.map((value, index) => (
                <div key={index} className={styles.row}>
                    <Input
                        className={cx(mono && styles.mono)}
                        onChange={(event) => setAt(index, event.target.value)}
                        placeholder={placeholder}
                        value={value}
                    />
                    <Button
                        aria-label={removeLabel}
                        className={styles.remove}
                        icon={<CloseOutlined />}
                        onClick={() => removeAt(index)}
                        size="small"
                        type="text"
                    />
                </div>
            ))}
            <Button className={styles.add} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {addLabel}
            </Button>
        </div>
    )
}
