import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Dropdown, notification, Spin } from 'antd'
import { DownOutlined, LoadingOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import {
    getProjectBranches,
    isProjectModifiedConflict,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'
import { SearchInput } from '../../components/SearchInput'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { BranchLabel, type BranchTone } from './BranchLabel'

const useStyles = createStyles(({ css, token }) => ({
    trigger: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        /* Never wider than the place it is put in: the branch name truncates instead. */
        max-width: 100%;
        min-width: 0;
        padding: 0 4px;
        border: 0;
        border-radius: ${token.borderRadiusSM}px;
        background: transparent;
        color: inherit;
        font: inherit;
        cursor: pointer;

        &:hover {
            background: ${token.colorFillTertiary};
        }

        /* Blocked because the project is busy with something else — nothing to wait for here. */
        &:disabled {
            cursor: not-allowed;
        }

        /* The switch this trigger started is what the wait is for. */
        &[aria-busy='true'] {
            cursor: progress;
        }
    `,
    caret: css`
        color: ${token.colorTextQuaternary};
        font-size: 10px;
    `,
    /** The caret's place while the switch runs, so the trigger itself says the branch is changing. */
    busy: css`
        color: ${token.colorPrimary};
    `,
    /**
     * The popup reads as one card: a fixed width so a long branch name is clipped instead of stretching the
     * list across the screen, and the list below the search is the card's own body, not a second card
     * floating under it.
     */
    popup: css`
        min-width: 220px;
        max-width: 320px;
        background: ${token.colorBgElevated};
        border-radius: ${token.borderRadiusLG}px;
        box-shadow: ${token.boxShadowSecondary};
        overflow: hidden;

        /* The list is this card's own body, not a second card floating under the search. */
        .ant-dropdown-menu {
            box-shadow: none;
            background: transparent;
            border-radius: 0;
            padding: 4px 0;
            max-height: 320px;
            overflow-y: auto;
        }

        .ant-dropdown-menu-item {
            max-width: 100%;
        }

        /* A long branch name is clipped to the fixed width instead of stretching the popup. */
        .ant-dropdown-menu-title-content {
            min-width: 0;
            overflow: hidden;
        }
    `,
    search: css`
        padding: 8px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    empty: css`
        padding: 12px;
        color: ${token.colorTextTertiary};
        text-align: center;
    `,
}))

interface BranchSwitcherProps {
    projectId: string
    currentBranch: string
    /** Marks of the current branch, known from the project itself without listing the branches. */
    currentBranchProtected?: boolean | undefined
    currentBranchDefault?: boolean | undefined
    /**
     * The project moved to another branch and has to be read again. A promise is awaited, so the switch
     * counts as running until the screen shows the new branch's data.
     */
    onSwitched: () => void | Promise<unknown>
    /**
     * Whether the switch — the request and the reload behind it — is running, so the screen around it can
     * mark the project busy and block its other actions meanwhile.
     */
    onBusyChange?: ((busy: boolean) => void) | undefined
    /** Blocks the switch while the project is busy with another operation of its own. */
    disabled?: boolean | undefined
    /** Colour tone of the current branch — `secondary` to read like a breadcrumb link. */
    tone?: BranchTone | undefined
    'data-testid'?: string
}

/**
 * Switches the project to another of its branches, showing the Default and protected marks both for the
 * current branch and for every branch offered.
 *
 * The branch reads as plain text with a caret rather than as a form input, so it stays unobtrusive
 * wherever it is placed — the breadcrumb and the Overview tab render the very same control.
 *
 * The switch targets and their marks are fetched when the menu first opens. Simply showing the current
 * branch costs no request and does not carry every project membership in the projects response.
 *
 * A switch takes seconds on a slow environment, so the trigger says so for as long as it runs — through
 * the request and through the reload that follows it, since only then does the screen show the new branch.
 */
export const BranchSwitcher = ({
    projectId,
    currentBranch,
    currentBranchProtected,
    currentBranchDefault,
    onSwitched,
    onBusyChange,
    disabled = false,
    tone,
    'data-testid': testId = 'branch-switcher',
}: BranchSwitcherProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [branchInfo, setBranchInfo] = useState<ProjectBranch[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const [discardSwitchBranch, setDiscardSwitchBranch] = useState<string | null>(null)
    const [query, setQuery] = useState('')
    const projectIdRef = useRef(projectId)

    useEffect(() => {
        projectIdRef.current = projectId
        setBranchInfo(null)
        setLoading(false)
        setDiscardSwitchBranch(null)
        setQuery('')
    }, [projectId])

    const loadBranches = async () => {
        if (branchInfo !== null || loading) {
            return
        }
        const requestedProjectId = projectId
        setLoading(true)
        try {
            const loaded = await getProjectBranches(requestedProjectId)
            if (projectIdRef.current === requestedProjectId) {
                setBranchInfo(loaded)
            }
        } catch (e) {
            if (projectIdRef.current === requestedProjectId) {
                notification.error({ title: t('browser.branch.load_failed'), description: errorMessage(e) })
            }
        } finally {
            if (projectIdRef.current === requestedProjectId) {
                setLoading(false)
            }
        }
    }

    const switchTo = async (branch: string, discardChanges = false) => {
        if (branch === currentBranch) {
            return
        }
        setSwitching(true)
        onBusyChange?.(true)
        try {
            await switchProjectBranch(projectId, branch, discardChanges ? { discardChanges: true } : {})
            // The reload is part of the switch: until it lands the screen still shows the branch the user
            // switched away from, so the busy state has to outlive the request that started it.
            await onSwitched()
        } catch (e) {
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardSwitchBranch(branch)
                return
            }
            notification.error({ title: t('browser.branch.switch_failed'), description: errorMessage(e) })
        } finally {
            setSwitching(false)
            onBusyChange?.(false)
        }
    }

    // The current branch carries its marks from the project; the others only once the list is loaded.
    const marksOf = (branch: string) => {
        const loaded = branchInfo?.find(item => item.name === branch)
        if (loaded) {
            return { isDefault: loaded.base, isProtected: loaded.protected }
        }
        return branch === currentBranch
            ? { isDefault: currentBranchDefault, isProtected: currentBranchProtected }
            : {}
    }

    const branchLabel = (branch: string) => <BranchLabel name={branch} {...marksOf(branch)} />

    const current = <BranchLabel withIcon name={currentBranch} testId={testId} tone={tone} {...marksOf(currentBranch)} />

    const needle = query.trim().toLowerCase()
    const filteredBranches = needle
        ? (branchInfo ?? []).filter(branch => branch.name.toLowerCase().includes(needle))
        : branchInfo ?? []

    // What the popup shows below its search box: the branches once they are there, and while they are
    // still being read the reading itself — a heavy repository takes a moment to list them, and an empty
    // card would read as a project with no other branch to switch to.
    const branchListContent = (menu: ReactNode) => {
        if (branchInfo === null && loading) {
            return (
                <div className={styles.empty} data-testid={`${testId}-list-loading`}>
                    <Spin size="small" />
                </div>
            )
        }
        return needle && filteredBranches.length === 0
            ? <div className={styles.empty}>{t('browser.branch.no_match')}</div>
            : menu
    }

    const discardModal = (
        <DiscardChangesModal
            cancelButtonTestId={`${testId}-discard-switch-cancel`}
            confirmButtonTestId={`${testId}-discard-switch-confirm`}
            confirmText={t('browser.switch_branch_discard_confirm_unsafe')}
            onCancel={() => setDiscardSwitchBranch(null)}
            open={discardSwitchBranch !== null}
            warning={t('browser.switch_branch_discard_warning')}
            onConfirm={() => {
                const branch = discardSwitchBranch
                setDiscardSwitchBranch(null)
                if (branch) {
                    void switchTo(branch, true)
                }
            }}
        />
    )

    return (
        <>
            <Dropdown
                trigger={['click']}
                menu={{
                    items: filteredBranches.map(branch => ({
                        key: branch.name,
                        label: branchLabel(branch.name),
                    })),
                    selectedKeys: [currentBranch],
                    onClick: ({ key }) => void switchTo(key),
                }}
                onOpenChange={open => {
                    setQuery('')
                    if (open) {
                        void loadBranches()
                    }
                }}
                popupRender={menu => (
                    <div className={styles.popup}>
                        {/* Search sits at the top of the list: heavy repositories carry many branches, so the
                            list filters as the user types (client-side over the already-loaded branches). */}
                        <div className={styles.search}>
                            <SearchInput
                                autoFocus
                                data-testid={`${testId}-search`}
                                onChange={event => setQuery(event.target.value)}
                                placeholder={t('browser.branch.filter')}
                                value={query}
                            />
                        </div>
                        {branchListContent(menu)}
                    </div>
                )}
            >
                <button
                    aria-busy={switching}
                    className={styles.trigger}
                    data-testid={`${testId}-trigger`}
                    disabled={switching || disabled}
                    type="button"
                >
                    {current}
                    {switching
                        ? <LoadingOutlined spin className={cx(styles.caret, styles.busy)} data-testid={`${testId}-switching`} />
                        : <DownOutlined className={styles.caret} />}
                </button>
            </Dropdown>
            {discardModal}
        </>
    )
}
