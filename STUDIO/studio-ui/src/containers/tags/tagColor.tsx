import { useMemo } from 'react'
import { TagOutlined } from '@ant-design/icons'
import { useTheme } from 'antd-style'
import { tagTypeRank, useTagTypeOrder } from './tagOrder'

/**
 * The colors of the configured tag types, neighbours set apart. The red, orange and gold hues are left out: the
 * screens use them to warn and to report errors, so a tag in one of them would read as a problem.
 */
const TAG_COLORS = ['blue', 'green', 'magenta', 'cyan', 'purple', 'lime', 'geekblue'] as const

/** The gray of a tag type that is not configured. */
const UNDEFINED_COLOR = 'default'

export type TagColor = typeof TAG_COLORS[number] | typeof UNDEFINED_COLOR

/**
 * The colors of the tag types, by the configured ones. Every value of a type takes the color of the type, so the
 * chips of one type look alike wherever a project is shown.
 *
 * The configured tag types take the colors in the order an administrator arranged them in, so neighbouring types
 * differ and the colors repeat only past the seventh type. A type that is not configured is gray. Types match
 * regardless of case.
 */
export const tagColorsOf = (configured: readonly string[]): ((type: string) => TagColor) => {
    const rank = tagTypeRank(configured)
    return type => {
        const place = rank(type)
        return place < configured.length ? TAG_COLORS[place % TAG_COLORS.length] as TagColor : UNDEFINED_COLOR
    }
}

/** The colors of the tag types on the projects screen, by the configured tag types. */
export const useTagColor = (): ((type: string) => TagColor) => {
    const configured = useTagTypeOrder()
    return useMemo(() => tagColorsOf(configured), [configured])
}

/** The tag icon in the color of a tag type: marks the type where the projects are filtered or grouped by it. */
export const TagTypeMark = ({ type, className, testId }: {
    type: string
    className?: string | undefined
    testId?: string | undefined
}) => {
    const token = useTheme()
    const color = useTagColor()(type)
    return (
        <TagOutlined
            className={className}
            data-testid={testId}
            style={{ color: color === UNDEFINED_COLOR ? token.colorTextTertiary : token[color] }}
        />
    )
}
