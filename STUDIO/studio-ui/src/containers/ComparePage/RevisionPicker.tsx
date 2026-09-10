import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Select } from 'antd'
import { useTranslation } from 'react-i18next'
import { FieldRow } from 'components/FieldRow'
import { getProjectCompareFiles, type ProjectFileSide } from 'services/compare'
import { getProject, getProjectBranches, type ProjectBranch } from 'services/repositories'
import { BranchSelect } from 'containers/projects/BranchSelect'
import { useProjectRevisions } from 'containers/projects/revisions'
import { errorMessage } from 'utils/errorMessage'
import { useStyles } from './ComparePage.styles'

const LABEL_WIDTH = 110

/** The two files a comparison of a project is started for. */
export interface ProjectComparisonSides {
    first: ProjectFileSide
    second: ProjectFileSide
}

interface RevisionPickerProps {
    projectId: string
    /** Told which two files to compare, or null while either side has none. */
    onChange: (sides: ProjectComparisonSides | null) => void
}

/** The dropdown options for a list of file paths. */
const fileOptions = (paths: string[]) => paths.map(path => ({ value: path, label: path }))

/**
 * Picks the two files to compare: one from the working copy, one from a revision the repository holds.
 *
 * The working copy is the project as its own user has it now, so it needs nothing but a file. The other
 * side is read from a revision, on the branch that revision belongs to, so it asks for the branch, the
 * revision and the file, in that order - each answer decides what the next one can offer.
 */
export const RevisionPicker: React.FC<RevisionPickerProps> = ({ projectId, onChange }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [error, setError] = useState<string | null>(null)
    const [projectName, setProjectName] = useState<string | null>(null)
    const [branches, setBranches] = useState<ProjectBranch[]>([])
    const [branch, setBranch] = useState<string | undefined>(undefined)
    const [revision, setRevision] = useState<string | undefined>(undefined)
    const [workingFiles, setWorkingFiles] = useState<string[] | null>(null)
    const [revisionFiles, setRevisionFiles] = useState<string[] | null>(null)
    const [workingFile, setWorkingFile] = useState<string | undefined>(undefined)
    const [revisionFile, setRevisionFile] = useState<string | undefined>(undefined)

    const { options: revisionOptions, revisions } = useProjectRevisions({ id: projectId }, true, undefined, branch)

    // The branch the project is on is the one its revisions are offered from, as the files beside them are.
    useEffect(() => {
        let cancelled = false
        getProject(projectId)
            .then(project => {
                if (!cancelled) {
                    setProjectName(project.name)
                    setBranch(project.branch ?? undefined)
                }
            })
            .catch((failure: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(failure) || t('failed'))
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

    const readFiles = useCallback((where: { branch?: string | undefined; revision?: string | undefined })
    : Promise<string[]> => {
        // What failed last time belongs to the files read last time, so it goes before these are read.
        setError(null)
        return getProjectCompareFiles(projectId, where).catch((failure: unknown) => {
            setError(errorMessage(failure) || t('failed'))
            return []
        })
    }, [projectId, t])

    useEffect(() => {
        let cancelled = false
        void readFiles({}).then(files => {
            if (!cancelled) {
                setWorkingFiles(files)
                setWorkingFile(current => (current && files.includes(current) ? current : files[0]))
            }
        })
        return () => {
            cancelled = true
        }
    }, [readFiles])

    // The newest revision of the branch is the one offered first, as the old screen offered it.
    useEffect(() => {
        setRevision(current => (revisionOptions.some(option => option.value === current)
            ? current
            : revisionOptions[0]?.value))
    }, [revisionOptions])

    // A revision holds files of its own: one it no longer has cannot stay picked.
    useEffect(() => {
        if (!revision) {
            setRevisionFiles(null)
            return undefined
        }
        let cancelled = false
        void readFiles({ branch, revision }).then(files => {
            if (!cancelled) {
                setRevisionFiles(files)
            }
        })
        return () => {
            cancelled = true
        }
    }, [readFiles, branch, revision])

    // The same file of two revisions is what is usually compared, so the revision side follows the
    // working copy. The two sides are read at once and either of them can answer first, so the revision
    // waits for the working copy rather than picking a file it would then have to take back.
    useEffect(() => {
        if (revisionFiles && workingFiles) {
            setRevisionFile(current => pickSameFile(revisionFiles, current, workingFile))
        }
    }, [revisionFiles, workingFiles, workingFile])

    const branchMarks = useCallback((name: string) => {
        const info = branches.find(candidate => candidate.name === name)
        return { isDefault: info?.base, isProtected: info?.protected }
    }, [branches])

    useEffect(() => {
        onChange(workingFile && revisionFile
            ? { first: { path: workingFile }, second: { path: revisionFile, branch, revision } }
            : null)
    }, [onChange, workingFile, revisionFile, branch, revision])

    return (
        <>
            {projectName && (
                <div className={styles.pickerTitle} data-testid="compare-project-title">
                    {t('revisions_title', { name: projectName })}
                </div>
            )}
            {error && <Alert showIcon data-testid="compare-picker-error" message={error} type="error" />}
            <div className={styles.sides}>
                <div className={styles.side} data-testid="compare-side-working">
                    <FieldRow label={t('revision')} labelWidth={LABEL_WIDTH}>
                        <span data-testid="compare-working-revision">{t('user_workspace')}</span>
                    </FieldRow>
                    <FieldRow label={t('select_file')} labelWidth={LABEL_WIDTH}>
                        <Select
                            data-testid="compare-working-file"
                            loading={workingFiles === null}
                            onChange={setWorkingFile}
                            options={fileOptions(workingFiles ?? [])}
                            placeholder={t('no_excel_files')}
                            popupMatchSelectWidth={false}
                            style={{ width: '100%' }}
                            value={workingFile}
                        />
                    </FieldRow>
                </div>
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
                            loading={revisions === null}
                            onChange={setRevision}
                            options={revisionOptions}
                            popupMatchSelectWidth={false}
                            style={{ width: '100%' }}
                            value={revision}
                        />
                    </FieldRow>
                    <FieldRow label={t('select_file')} labelWidth={LABEL_WIDTH}>
                        <Select
                            data-testid="compare-revision-file"
                            loading={revisionFiles === null}
                            onChange={setRevisionFile}
                            options={fileOptions(revisionFiles ?? [])}
                            placeholder={t('no_excel_files')}
                            popupMatchSelectWidth={false}
                            style={{ width: '100%' }}
                            value={revisionFile}
                        />
                    </FieldRow>
                </div>
            </div>
        </>
    )
}

/**
 * The file a side keeps: the one already picked while the revision still holds it, else the file picked
 * on the other side, because the same file of two revisions is what is usually compared.
 */
const pickSameFile = (files: string[], picked?: string, other?: string): string | undefined => {
    if (picked && files.includes(picked)) {
        return picked
    }
    return other && files.includes(other) ? other : files[0]
}

export default RevisionPicker
