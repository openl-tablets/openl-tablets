import React from 'react'
import { useTranslation } from 'react-i18next'
import { Flex, Tooltip, Typography } from 'antd'
import type { MessageCell } from './bundle'
import type { TextSpot } from '../../services/projectStatus'
import { useStyles } from './LocalizationWorkspace.styles'
import type { MessageWarning } from './messageWarnings'
import { ProblemList } from './ProblemList'

interface MessageTextProps {
    /** What the language shows for the key. */
    cell: MessageCell
    nameOf: (tag: string) => string
    /** The warnings about the own text, whose pieces are marked in it. */
    warnings: readonly MessageWarning[]
}

/** A piece of the text marked, and what is wrong with it. */
interface MarkedSpot extends TextSpot {
    warnings: MessageWarning[]
}

/**
 * The pieces of the text the warnings are about, in the order of the text, each with the warnings about it. A piece
 * within another one is joined to it, together with its warnings.
 */
const spotsOf = (warnings: readonly MessageWarning[], length: number): MarkedSpot[] => {
    const spots = warnings
        .flatMap(warning => warning.spots.map(spot => ({ ...spot, warning })))
        .filter(spot => spot.end <= length)
        .toSorted((one, other) => one.start - other.start)
    const joined: MarkedSpot[] = []
    for (const { start, end, warning } of spots) {
        const last = joined.at(-1)
        if (last !== undefined && start < last.end) {
            last.end = Math.max(last.end, end)
            if (!last.warnings.includes(warning)) {
                last.warnings.push(warning)
            }
        } else {
            joined.push({ start, end, warnings: [warning]})
        }
    }
    return joined
}

/**
 * The text with the pieces the warnings are about marked, so the reader sees where to mend it. A marked piece tells
 * what is wrong with it when pointed at.
 */
const markedText = (text: string, warnings: readonly MessageWarning[], spotClass: string): React.ReactNode => {
    const spots = spotsOf(warnings, text.length)
    if (spots.length === 0) {
        return text
    }
    const pieces: React.ReactNode[] = []
    let read = 0
    for (const { start, end, warnings: told } of spots) {
        if (start > read) {
            pieces.push(text.slice(read, start))
        }
        pieces.push(
            <Tooltip key={start} title={<ProblemList problems={told} />}>
                <span className={spotClass} data-testid="localization-text-problem">
                    {text.slice(start, end)}
                </span>
            </Tooltip>
        )
        read = end
    }
    pieces.push(text.slice(read))
    return pieces
}

/** Draws what a language shows for a key: its own text, an inherited one, an empty text, or nothing at all. */
export const MessageText = ({ cell, nameOf, warnings }: MessageTextProps) => {
    const { t } = useTranslation('localization')
    const { styles } = useStyles()
    switch (cell.kind) {
        case 'own':
            return <Typography.Text className={styles.text}>{markedText(cell.text, warnings, styles.problemSpot)}</Typography.Text>
        case 'empty':
            return <Typography.Text type="secondary">{t('empty')}</Typography.Text>
        case 'inherited':
            return (
                <Flex vertical>
                    <Typography.Text italic className={styles.text} type="secondary">
                        {cell.text === '' ? t('empty') : cell.text}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                        <small>
                            {cell.project === undefined
                                ? t('inherited_from', { language: nameOf(cell.from) })
                                : t('inherited_from_project', { language: nameOf(cell.from), project: cell.project.name })}
                        </small>
                    </Typography.Text>
                </Flex>
            )
        case 'missing':
            return <Typography.Text type="warning">{t('missing')}</Typography.Text>
    }
}
