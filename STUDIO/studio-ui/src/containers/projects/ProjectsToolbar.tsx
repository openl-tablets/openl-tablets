import type { InputRef } from 'antd'
import { useTranslation } from 'react-i18next'
import { Input, Segmented, Select } from 'antd'
import { AppstoreOutlined, ControlOutlined, SearchOutlined, UnorderedListOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { MOCKUP } from './projectsTheme'

export type ProjectSort = 'updated' | 'name' | 'status'
export type ProjectView = 'list' | 'grid'

const useStyles = createStyles(({ css, token }) => ({
    toolbar: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
    `,
    search: css`
        flex: 1;
        min-width: 240px;

        .ant-input-prefix .anticon {
            color: ${token.colorTextQuaternary};
        }
    `,
    sort: css`
        width: 180px;
        flex: none;
    `,
    hint: css`
        padding: 0 5px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        color: ${token.colorTextQuaternary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        line-height: 16px;
    `,
}))

interface ProjectsToolbarProps {
    search: string
    onSearch: (value: string) => void
    sort: ProjectSort
    onSort: (value: ProjectSort) => void
    view: ProjectView
    onView: (value: ProjectView) => void
    searchRef?: React.Ref<InputRef>
}

/** Search, sort and list/grid view controls above the projects list. */
export const ProjectsToolbar = ({ search, onSearch, sort, onSort, view, onView, searchRef }: ProjectsToolbarProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()

    return (
        <div className={styles.toolbar}>
            <Input
                ref={searchRef}
                allowClear
                className={styles.search}
                data-testid="projects-search"
                onChange={event => onSearch(event.target.value)}
                placeholder={t('home.search_placeholder')}
                prefix={<SearchOutlined />}
                suffix={<span aria-hidden className={styles.hint}>/</span>}
                value={search}
            />
            <Select
                className={styles.sort}
                data-testid="projects-sort"
                onChange={onSort}
                prefix={<ControlOutlined />}
                value={sort}
                options={[
                    { value: 'updated', label: t('home.sort_updated') },
                    { value: 'name', label: t('home.sort_name') },
                    { value: 'status', label: t('home.sort_status') },
                ]}
            />
            <Segmented<ProjectView>
                data-testid="projects-view"
                onChange={onView}
                value={view}
                options={[
                    { value: 'list', icon: <UnorderedListOutlined />, title: t('home.view_list') },
                    { value: 'grid', icon: <AppstoreOutlined />, title: t('home.view_grid') },
                ]}
            />
        </div>
    )
}
