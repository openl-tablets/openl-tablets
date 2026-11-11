import { Button, Tooltip, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
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

/**
 * A value the projects carry that is not configured. Its label opens the projects that carry it, so a tag added by
 * mistake is found and removed there; the plus adds the value to the configuration.
 */
const FoundTagChip = ({ type, tag, onDefine, styles }: FoundProps & { type: string, tag: FoundTag }) => {
    const { t } = useTranslation()
    const add = t('tags:found_add', { type, value: tag.value })
    const open = t('tags:found_open', { count: tag.projects, type, value: tag.value })
    return (
        <span className={styles.found} data-testid={`found-tag-${type}-${tag.value}`}>
            <Tooltip title={open}>
                <Link aria-label={open} className={styles.foundLink} to={projectsWithTag(type, tag.value)}>
                    {tag.value}
                    <span className={styles.foundCount}>{tag.projects}</span>
                </Link>
            </Tooltip>
            <Tooltip title={add}>
                <Button
                    aria-label={add}
                    className={styles.foundAdd}
                    data-testid={`found-tag-add-${type}-${tag.value}`}
                    icon={<PlusOutlined />}
                    onClick={() => onDefine(type, [tag.value])}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </span>
    )
}

/**
 * The tags the projects carry that are not configured, by tag type: a tag type that is not configured with all its
 * values, and a configured one with the values it lacks. A value is added from its chip. A tag type that is not
 * configured is also added with all its values at once; a configured one has nothing to add but its values.
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
                    <span className={defined ? styles.foundType : `${styles.foundType} ${styles.missingType}`}>
                        {type}
                    </span>
                    <span className={styles.tagsCell}>
                        {values.map(tag => (
                            <FoundTagChip key={tag.value} onDefine={onDefine} styles={styles} tag={tag} type={type} />
                        ))}
                    </span>
                    {!defined && (
                        <Button
                            data-testid={`found-type-add-${type}`}
                            onClick={() => onDefine(type, values.map(tag => tag.value))}
                            size="small"
                        >
                            {t('tags:found_add_type')}
                        </Button>
                    )}
                </div>
            ))}
        </section>
    )
}
