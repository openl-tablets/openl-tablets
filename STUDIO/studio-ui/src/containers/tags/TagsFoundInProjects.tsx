import type { ReactNode } from 'react'
import { Tooltip, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { IconAction } from 'components/IconAction'
import { joinClasses } from 'utils/joinClasses'
import type { useStyles } from './TagTable.styles'
import { projectsWithTag, type FoundTag, type FoundTagType } from './tagsInProjects'

type TagStyles = ReturnType<typeof useStyles>['styles']

/** Defines tags found in projects: the tag type, created when it does not exist, and the values to add to it. */
export type DefineTags = (type: string, values: string[]) => void

/** What every part drawing found tags takes: how to define them, and the styles the page read once for all. */
interface FoundProps {
    onDefine: DefineTags
    styles: TagStyles
}

interface FoundChipProps {
    /** The look of the chip: a value, or a tag type. */
    className: string
    /** The tooltip and the accessible name of the plus. */
    add: string
    onAdd: () => void
    styles: TagStyles
    testId: string
    addTestId: string
    children: ReactNode
}

/** Something the projects carry that is not configured: what it names, and a plus that adds it to the configuration. */
const FoundChip = ({ className, add, onAdd, styles, testId, addTestId, children }: FoundChipProps) => (
    <span className={className} data-testid={testId}>
        {children}
        <IconAction
            className={styles.foundAdd}
            data-testid={addTestId}
            icon={<PlusOutlined />}
            onClick={onAdd}
            size="small"
            title={add}
        />
    </span>
)

/**
 * A value the projects carry that is not configured. Its label opens the projects that carry it, so a tag added by
 * mistake is found and removed there; the plus adds the value to the configuration.
 */
const FoundTagChip = ({ type, tag, onDefine, styles }: FoundProps & { type: string, tag: FoundTag }) => {
    const { t } = useTranslation()
    const open = t('tags:found_open', { count: tag.projects, type, value: tag.value })
    return (
        <FoundChip
            add={t('tags:found_add', { type, value: tag.value })}
            addTestId={`found-tag-add-${type}-${tag.value}`}
            className={styles.found}
            onAdd={() => onDefine(type, [tag.value])}
            styles={styles}
            testId={`found-tag-${type}-${tag.value}`}
        >
            <Tooltip title={open}>
                <Link aria-label={open} className={styles.foundLink} to={projectsWithTag(type, tag.value)}>
                    {tag.value}
                    <span className={styles.foundCount}>{tag.projects}</span>
                </Link>
            </Tooltip>
        </FoundChip>
    )
}

/**
 * A tag type the projects carry that is not configured, marked as its values are. The plus adds it with all the
 * values the projects carry for it.
 */
const FoundTypeChip = ({ type, values, onDefine, styles }: FoundProps & Omit<FoundTagType, 'defined'>) => {
    const { t } = useTranslation()
    return (
        <FoundChip
            add={t('tags:found_add_type', { type })}
            addTestId={`found-type-add-${type}`}
            className={joinClasses(styles.found, styles.foundTypeChip)}
            onAdd={() => onDefine(type, values.map(tag => tag.value))}
            styles={styles}
            testId={`found-type-name-${type}`}
        >
            <span className={joinClasses(styles.foundType, styles.missingType)}>{type}</span>
        </FoundChip>
    )
}

/**
 * The tags the projects carry that are not configured, by tag type: a tag type that is not configured with all its
 * values, and a configured one with the values it lacks. A value is added from its chip. A tag type that is not
 * configured is a chip too, which adds it with all its values at once; a configured one has nothing to add but its
 * values.
 */
export const TagTypesFoundInProjects = ({ types, onDefine, styles }: FoundProps & { types: FoundTagType[] }) => {
    const { t } = useTranslation()
    if (types.length === 0) {
        return null
    }
    return (
        <section className={styles.foundSection} data-testid="tag-types-found">
            <Typography.Title level={5}>{t('tags:found_types_title')}</Typography.Title>
            <Typography.Paragraph type="secondary">{t('tags:found_types_description')}</Typography.Paragraph>
            {types.map(({ type, defined, values }) => (
                <div key={type} className={styles.foundRow} data-testid={`found-type-${type}`}>
                    {defined ? (
                        <span className={styles.foundType}>{type}</span>
                    ) : (
                        <FoundTypeChip onDefine={onDefine} styles={styles} type={type} values={values} />
                    )}
                    <span className={styles.tagsCell}>
                        {values.map(tag => (
                            <FoundTagChip key={tag.value} onDefine={onDefine} styles={styles} tag={tag} type={type} />
                        ))}
                    </span>
                </div>
            ))}
        </section>
    )
}
