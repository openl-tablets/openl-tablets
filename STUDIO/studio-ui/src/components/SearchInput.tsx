import { forwardRef } from 'react'
import { Input } from 'antd'
import type { InputProps, InputRef } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useStyles } from './SearchInput.styles'

/**
 * Text input with a search icon built into the field and a clear button.
 *
 * Filtering runs as the user types (the caller debounces), so there is no separate search button.
 * Accepts every Ant Design {@link InputProps}; a caller-supplied {@code className} is merged with
 * the shared styling, and {@code prefix} or {@code allowClear} can be overridden when needed.
 */
export const SearchInput = forwardRef<InputRef, InputProps>(({ className, ...props }, ref) => {
    const { styles, cx } = useStyles()
    return (
        <Input
            ref={ref}
            allowClear
            prefix={<SearchOutlined />}
            {...props}
            className={cx(styles.search, className)}
        />
    )
})

SearchInput.displayName = 'SearchInput'
