import type { CSSProperties } from 'react'
import { useCallback, useMemo, useState } from 'react'
import { AutoComplete } from 'antd'

interface Option {
    label: string
    value: string
}

interface OptionGroup {
    label: string
    options: Option[]
}

interface SuggestInputProps {
    value: string
    onChange: (value: string) => void
    options: (Option | OptionGroup)[]
    id?: string
    className?: string
    style?: CSSProperties
    placeholder?: string
    'data-testid'?: string
    'aria-label'?: string
}

/**
 * Case-insensitive match on the option text, so the list narrows to what the user is typing.
 *
 * <p>A group heading never matches. Ant Design reads a matching heading as "the whole group matches" and stops
 * filtering the options under it, so `es` against `Simple Types` would offer every simple type there is.
 */
const matchLabel = (input: string, option?: { label?: unknown, options?: unknown }): boolean =>
    !Array.isArray(option?.options) &&
    typeof option?.label === 'string' &&
    option.label.toLowerCase().includes(input.toLowerCase())

/**
 * A text field with a list of suggestions.
 *
 * <p>The list suggests, it never restricts: any value can be typed, whether it is offered or not.
 *
 * <p>Opening the list always shows all of it. Ant Design matches the filter against the field's current value, so a
 * field that already holds `String` would otherwise open on that one entry and hide every other type. The filter is
 * therefore applied only from the first keystroke, and released again each time the list reopens.
 */
export const SuggestInput = ({ value, onChange, options, ...rest }: SuggestInputProps) => {
    const [typed, setTyped] = useState(false)
    const onSearch = useCallback(() => setTyped(true), [])
    // One object per filter state rather than per render: a grid gives every cell one of these, and a fresh config
    // on each keystroke defeats the memoization rc-select does on it.
    const showSearch = useMemo(() => ({ filterOption: typed ? matchLabel : false, onSearch }), [onSearch, typed])

    return (
        <AutoComplete
            {...rest}
            onChange={onChange}
            // Closing releases the filter, so the next open shows the whole list again. Opening keeps whatever has
            // been typed: a keystroke both searches and opens, in one batch, and it must not undo its own filter —
            // hence the updater form, which reads the search's own state rather than the render's.
            onOpenChange={open => setTyped(current => open && current)}
            options={options}
            showSearch={showSearch}
            value={value}
        />
    )
}
