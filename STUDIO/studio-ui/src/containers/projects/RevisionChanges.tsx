import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Empty, Flex, Pagination, Skeleton } from 'antd'
import { getRevisionChanges, type RevisionChanges as Changes } from '../../services/repositories'
import type { ProjectFileChange } from '../../services/projectStatus'
import { ChangedFileList, type FileComparison } from './ChangedFileList'
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
}

/** The changes of a revision: undefined while they are read, null when they cannot be. */
export type LoadedChanges = Changes | null | undefined

/**
 * The files a revision changed, against the given base or else the revision it was made on, read once for the
 * revisions named.
 */
export const useRevisionChanges = (projectId: string, revision: string, base?: string): LoadedChanges => {
    const [changes, setChanges] = useState<LoadedChanges>(undefined)
    useEffect(() => {
        let cancelled = false
        setChanges(undefined)
        getRevisionChanges(projectId, revision, base)
            .then(answer => { if (!cancelled) setChanges(answer) })
            .catch(() => { if (!cancelled) setChanges(null) })
        return () => { cancelled = true }
    }, [projectId, revision, base])
    return changes
}

interface ChangedFilePagesProps {
    files: ProjectFileChange[]
    /** The page shown, from 1. */
    page: number
    onPage: (page: number) => void
    compare: FileComparison<ProjectFileChange>
    /** The path of the file compared last, which the list marks. */
    selected?: string | null | undefined
}

/** Changed files listed {@link CHANGED_FILES_PAGE_SIZE} to a page, with the pages to turn under them. */
export const ChangedFilePages = ({ files, page, onPage, compare, selected }: ChangedFilePagesProps) => {
    const start = (page - 1) * CHANGED_FILES_PAGE_SIZE
    return (
        <Flex vertical gap="small">
            <ChangedFileList
                compare={compare}
                files={files.slice(start, start + CHANGED_FILES_PAGE_SIZE)}
                selected={selected}
                testId="revision-changes"
                virtual={false}
            />
            <Pagination
                hideOnSinglePage
                align="end"
                current={page}
                onChange={onPage}
                pageSize={CHANGED_FILES_PAGE_SIZE}
                showSizeChanger={false}
                size="small"
                total={files.length}
            />
        </Flex>
    )
}

/**
 * The files of a project a revision changed, read once when it is first shown, a page at a time.
 *
 * <p>Every file offers Compare, which opens the comparison window with the file before and after the revision: a
 * workbook compared table by table, any other file line by line. A file the revision added is empty before it, and one
 * it deleted is empty after it, so all of it shows as added or removed. A revision that changed no file of the project
 * says so.
 */
export const RevisionChanges = ({ projectId, branch, revision }: RevisionChangesProps) => {
    const { t } = useTranslation('repository')
    const changes = useRevisionChanges(projectId, revision)
    const [page, setPage] = useState(1)

    if (changes === null) {
        return <Empty description={t('browser.history.changes_error')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    if (changes === undefined) {
        return <Skeleton active paragraph={{ rows: 2 }} title={false} />
    }
    if (changes.files.length === 0) {
        return <Empty description={t('browser.history.no_changes')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    const { base: before, revision: after, files } = changes
    return (
        <div data-testid={`revision-changes-${revision}`}>
            <ChangedFilePages
                files={files}
                onPage={setPage}
                page={page}
                // An added or a deleted file is compared too: the side that does not hold it is empty.
                compare={{
                    everyFile: true,
                    hint: t('browser.history.compare_hint'),
                    open: file => openFileRevisionsCompareWindow(projectId, file.path, branch ?? undefined,
                        before?.revisionNo ?? null, after.revisionNo, file.type),
                }}
            />
        </div>
    )
}
