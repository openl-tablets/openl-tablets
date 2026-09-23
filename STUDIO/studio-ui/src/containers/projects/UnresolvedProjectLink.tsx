import { Button, Empty, Space, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { toUrlSafeId } from '../../services/projectId'
import type { ProjectLinkProblem } from '../../services/projectLink'

interface UnresolvedProjectLinkProps {
    problem: ProjectLinkProblem
    /** What the link named the project by. */
    addressed: string
    /** The screen's address for a project id: where a chosen candidate opens. */
    routeOf: (id: string) => string
}

/**
 * What a project screen shows in place of the project when its link does not lead to one.
 *
 * A link that leads to no project the reader may see says so and leads back to the projects.
 *
 * A name that several projects carry lists them, each with where it is stored: the repository, and the folder that
 * tells apart projects of one name in one repository. The one picked opens on the same screen the link was for,
 * addressed by its id.
 */
export const UnresolvedProjectLink = ({ problem, addressed, routeOf }: UnresolvedProjectLinkProps) => {
    const { t } = useTranslation('repository')
    const navigate = useNavigate()
    const back = (
        <Button onClick={() => navigate('/projects')} type="primary">
            {t('home.back_to_projects')}
        </Button>
    )
    if (problem.kind === 'missing') {
        return (
            <Empty data-testid="project-workspace-missing" description={t('home.not_found')}>
                {back}
            </Empty>
        )
    }
    return (
        <Empty
            data-testid="project-link-ambiguous"
            description={(
                <Space orientation="vertical" size={0}>
                    <Typography.Title level={5}>{t('home.ambiguous_title', { name: addressed })}</Typography.Title>
                    <Typography.Text type="secondary">{t('home.ambiguous_hint')}</Typography.Text>
                </Space>
            )}
        >
            <Space orientation="vertical">
                {problem.candidates.map(candidate => {
                    const stored = [candidate.repositoryName, candidate.path].filter(Boolean).join(' · ')
                    return (
                        <Button
                            key={candidate.id}
                            block
                            data-testid="project-link-candidate"
                            onClick={() => navigate(routeOf(toUrlSafeId(candidate.id)))}
                        >
                            {candidate.name}
                            {stored && <Typography.Text type="secondary">{stored}</Typography.Text>}
                        </Button>
                    )
                })}
                {back}
            </Space>
        </Empty>
    )
}
