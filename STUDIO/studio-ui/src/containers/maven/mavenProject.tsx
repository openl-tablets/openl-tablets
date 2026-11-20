import { Tag, Tooltip } from 'antd'
import { LockOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { mavenArtifactOf, projectCoordinatesOf } from '../../services/projectId'

/**
 * What follows the name of a project that can only be read: the version of a project downloaded from the Maven
 * repository, as several versions of a project may be required, and the mark that it is read-only, with why on hover.
 */
export const ReadOnlyTag = ({ projectId }: { projectId: string }) => {
    const { t } = useTranslation('maven')
    const version = projectCoordinatesOf(mavenArtifactOf(projectId))?.version
    return (
        <>
            {version && <Tag data-testid="maven-version" variant="filled">{version}</Tag>}
            <Tooltip title={t('view.read_only_hint')}>
                <Tag data-testid="maven-read-only" icon={<LockOutlined />}>{t('view.read_only')}</Tag>
            </Tooltip>
        </>
    )
}
