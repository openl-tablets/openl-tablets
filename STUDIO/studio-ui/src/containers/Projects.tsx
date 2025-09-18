import React, { useState, useEffect, useCallback } from 'react'
import { Tree, Input, Button, Card, Space, Typography, Badge, Tooltip, Spin, Dropdown } from 'antd'
import { 
    BranchesOutlined,
    BookOutlined,
    StarOutlined, 
    StarFilled, 
    SearchOutlined, 
    FilterOutlined,
    InfoCircleOutlined,
    CheckCircleOutlined,
    HomeOutlined,
    EyeOutlined,
    EditOutlined,
    StopOutlined,
    FileTextOutlined
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { Project } from '../types/projects'
import { Repository } from '../types/repositories'
import './Projects.scss'

const { Search } = Input
const { Title, Text } = Typography

// File extensions for filtering
const FILE_EXTENSIONS = [
    '.csv',
    '.groovy', 
    '.json',
    '.md',
    '.properties',
    '.xlsx',
    '.xml'
]

interface ProjectWithFavorites extends Project {
    isFavorite?: boolean
    repositoryName?: string
}

interface RepositoryWithProjects extends Repository {
    projects: ProjectWithFavorites[]
    isExpanded?: boolean
}

const Projects: React.FC = () => {
    const { t } = useTranslation()
    const [repositories, setRepositories] = useState<RepositoryWithProjects[]>([])
    const [filteredRepositories, setFilteredRepositories] = useState<RepositoryWithProjects[]>([])
    const [favoriteProjects, setFavoriteProjects] = useState<Set<string>>(new Set())
    const [searchText, setSearchText] = useState('')
    const [loading, setLoading] = useState(true)
    const [expandedKeys, setExpandedKeys] = useState<string[]>([])
    const [selectedExtensions, setSelectedExtensions] = useState<Set<string>>(new Set())
    const [hasAutoExpandedPinned, setHasAutoExpandedPinned] = useState(false)

    // Load favorite projects from localStorage
    useEffect(() => {
        const savedFavorites = localStorage.getItem('favoriteProjects')
        if (savedFavorites) {
            setFavoriteProjects(new Set(JSON.parse(savedFavorites)))
        }
    }, [])

    // Save favorite projects to localStorage
    const saveFavorites = useCallback((favorites: Set<string>) => {
        localStorage.setItem('favoriteProjects', JSON.stringify([...favorites]))
    }, [])

    // Fetch repositories and projects
    const fetchRepositories = useCallback(async () => {
        try {
            setLoading(true)
            
            // Fetch repositories and projects in parallel
            const [repos, allProjects] = await Promise.all([
                apiCall('/repos') as Promise<Repository[]>,
                apiCall('/projects') as Promise<Project[]>
            ])
            
            // Map projects by repository
            const reposWithProjects = repos.map(repo => {
                const repoProjects = allProjects.filter(project => project.repository === repo.id)
                return {
                    ...repo,
                    projects: repoProjects.map(project => ({
                        ...project,
                        repositoryName: repo.name,
                        isFavorite: favoriteProjects.has(`${repo.name}:${project.name}`)
                    }))
                }
            })
            
            setRepositories(reposWithProjects)
            setFilteredRepositories(reposWithProjects)
        } catch (error) {
            console.error('Failed to fetch repositories and projects:', error)
        } finally {
            setLoading(false)
        }
    }, [favoriteProjects])

    useEffect(() => {
        fetchRepositories()
    }, [fetchRepositories])

    // Auto-expand pinned projects section if it exists (only once)
    useEffect(() => {
        const favoriteProjectsList = repositories
            .flatMap(repo => repo.projects)
            .filter(project => project.isFavorite)

        if (favoriteProjectsList.length > 0 && !hasAutoExpandedPinned && !expandedKeys.includes('pinned-projects')) {
            setExpandedKeys(prev => [...prev, 'pinned-projects'])
            setHasAutoExpandedPinned(true)
        }
    }, [repositories, hasAutoExpandedPinned, expandedKeys])

    // Filter repositories and projects based on search text and extensions
    useEffect(() => {
        let filtered = repositories

        // Apply search text filter (only to project names, not repository names)
        if (searchText.trim()) {
            filtered = filtered.map(repo => ({
                ...repo,
                projects: repo.projects.filter(project =>
                    project.name.toLowerCase().includes(searchText.toLowerCase())
                )
            })).filter(repo => repo.projects.length > 0)
        }

        // Apply extension filter (if any extensions are selected)
        // Note: This will be implemented when file filtering is available
        if (selectedExtensions.size > 0) {
            // For now, don't apply extension filters since file filtering is not implemented
            // filtered = filtered.map(repo => ({
            //     ...repo,
            //     projects: repo.projects.filter(project => {
            //         // This will filter files under projects when implemented
            //         return Array.from(selectedExtensions).some(ext => 
            //             project.name.toLowerCase().includes(ext.toLowerCase())
            //         )
            //     })
            // })).filter(repo => repo.projects.length > 0)
        }

        setFilteredRepositories(filtered)
    }, [searchText, repositories, selectedExtensions])

    // Toggle favorite status
    const toggleFavorite = useCallback((repoName: string, projectName: string) => {
        const projectKey = `${repoName}:${projectName}`
        const newFavorites = new Set(favoriteProjects)
        
        if (newFavorites.has(projectKey)) {
            newFavorites.delete(projectKey)
        } else {
            newFavorites.add(projectKey)
        }
        
        setFavoriteProjects(newFavorites)
        saveFavorites(newFavorites)
        
        // Update the repositories state
        setRepositories(prev => prev.map(repo => ({
            ...repo,
            projects: repo.projects.map(project => ({
                ...project,
                isFavorite: newFavorites.has(`${repo.name}:${project.name}`)
            }))
        })))
    }, [favoriteProjects, saveFavorites])

    // Handle extension filter toggle
    const toggleExtensionFilter = useCallback((extension: string) => {
        const newExtensions = new Set(selectedExtensions)
        if (newExtensions.has(extension)) {
            newExtensions.delete(extension)
        } else {
            newExtensions.add(extension)
        }
        setSelectedExtensions(newExtensions)
    }, [selectedExtensions])

    // Clear all extension filters
    const clearExtensionFilters = useCallback(() => {
        setSelectedExtensions(new Set())
    }, [])

    // Create dropdown menu items
    const createExtensionFilterMenu = () => {
        const menuItems = [
            {
                key: 'clear-filters',
                label: (
                    <div 
                        onClick={clearExtensionFilters}
                        style={{ 
                            padding: '4px 8px',
                            cursor: 'pointer',
                            borderBottom: '1px solid #f0f0f0',
                            marginBottom: '4px'
                        }}
                    >
                        Clear Filters
                    </div>
                )
            },
            ...FILE_EXTENSIONS.map(extension => ({
                key: extension,
                label: (
                    <div 
                        onClick={() => toggleExtensionFilter(extension)}
                        style={{ 
                            padding: '4px 8px',
                            cursor: 'pointer',
                            backgroundColor: selectedExtensions.has(extension) ? '#e6f7ff' : 'transparent',
                            border: selectedExtensions.has(extension) ? '1px solid #1890ff' : '1px solid transparent',
                            borderRadius: '4px',
                            margin: '2px 0'
                        }}
                    >
                        {extension}
                    </div>
                )
            }))
        ]
        return { items: menuItems }
    }

    // Get project status icon
    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'LOCAL':
                return <HomeOutlined style={{ color: '#722ed1' }} />
            case 'ARCHIVED':
                return <FileTextOutlined style={{ color: '#faad14' }} />
            case 'OPENED':
                return <CheckCircleOutlined style={{ color: '#52c41a' }} />
            case 'VIEWING_VERSION':
                return <EyeOutlined style={{ color: '#1890ff' }} />
            case 'EDITING':
                return <EditOutlined style={{ color: '#13c2c2' }} />
            case 'CLOSED':
                return <StopOutlined style={{ color: '#ff4d4f' }} />
            default:
                return <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
        }
    }

    // Build tree data
    const buildTreeData = () => {
        const favoriteProjectsList = repositories
            .flatMap(repo => repo.projects)
            .filter(project => project.isFavorite)

        const treeData = []

        // Add pinned projects section
        if (favoriteProjectsList.length > 0) {
            treeData.push({
                title: (
                    <div className="tree-section-header">
                        <StarFilled style={{ color: '#faad14' }} />
                        <span>Pinned Projects</span>
                        <Badge showZero count={favoriteProjectsList.length} />
                    </div>
                ),
                key: 'pinned-projects',
                children: favoriteProjectsList.map(project => ({
                    title: (
                        <div className="project-item">
                            <div className="project-left">
                                <BookOutlined style={{ color: '#1890ff' }} />
                                <Tooltip placement="top" title={project.name}>
                                    <span className="project-name">{project.name}</span>
                                </Tooltip>
                                <Tooltip placement="top" title={project.repositoryName}>
                                    <span className="repository-label">{project.repositoryName}</span>
                                </Tooltip>
                            </div>
                            <div className="project-right">
                                {getStatusIcon(project.status)}
                                <Button
                                    icon={<StarFilled style={{ color: '#faad14' }} />}
                                    size="small"
                                    type="text"
                                    onClick={(e) => {
                                        e.stopPropagation()
                                        toggleFavorite(project.repositoryName!, project.name)
                                    }}
                                />
                            </div>
                        </div>
                    ),
                    key: `pinned-${project.repositoryName}-${project.name}`,
                    isLeaf: true
                }))
            })
        }

        // Add repositories directly without "All Projects" wrapper
        filteredRepositories.forEach(repo => {
            if (repo.projects.length > 0) {
                // Find the original repository to get total project count
                const originalRepo = repositories.find(r => r.name === repo.name)
                const totalProjects = originalRepo ? originalRepo.projects.length : repo.projects.length
                const visibleProjects = repo.projects.length
                
                // Show [visible/total] format when search is active
                const isFiltered = searchText.trim()
                const badgeCount = isFiltered ? `${visibleProjects}/${totalProjects}` : visibleProjects
                
                treeData.push({
                    title: (
                        <div className="repository-item">
                            <BranchesOutlined style={{ color: '#722ed1' }} />
                            <span className="repository-name">{repo.name}</span>
                            <Badge showZero count={badgeCount} />
                        </div>
                    ),
                    key: `repo-${repo.name}`,
                    children: repo.projects.map(project => ({
                        title: (
                            <div className="project-item">
                                <div className="project-left">
                                    <BookOutlined style={{ color: '#1890ff' }} />
                                    <Tooltip placement="top" title={project.name}>
                                        <span className="project-name">{project.name}</span>
                                    </Tooltip>
                                </div>
                                <div className="project-right">
                                    {getStatusIcon(project.status)}
                                    <Button
                                        size="small"
                                        type="text"
                                        icon={project.isFavorite ? 
                                            <StarFilled style={{ color: '#faad14' }} /> : 
                                            <StarOutlined />}
                                        onClick={(e) => {
                                            e.stopPropagation()
                                            toggleFavorite(repo.name, project.name)
                                        }}
                                    />
                                </div>
                            </div>
                        ),
                        key: `${repo.name}-${project.name}`,
                        isLeaf: true
                    }))
                })
            }
        })

        return treeData
    }

    if (loading) {
        return (
            <div className="projects-container">
                <div className="projects-loading">
                    <Spin size="large" />
                    <Text>Loading projects...</Text>
                </div>
            </div>
        )
    }

    return (
        <div className="projects-container">
            <div className="projects-sidebar">
                <div className="projects-header">
                    <Space>
                        <Search
                            allowClear
                            onChange={(e) => setSearchText(e.target.value)}
                            placeholder="Search projects..."
                            prefix={<SearchOutlined />}
                            style={{ width: 300 }}
                            value={searchText}
                        />
                        <Dropdown 
                            menu={createExtensionFilterMenu()} 
                            placement="bottomRight"
                            trigger={['click']}
                        >
                            <Button 
                                icon={<FilterOutlined />} 
                                type="text"
                                style={{ 
                                    color: selectedExtensions.size > 0 ? '#1890ff' : undefined 
                                }}
                            >
                                {selectedExtensions.size > 0 && (
                                    <Badge 
                                        count={selectedExtensions.size} 
                                        style={{ 
                                            backgroundColor: '#f0f0f0',
                                            color: '#8c8c8c',
                                            border: 'none'
                                        }} 
                                    />
                                )}
                            </Button>
                        </Dropdown>
                    </Space>
                </div>
                <div className="projects-tree-container">
                    <Tree
                        defaultExpandAll
                        showIcon
                        className="projects-tree"
                        expandedKeys={expandedKeys}
                        onExpand={(keys) => setExpandedKeys(keys as string[])}
                        treeData={buildTreeData()}
                    />
                </div>
            </div>
            <div className="projects-main-content">
                <div className="welcome-section">
                    <div className="welcome-icon">
                        <div className="logo-icon">
                            <div className="logo-inner" />
                        </div>
                    </div>
                    <Title className="welcome-title" level={1}>OpenL Studio</Title>
                    <Text className="welcome-subtitle">Empowering businesses with intelligent rule management</Text>
                    <div className="welcome-actions">
                        <Button icon={<span>+</span>} size="large" type="primary">
                            Create New Project
                        </Button>
                        <Button icon={<span>🕐</span>} size="large">
                            Recent Projects
                        </Button>
                    </div>
                    <div className="recent-projects-section">
                        <div className="recent-projects-header">
                            <span>Recent Projects</span>
                            <Badge showZero count={0} />
                        </div>
                        <div className="no-projects-found">
                            <div className="no-projects-icon">
                                <span>&lt; /&gt;</span>
                            </div>
                            <Text className="no-projects-text">No recent projects found</Text>
                            <Button icon={<span>+</span>} type="default">
                                Start Your First Project
                            </Button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default Projects

