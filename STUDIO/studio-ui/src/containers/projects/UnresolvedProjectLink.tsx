import { Button, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'

/**
 * What a project screen shows in place of the project when its link leads to no project the reader may see: it says
 * so and leads back to the projects.
 */
export const UnresolvedProjectLink = () => {
    const { t } = useTranslation('repository')
    const navigate = useNavigate()
    return (
        <Empty data-testid="project-workspace-missing" description={t('home.not_found')}>
            <Button onClick={() => navigate('/projects')} type="primary">
                {t('home.back_to_projects')}
            </Button>
        </Empty>
    )
}
