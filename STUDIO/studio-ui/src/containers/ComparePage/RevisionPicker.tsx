import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Select, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { FieldRow } from 'components/FieldRow'
import { getProject, getProjectBranches, type ProjectBranch } from 'services/repositories'
import { BranchSelect } from 'containers/projects/BranchSelect'
import { useProjectRevisions } from 'containers/projects/revisions'
import { errorMessage } from 'utils/errorMessage'
import { useStyles } from './ComparePage.styles'

const LABEL_WIDTH = 110

/** The revision the working copy of a project is compared with, and the branch it is read on. */
export interface PickedRevision {
    /** The branch the revision is read on, or nothing in a repository without branches. */
    branch: string | undefined
    revision: string
}

interface RevisionPickerProps {
    projectId: string
    /** Told which revision to compare the working copy with, or null while none is picked. */
    onChange: (picked: PickedRevision | null) => void
}

/**
 * Picks the revision the working copy of a project is compared with: the branch first, then a revision of it. Each
 * answer decides what the next one can offer. The files changed between the two are listed once the comparison
 * starts, and the user picks the file to compare there.
 */
export const RevisionPicker: React.FC<RevisionPickerProps> = ({ projectId, onChange }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [error, setError] = useState<string | null>(null)
    const [projectName, setProjectName] = useState<string | null>(null)
    const [branches, setBranches] = useState<ProjectBranch[]>([])
    const [branch, setBranch] = useState<string | undefined>(undefined)
    const [revision, setRevision] = useState<string | undefined>(undefined)

    const { options: revisionOptions, revisions, error: historyError } =
        useProjectRevisions({ id: projectId }, true, undefined, branch)
    // A long history takes a while to read: the field says so until it is read.
    const loadingRevisions = revisions === null

    // The branch the project is on is the one its revisions are offered from.
    useEffect(() => {
        let cancelled = false
        getProject(projectId)
            .then(project => {
                if (!cancelled) {
                    setProjectName(project.name)
                    setBranch(project.branch ?? undefined)
                }
            })
            .catch((projectError: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(projectError) || t('failed'))
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, t])

    // A repository without branches answers with none, and the branch is then not asked for at all.
    useEffect(() => {
        let cancelled = false
        getProjectBranches(projectId)
            .then(loaded => {
                if (!cancelled) {
                    setBranches(loaded)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setBranches([])
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId])

    // The newest revision of the branch is the one offered first, as the old screen offered it.
    useEffect(() => {
        setRevision(current => (revisionOptions.some(option => option.value === current)
            ? current
            : revisionOptions[0]?.value))
    }, [revisionOptions])

    const branchMarks = useCallback((name: string) => {
        const info = branches.find(candidate => candidate.name === name)
        return { isDefault: info?.base, isProtected: info?.protected }
    }, [branches])

    useEffect(() => {
        onChange(revision ? { branch, revision } : null)
    }, [onChange, branch, revision])

    return (
        <>
            {projectName && (
                <div className={styles.pickerTitle} data-testid="compare-project-title">
                    {t('revisions_title', { name: projectName })}
                </div>
            )}
            {(error ?? historyError) && (
                <Alert showIcon data-testid="compare-picker-error" title={error ?? historyError} type="error" />
            )}
            <div className={styles.side} data-testid="compare-side-revision">
                {branches.length > 0 && (
                    <FieldRow label={t('branch')} labelWidth={LABEL_WIDTH}>
                        <BranchSelect
                            branchNames={branches.map(({ name }) => name)}
                            data-testid="compare-branch"
                            marksOf={branchMarks}
                            onChange={setBranch}
                            value={branch}
                        />
                    </FieldRow>
                )}
                <FieldRow label={t('revision')} labelWidth={LABEL_WIDTH}>
                    <Select
                        data-testid="compare-revision"
                        loading={loadingRevisions}
                        notFoundContent={loadingRevisions ? <Spin size="small" /> : undefined}
                        onChange={setRevision}
                        options={revisionOptions}
                        placeholder={loadingRevisions ? t('revisions_loading') : undefined}
                        popupMatchSelectWidth={false}
                        style={{ width: '100%' }}
                        value={revision}
                    />
                </FieldRow>
            </div>
        </>
    )
}

export default RevisionPicker
