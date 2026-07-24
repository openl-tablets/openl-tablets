import type { InputRef } from 'antd'
import { useTranslation } from 'react-i18next'
import { Segmented, Select } from 'antd'
import {
    AppstoreOutlined,
    ControlOutlined,
    UnorderedListOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { SearchInput } from '../../components/SearchInput'
import type { ProjectSort } from './projectListing'

export type ProjectView = 'list' | 'grid'

const useStyles = createStyles(({ css }) => ({
    toolbar: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
    `,
    search: css`
        flex: 1;
        min-width: 240px;
    `,
    sort: css`
        width: 180px;
        flex: none;
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

/**
 * Search and list/grid view controls above the projects list. The cards of the grid have no headers to
 * click, so only that view gets a sort dropdown — the list sorts by its column headers — and its options
 * are exactly those columns.
 */
export const ProjectsToolbar = ({ search, onSearch, sort, onSort, view, onView, searchRef }: ProjectsToolbarProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()

    return (
        <div className={styles.toolbar}>
            <SearchInput
                ref={searchRef}
                className={styles.search}
                data-testid="projects-search"
                onChange={event => onSearch(event.target.value)}
                placeholder={t('home.search_placeholder')}
                value={search}
            />
            {view === 'grid' && (
                <Select
                    className={styles.sort}
                    data-testid="projects-sort"
                    onChange={onSort}
                    prefix={<ControlOutlined />}
                    value={sort}
                    options={[
                        { value: 'name', label: t('home.col_project') },
                        { value: 'branch', label: t('home.col_branch') },
                        { value: 'updated', label: t('home.col_modified') },
                    ]}
                />
            )}
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
