import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input, Tooltip } from 'antd'
import { CloseCircleOutlined, WarningOutlined } from '@ant-design/icons'
import type { Bundle, BundleLanguage } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { isError, problemsOf, type MessageProblem } from './messageProblems'
import { MessageText } from './MessageText'
import type { MessageDrafts } from './useMessageDrafts'

interface MessageCellProps {
    bundle: Bundle
    language: BundleLanguage
    messageKey: string
    drafts: MessageDrafts
    canWrite: boolean
    styles: LocalizationStyles
    nameOf: (tag: string) => string
    /** Lists the problems of the text under it, where there is room for them, rather than in a tooltip. */
    listProblems?: boolean | undefined
}

/**
 * What is wrong with a text, a line for each problem. Under the text each line is marked as a warning, or as an
 * error, so it is told from the text itself; in a tooltip the lines are plain.
 */
const ProblemList = ({ problems, styles, marked }: {
    problems: MessageProblem[]
    styles: LocalizationStyles
    marked: boolean
}) => {
    const { t } = useTranslation('localization')
    if (!marked) {
        return (
            <ul className={styles.problemList} data-testid="localization-cell-problems">
                {problems.map(problem => <li key={problem}>{t(`problem_${problem}`)}</li>)}
            </ul>
        )
    }
    return (
        <ul className={styles.problemLines} data-testid="localization-cell-problems">
            {problems.map(problem => (
                <li key={problem} className={isError(problem) ? styles.problemError : styles.problemWarning}>
                    {isError(problem) ? <CloseCircleOutlined /> : <WarningOutlined />}
                    {t(`problem_${problem}`)}
                </li>
            ))}
        </ul>
    )
}

/**
 * The text of a key in a language, written in place.
 *
 * <p>A click opens the text for writing. Enter keeps it, Shift+Enter starts a new line, Escape gives it up, and
 * leaving the cell keeps it. What is being written lives in the cell, so a keystroke draws no other cell.
 *
 * <p>A text opened in a cell that shows an inherited one starts empty: keeping it empty changes nothing, and the
 * language goes on inheriting. A language other than the default one can give its own text up for the inherited
 * one.
 *
 * <p>What is wrong with the text is marked on the cell, and listed under the text while it is typed, before it is
 * kept.
 */
export const MessageCell = ({
    bundle,
    language,
    messageKey,
    drafts,
    canWrite,
    styles,
    nameOf,
    listProblems = false,
}: MessageCellProps) => {
    const { t } = useTranslation('localization')
    const own = drafts.textOf(language, messageKey)
    const [draft, setDraft] = useState<string | null>(null)
    // An empty text typed in a cell with no text of its own is no text at all.
    const typed = draft === '' && own === undefined ? undefined : draft
    const problems = problemsOf(bundle, language, messageKey, draft === null
        ? drafts.textOf
        : (asked, key) => (asked === language && key === messageKey ? typed ?? undefined : drafts.textOf(asked, key)))

    const keep = () => {
        if (draft !== null && draft !== (own ?? '')) {
            drafts.write(language, messageKey, draft)
        }
        setDraft(null)
    }
    const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            keep()
        } else if (event.key === 'Escape') {
            setDraft(null)
        }
    }

    if (draft === null) {
        return (
            <div
                className={drafts.changed(language.tag, messageKey) ? styles.changed : styles.cell}
                data-testid="localization-cell"
                onClick={canWrite ? () => setDraft(own ?? '') : undefined}
            >
                <MessageText
                    bundle={bundle}
                    language={language}
                    messageKey={messageKey}
                    nameOf={nameOf}
                    styles={styles}
                    textOf={drafts.textOf}
                />
                {listProblems && problems.length > 0 && <ProblemList marked problems={problems} styles={styles} />}
                {!listProblems && problems.length > 0 && (
                    <Tooltip title={<ProblemList marked={false} problems={problems} styles={styles} />}>
                        <WarningOutlined
                            className={problems.some(isError) ? styles.errorMark : styles.problemMark}
                            data-testid="localization-cell-problem"
                        />
                    </Tooltip>
                )}
            </div>
        )
    }
    return (
        <div className={styles.cell}>
            <Input.TextArea
                autoFocus
                autoSize
                data-testid="localization-cell-input"
                onBlur={keep}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={onKeyDown}
                size="small"
                value={draft}
            />
            {problems.length > 0 && <ProblemList marked problems={problems} styles={styles} />}
            {language.tag !== '' && own !== undefined && (
                <Button
                    className={styles.inherit}
                    data-testid="localization-cell-inherit"
                    size="small"
                    type="link"
                    // Pressed before the field loses the focus, so leaving it does not keep the text first.
                    onMouseDown={event => {
                        event.preventDefault()
                        drafts.write(language, messageKey, null)
                        setDraft(null)
                    }}
                >
                    {t('use_inherited')}
                </Button>
            )}
        </div>
    )
}
