import { Link } from 'react-router-dom'
import { Tag } from 'antd'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { BranchLabel } from './BranchLabel'
import { useSharedStyles } from './sharedStyles'
import type { ProjectDependency } from '../../types/projects'

const useStyles = createStyles(({ css, token }) => ({
    /** The rows step in from the heading they belong to, like every other list on the tab. */
    indent: css`
        margin-left: 18px;
    `,
    rowName: css`
        flex: 1;
        min-width: 0;
    `,
    rowLink: css`
        flex: 1;
        min-width: 0;
        color: ${token.colorLink};

        &:hover {
            color: ${token.colorLinkHover};
        }
    `,
    rowMeta: css`
        flex: none;
        font-size: 12px;
    `,
    /** A dependency the workspace cannot show is marked, so an empty row does not read as a broken link. */
    missingTag: css`
        flex: none;
        margin: 0;
        color: ${token.colorErrorText};
        background: ${token.colorErrorBg};
        border-color: ${token.colorErrorBorder};
    `,
    /**
     * A dependency another dependency brings in is not part of what this rules.xml declares, so it reads
     * as an aside rather than as an error.
     */
    transitiveTag: css`
        flex: none;
        margin: 0;
        color: ${token.colorTextTertiary};
        background: ${token.colorFillQuaternary};
        border-color: ${token.colorBorderSecondary};
    `,
}))

/**
 * The projects a project relates to — what it depends on, or what depends on it — one per line, each with
 * its branch and what is worth knowing about it.
 *
 * The single rendering of a dependency across the workspace: the Overview tab and the open dialog show
 * one the same way.
 */
export const DependencyList = ({ deps }: { deps: ProjectDependency[] }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <ul className={cx(shared.linedList, styles.indent)}>
            {deps.map(dep => (
                <li key={dep.id ?? dep.name} className={shared.linedItem}>
                    {/* rules.xml names a project the workspace does not have: it is shown as declared,
                        with nothing to open. */}
                    {dep.id
                        ? (
                            <Link className={cx(shared.valueText, shared.ellipsis, styles.rowLink)} to={`/projects/${encodeURIComponent(dep.id)}`}>
                                {dep.name}
                            </Link>
                        )
                        : <span className={cx(shared.valueText, shared.ellipsis, styles.rowName)}>{dep.name}</span>}
                    {dep.missing && (
                        <Tag className={styles.missingTag} data-testid={`dependency-missing-${dep.name}`}>
                            {t('browser.overview.dependency_missing')}
                        </Tag>
                    )}
                    {/* rules.xml declares the direct ones only; this one comes with another dependency. */}
                    {dep.transitive && (
                        <Tag className={styles.transitiveTag} data-testid={`dependency-transitive-${dep.name}`}>
                            {t('browser.overview.dependency_transitive')}
                        </Tag>
                    )}
                    {dep.branch && (
                        <BranchLabel
                            className={styles.rowMeta}
                            isDefault={dep.branchDefault}
                            isProtected={dep.branchProtected}
                            name={dep.branch}
                            testId={`dependency-branch-${dep.id}`}
                        />
                    )}
                </li>
            ))}
        </ul>
    )
}
