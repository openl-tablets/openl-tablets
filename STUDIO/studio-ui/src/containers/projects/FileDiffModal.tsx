import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Empty, Modal, Select, Skeleton, Space } from 'antd'
import { createStyles } from 'antd-style'
import { getFileContent } from '../../services/files'
import { getProjectRevisions, type ProjectRevision } from '../../services/repositories'
import { DiffInputTooLargeError, diffLines, type DiffLine } from '../../utils/lineDiff'

const useStyles = createStyles(({ css, token }) => ({
    controls: css`
        display: flex;
        gap: ${token.margin}px;
        margin-bottom: ${token.margin}px;
    `,
    diff: css`
        max-height: 60vh;
        overflow: auto;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        line-height: 1.5;
    `,
    row: css`
        display: flex;
        white-space: pre-wrap;
        word-break: break-word;
    `,
    gutter: css`
        flex: 0 0 3.5em;
        text-align: right;
        padding-right: ${token.paddingXS}px;
        color: ${token.colorTextQuaternary};
        user-select: none;
    `,
    text: css`
        flex: 1;
        padding-right: ${token.paddingSM}px;
    `,
    add: css`
        background: ${token.colorSuccessBg};
    `,
    remove: css`
        background: ${token.colorErrorBg};
    `,
}))

interface FileDiffModalProps {
    open: boolean
    projectId: string
    repositoryId: string
    projectName: string
    branch?: string | null
    path: string | null
    onClose: () => void
}

const SIGN: Record<DiffLine['kind'], string> = { context: ' ', add: '+', remove: '-' }
const OPTION_COMMENT_MAX_LENGTH = 120

const shortComment = (comment: string): string => {
    const firstLine = comment.split(/\r?\n/, 1)[0]?.trim() || '—'
    return firstLine.length > OPTION_COMMENT_MAX_LENGTH
        ? `${firstLine.slice(0, OPTION_COMMENT_MAX_LENGTH).trimEnd()}...`
        : firstLine
}

/**
 * Compare a text file between two of the project's revisions. Revisions load when the modal opens; the
 * two newest are preselected. Content for each side is fetched from the Files API at that revision and
 * shown as a unified line diff.
 */
export const FileDiffModal = ({
    open,
    projectId,
    repositoryId,
    projectName,
    branch,
    path,
    onClose,
}: FileDiffModalProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [revisions, setRevisions] = useState<ProjectRevision[] | 'error' | null>(null)
    const [from, setFrom] = useState<string | null>(null)
    const [to, setTo] = useState<string | null>(null)
    const [lines, setLines] = useState<DiffLine[] | 'loading' | 'too-large' | null>(null)

    useEffect(() => {
        if (!open || !path) {
            return
        }
        let cancelled = false
        setRevisions(null)
        setLines(null)
        getProjectRevisions(repositoryId, projectName, branch ?? null, { size: 100 })
            .then(page => {
                if (cancelled) {
                    return
                }
                setRevisions(page.content)
                setTo(page.content[0]?.revisionNo ?? null)
                setFrom(page.content[1]?.revisionNo ?? page.content[0]?.revisionNo ?? null)
            })
            .catch(() => {
                if (!cancelled) {
                    setRevisions('error')
                }
            })
        return () => {
            cancelled = true
        }
    }, [open, path, repositoryId, projectName, branch])

    useEffect(() => {
        if (!open || !path || !from || !to) {
            return
        }
        let cancelled = false
        setLines('loading')
        Promise.all([getFileContent(projectId, path, from), getFileContent(projectId, path, to)])
            .then(([oldText, newText]) => {
                if (!cancelled) {
                    setLines(diffLines(oldText, newText))
                }
            })
            .catch(error => {
                if (!cancelled) {
                    if (error instanceof DiffInputTooLargeError) {
                        setLines('too-large')
                        return
                    }
                    setLines(null)
                    setRevisions('error')
                }
            })
        return () => {
            cancelled = true
        }
    }, [open, path, projectId, from, to])

    const options = revisions === null || revisions === 'error'
        ? []
        : revisions.map(revision => ({
            value: revision.revisionNo,
            label: `${revision.shortRevisionNo} · ${shortComment(revision.fullComment)}`,
        }))

    const hasChanges = Array.isArray(lines) && lines.some(line => line.kind !== 'context')

    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={onClose}
            open={open}
            title={path ?? ''}
            width={860}
        >
            {revisions === null && <Skeleton active paragraph={{ rows: 6 }} />}
            {revisions === 'error' && (
                <Alert showIcon data-testid="file-diff-error" title={t('browser.files.compare_failed')} type="error" />
            )}
            {Array.isArray(revisions) && revisions.length < 2 && (
                <Empty data-testid="file-diff-no-history" description={t('browser.files.compare_no_history')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
            {Array.isArray(revisions) && revisions.length >= 2 && (
                <>
                    <Space className={styles.controls}>
                        <Select
                            data-testid="file-diff-from"
                            onChange={setFrom}
                            options={options}
                            style={{ minWidth: 320 }}
                            value={from}
                        />
                        <Select
                            data-testid="file-diff-to"
                            onChange={setTo}
                            options={options}
                            style={{ minWidth: 320 }}
                            value={to}
                        />
                    </Space>
                    {lines === 'loading' && <Skeleton active paragraph={{ rows: 6 }} />}
                    {lines === 'too-large' && (
                        <Alert
                            showIcon
                            data-testid="file-diff-too-large"
                            title={t('browser.files.compare_too_large')}
                            type="warning"
                        />
                    )}
                    {Array.isArray(lines) && !hasChanges && (
                        <Empty data-testid="file-diff-identical" description={t('browser.files.compare_identical')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    )}
                    {Array.isArray(lines) && hasChanges && (
                        <div className={styles.diff} data-testid="file-diff-body">
                            {lines.map((line, index) => (
                                <div key={index} className={cx(styles.row, line.kind === 'add' && styles.add, line.kind === 'remove' && styles.remove)}>
                                    <span className={styles.gutter}>{line.oldNumber ?? ''}</span>
                                    <span className={styles.gutter}>{line.newNumber ?? ''}</span>
                                    <span className={styles.text}>{SIGN[line.kind]} {line.text}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </>
            )}
        </Modal>
    )
}
