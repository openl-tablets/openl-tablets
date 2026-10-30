import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Empty, Flex, Pagination, Skeleton } from 'antd'
import { getRevisionChanges, type RevisionChanges as Changes } from '../../services/repositories'
import { ChangedFileList } from './ChangedFileList'
import { openFileRevisionsCompareWindow } from './compare'

/** How many changed files a page shows. */
export const CHANGED_FILES_PAGE_SIZE = 10

interface RevisionChangesProps {
    /** URL-safe project id — what the changes are asked about and what the comparison opens. */
    projectId: string
    /** The branch holding the revisions, or nothing in a repository without branches. */
    branch?: string | null | undefined
    /** The revision whose changes are shown. */
    revision: string
    /** The earlier revision to compare it with, or nothing for the revision it was made on. */
    base?: string | undefined
}

/**
 * The files of a project a revision changed, read once when it is first shown, a page at a time.
 *
 * <p>Every file offers Compare, which opens the comparison window with the file before and after the revision: a
 * workbook compared table by table, any other file line by line. A file the revision added is empty before it, and one
 * it deleted is empty after it, so all of it shows as added or removed. A revision that changed no file of the project
 * says so.
 */
export const RevisionChanges = ({ projectId, branch, revision, base }: RevisionChangesProps) => {
    const { t } = useTranslation('repository')
    const [changes, setChanges] = useState<Changes | null>(null)
    const [failed, setFailed] = useState(false)
    const [page, setPage] = useState(1)

    useEffect(() => {
        let cancelled = false
        setChanges(null)
        setFailed(false)
        setPage(1)
        getRevisionChanges(projectId, revision, base)
            .then(answer => { if (!cancelled) setChanges(answer) })
            .catch(() => { if (!cancelled) setFailed(true) })
        return () => { cancelled = true }
    }, [projectId, revision, base])

    if (failed) {
        return <Empty description={t('browser.history.changes_error')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    if (!changes) {
        return <Skeleton active paragraph={{ rows: 2 }} title={false} />
    }
    if (changes.files.length === 0) {
        return <Empty description={t('browser.history.no_changes')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    const { base: before, revision: after, files } = changes
    const start = (page - 1) * CHANGED_FILES_PAGE_SIZE
    return (
        <Flex vertical data-testid={`revision-changes-${revision}`} gap="small">
            <ChangedFileList
                files={files.slice(start, start + CHANGED_FILES_PAGE_SIZE)}
                testId="revision-changes"
                virtual={false}
                // An added or a deleted file is compared too: the side that does not hold it is empty.
                compare={{
                    everyFile: true,
                    hint: t('browser.history.compare_hint'),
                    open: file => openFileRevisionsCompareWindow(projectId, file.path, branch ?? undefined,
                        before?.revisionNo ?? null, after.revisionNo,
                        file.type),
                }}
            />
            <Pagination
                hideOnSinglePage
                align="end"
                current={page}
                onChange={setPage}
                pageSize={CHANGED_FILES_PAGE_SIZE}
                showSizeChanger={false}
                size="small"
                total={files.length}
            />
        </Flex>
    )
}
