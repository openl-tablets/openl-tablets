import { Link } from 'react-router'
import { WarningOutlined } from '@ant-design/icons'
import { Tag, Tooltip } from 'antd'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { BranchLabel } from './BranchLabel'
import { useSharedStyles } from './sharedStyles'
import type { ProjectDependency } from '../../types/projects'
import { mavenProjectPage } from '../../services/mavenDependencies'
import { projectCoordinatesOf } from '../../services/projectId'

const useStyles = createStyles(({ css, token }) => ({
    /** The rows step in from the heading they belong to, like every other list on the tab. */
    indent: css`
        margin-left: 18px;
    `,
    /** The name keeps room for a few letters however many marks follow it, and is cut short past that. */
    rowName: css`
        flex: 1;
        min-width: 5em;
    `,
    rowLink: css`
        flex: 1;
        min-width: 5em;
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
    /** The project compiles with its Maven archive: a fact about where it comes from, not an error. */
    mavenTag: css`
        flex: none;
        margin: 0;
        color: ${token.colorInfoText};
        background: ${token.colorInfoBg};
        border-color: ${token.colorInfoBorder};
    `,
    /** Another version of the project is asked for and left out: worth a look, not an error of its own. */
    conflictTag: css`
        flex: none;
        margin: 0;
        color: ${token.colorWarningText};
        background: ${token.colorWarningBg};
        border-color: ${token.colorWarningBorder};
    `,
    /** Coordinates the dependency names while the open project is used instead. */
    mavenDeclared: css`
        flex: none;
        font-size: 12px;
        color: ${token.colorTextTertiary};
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
                    {/* The project compiles with the archive of the Maven repository, which opens read-only. */}
                    {dep.maven && <MavenSource coordinates={dep.mavenArtifact} name={dep.name} styles={styles} />}
                    {!dep.maven && dep.mavenArtifact && (
                        <Tooltip title={t('browser.overview.dependency_maven_declared')}>
                            <span
                                className={styles.mavenDeclared}
                                data-testid={`dependency-maven-declared-${dep.name}`}
                            >
                                {dep.mavenArtifact}
                            </span>
                        </Tooltip>
                    )}
                    {/* One project of a name is compiled: the other versions asked for are left out. */}
                    {dep.conflicts && dep.conflicts.length > 0 && (
                        <Tooltip
                            title={dep.conflicts.map(conflict => (
                                <div key={`${conflict.requestedBy} ${conflict.mavenArtifact ?? ''}`}>
                                    {t('browser.overview.dependency_conflict_item', {
                                        coordinates: conflict.mavenArtifact ?? dep.name,
                                        project: conflict.requestedBy,
                                    })}
                                </div>
                            ))}
                        >
                            <Tag
                                className={styles.conflictTag}
                                data-testid={`dependency-conflict-${dep.name}`}
                                icon={<WarningOutlined />}
                            >
                                {t('browser.overview.dependency_conflict')}
                            </Tag>
                        </Tooltip>
                    )}
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

/** Where a project compiled from its Maven archive comes from, with a link to see the archive read-only. */
const MavenSource = ({ coordinates, name, styles }: {
    coordinates: string | undefined
    name: string
    styles: ReturnType<typeof useStyles>['styles']
}) => {
    const { t } = useTranslation('repository')
    const parsed = projectCoordinatesOf(coordinates)
    const tag = (
        <Tag className={styles.mavenTag} data-testid={`dependency-maven-${name}`}>
            {t('browser.overview.dependency_maven')}
        </Tag>
    )
    return parsed
        ? (
            <Tooltip title={t('browser.overview.dependency_maven_open', { coordinates })}>
                <Link data-testid={`dependency-maven-link-${name}`} to={mavenProjectPage(parsed)}>{tag}</Link>
            </Tooltip>
        )
        : tag
}

/** The libraries a project names by Maven coordinates, one per line. */
export const LibraryList = ({ libraries }: { libraries: string[] }) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <ul className={cx(shared.linedList, styles.indent)}>
            {libraries.map(library => (
                <li key={library} className={shared.linedItem} data-testid={`library-${library}`}>
                    <span className={cx(shared.valueText, shared.ellipsis, styles.rowName)}>{library}</span>
                </li>
            ))}
        </ul>
    )
}
