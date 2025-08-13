import React, { useCallback, useEffect, useMemo, useState, useRef } from 'react'
import { Button, Divider, Flex, Spin, Tree, Typography, Switch, Checkbox, Table, Collapse, Dropdown, Menu, Badge } from 'antd'
import { TableOutlined, StarOutlined, StarFilled, MoreOutlined } from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { apiCall } from '../services'
import { useUserStore } from 'store'

interface TraceNodeApi {
    key: number
    title: string
    tooltip?: string
    lazy?: boolean
    extraClasses?: string
}

interface TraceDetails {
    inputParameters?: any
    resultParameters?: any
    spreadsheetSteps?: Array<{
        step: string
        description: string
        value: string
    }>
}

interface FavoriteNode {
    key: number
    path: number[]  // Array of parent keys leading to this node
    title?: string  // Cached title for quick access
}

// Mock details data - keeping this for now since we don't have the details API endpoint
const mockDetailsData: Record<number, TraceDetails> = {
    1: {
        inputParameters: {
            "Bank Information": {
                bankFullName: "Commerzbank AG",
                bankID: "commerz",
                bankRatings: "Collection of Rating Data",
                country: "Germany",
                sector: "Financial Services"
            },
            "Financial Data": {
                totalAssets: 500000000000,
                totalLiabilities: 450000000000,
                netIncome: 2500000000,
                capitalRatio: 0.15
            }
        },
        resultParameters: {
            "Rating Results": {
                overallRating: "A+",
                riskScore: 85,
                confidenceLevel: 0.92,
                lastUpdated: "2024-01-15"
            },
            "Calculation Details": {
                methodology: "Basel III Compliant",
                calculationDate: "2024-01-15T10:30:00Z",
                version: "2.1.0"
            }
        },
        spreadsheetSteps: [
            {
                step: "CheckCurrentFinancialData",
                description: "= SetNonZeroValues(currentFinancialData)",
                value: "currentFinancialData"
            },
            {
                step: "CheckPreviousPeriodFinancialData",
                description: "= SetNonZeroValues(previousFinancialData)",
                value: "previousFinancialData"
            },
            {
                step: "BalanceQualityIndexCalculation",
                description: "= BalanceQualityIndexCalculation(currentFinancialData)",
                value: "BalanceQualityIndex"
            },
            {
                step: "CapitalAdequacyRatio",
                description: "= CalculateCapitalAdequacyRatio(regulatoryCapital, riskWeightedAssets)",
                value: "0.15"
            }
        ]
    },
    2: {
        inputParameters: {
            "Validation Rules": {
                requiredFields: ["bankID", "totalAssets", "capitalRatio"],
                dataTypes: {
                    bankID: "string",
                    totalAssets: "number",
                    capitalRatio: "number"
                },
                validationThresholds: {
                    minCapitalRatio: 0.08,
                    maxTotalAssets: 1000000000000
                }
            }
        },
        resultParameters: {
            "Validation Results": {
                isValid: true,
                validationErrors: [],
                validationWarnings: ["Data is more than 30 days old"]
            }
        },
        spreadsheetSteps: [
            {
                step: "DataCompletenessCheck",
                description: "= ValidateRequiredFields(inputData, requiredFields)",
                value: "true"
            },
            {
                step: "DataTypeValidation",
                description: "= ValidateDataTypes(inputData, expectedTypes)",
                value: "true"
            },
            {
                step: "ThresholdValidation",
                description: "= ValidateThresholds(inputData, thresholds)",
                value: "true"
            }
        ]
    },
    3: {
        inputParameters: {
            "Risk Parameters": {
                creditRiskWeight: 0.6,
                marketRiskWeight: 0.3,
                operationalRiskWeight: 0.1,
                confidenceLevel: 0.99
            }
        },
        resultParameters: {
            "Risk Assessment": {
                totalRiskScore: 75,
                riskCategory: "Medium",
                riskBreakdown: {
                    creditRisk: 45,
                    marketRisk: 20,
                    operationalRisk: 10
                }
            }
        },
        spreadsheetSteps: [
            {
                step: "CreditRiskCalculation",
                description: "= CalculateCreditRisk(creditData, creditRiskWeight)",
                value: "45"
            },
            {
                step: "MarketRiskAssessment",
                description: "= CalculateMarketRisk(marketData, marketRiskWeight)",
                value: "20"
            },
            {
                step: "OperationalRiskCalculation",
                description: "= CalculateOperationalRisk(operationalData, operationalRiskWeight)",
                value: "10"
            },
            {
                step: "TotalRiskAggregation",
                description: "= AggregateRisks(creditRisk, marketRisk, operationalRisk)",
                value: "75"
            }
        ]
    }
}

const toTreeNode = (n: TraceNodeApi, isFavorite: boolean = false): DataNode => {
    let icon = undefined
    
    if (n.extraClasses === 'spreadsheet') {
        icon = <TableOutlined style={{ color: '#1890ff' }} />
    } else if (n.extraClasses === 'decisiontable') {
        icon = <TableOutlined style={{ color: '#fa8c16' }} />
    }
    
    // Show as favorite if it's in favorites
    const shouldShowAsFavorite = isFavorite
    
    return {
        key: n.key,
        title: (
            <div 
                className={shouldShowAsFavorite ? 'favorite-tree-node' : ''}
                style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    width: '100%'
                }}
                data-node-key={n.key}
                data-node-title={n.title}
            >
                <span style={{ 
                    flex: 1, 
                    overflow: 'hidden', 
                    textOverflow: 'ellipsis',
                    fontWeight: shouldShowAsFavorite ? '500' : 'normal'
                }}>
                    {n.title}
                </span>
                {shouldShowAsFavorite && (
                    <StarFilled 
                        className="favorite-star"
                        style={{ color: '#faad14', marginLeft: 4, flexShrink: 0, fontSize: '12px' }} 
                    />
                )}
            </div>
        ),
        icon,
        isLeaf: n.lazy === false,
        children: n.lazy ? undefined : [],
    }
}

const Trace: React.FC = () => {
    const { userProfile } = useUserStore()
    const showRealNumbers = !!userProfile?.showRealNumbers

    const [treeData, setTreeData] = useState<DataNode[]>([])
    const [loading, setLoading] = useState(false)
    const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
    const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
    const [favorites, setFavorites] = useState<Map<number, FavoriteNode>>(new Map())
    const [showDetailed, setShowDetailed] = useState(true)
    const [detailsView, setDetailsView] = useState<'json' | 'tree'>('json')
    const [selectedNodeDetails, setSelectedNodeDetails] = useState<TraceDetails | null>(null)
    const [detailsLoading, setDetailsLoading] = useState(false)
    const [showFavorites, setShowFavorites] = useState(false)
    const [allNodes, setAllNodes] = useState<Map<number, TraceNodeApi>>(new Map())
    const [contextMenu, setContextMenu] = useState<{ visible: boolean; x: number; y: number; nodeKey: number; path: number[]; title?: string }>({
        visible: false,
        x: 0,
        y: 0,
        nodeKey: 0,
        path: [],
        title: undefined
    })
    const treeRef = useRef<HTMLDivElement>(null)

    // Load favorites from localStorage on component mount
    useEffect(() => {
        const savedFavorites = localStorage.getItem('trace-favorites')
        if (savedFavorites) {
            try {
                const favoritesArray = JSON.parse(savedFavorites)
                setFavorites(new Map(favoritesArray.map((f: any) => [f.key, f])))
            } catch (e) {
                console.error('Failed to load favorites:', e)
            }
        }
    }, [])

    // Save favorites to localStorage whenever favorites change
    useEffect(() => {
        localStorage.setItem('trace-favorites', JSON.stringify(Array.from(favorites.values())))
    }, [favorites])

    // Get node path and title from tree data
    const getNodePathAndTitle = useCallback((nodeKey: number): { path: number[], title: string } => {
        const path: number[] = []
        let title = `Node ${nodeKey}`
        
        // Search through tree data to find the node and its path
        const findNodeInTree = (nodes: DataNode[], currentPath: number[] = []): boolean => {
            for (const node of nodes) {
                const nodeKeyNum = Number(node.key)
                if (nodeKeyNum === nodeKey) {
                    // Found the node, use the current path
                    path.push(...currentPath)
                    
                    // Try to extract title from the node
                    if (typeof node.title === 'string') {
                        title = node.title
                    } else if (node.title && typeof node.title === 'object') {
                        // Try to extract title from React element
                        const titleElement = node.title as any
                        if (titleElement.props && titleElement.props.children) {
                            const children = titleElement.props.children
                            if (Array.isArray(children)) {
                                // Look for the text content in children
                                for (const child of children) {
                                    if (typeof child === 'string') {
                                        title = child
                                        break
                                    } else if (child && child.props && child.props.children) {
                                        const grandChild = child.props.children
                                        if (typeof grandChild === 'string') {
                                            title = grandChild
                                            break
                                        }
                                    }
                                }
                            } else if (typeof children === 'string') {
                                title = children
                            }
                        }
                    }
                    

                    return true
                }
                
                // Check children recursively
                if (node.children && node.children.length > 0) {
                    const newPath = [...currentPath, nodeKeyNum]
                    if (findNodeInTree(node.children, newPath)) {
                        return true
                    }
                }
            }
            return false
        }
        
        findNodeInTree(treeData)
        return { path, title }
    }, [treeData])

    const toggleFavorite = useCallback((nodeKey: number, path: number[] = [], title?: string) => {
        // If path is empty or title is generic, try to get it from tree data
        let finalPath = path
        let finalTitle = title
        
        if (path.length === 0 || !title || title.startsWith('Node ')) {
            const nodeInfo = getNodePathAndTitle(nodeKey)
            finalPath = path.length > 0 ? path : nodeInfo.path
            finalTitle = title && !title.startsWith('Node ') ? title : nodeInfo.title
        }
        

        
        setFavorites(prev => {
            const newFavorites = new Map(prev)
            const wasFavorite = newFavorites.has(nodeKey)
            
            if (wasFavorite) {
                newFavorites.delete(nodeKey)
            } else {
                newFavorites.set(nodeKey, { 
                    key: nodeKey, 
                    path: finalPath,
                    title: finalTitle
                })
            }
            
            // Update the tree data to reflect the favorite status change
            setTreeData(prevTreeData => {
                const updateNodeFavoriteStatus = (nodes: DataNode[]): DataNode[] => {
                    return nodes.map(node => {
                        if (node.key === nodeKey) {
                            // Update the node's title to reflect new favorite status
                            const updatedTitle = (
                                <div 
                                    className={!wasFavorite ? 'favorite-tree-node' : ''}
                                    style={{ 
                                        display: 'flex', 
                                        alignItems: 'center', 
                                        width: '100%'
                                    }}
                                    data-node-key={node.key}
                                    data-node-title={finalTitle || node.title}
                                >
                                    <span style={{ 
                                        flex: 1, 
                                        overflow: 'hidden', 
                                        textOverflow: 'ellipsis',
                                        fontWeight: !wasFavorite ? '500' : 'normal'
                                    }}>
                                        {finalTitle || (typeof node.title === 'string' ? node.title : 'Node ' + node.key)}
                                    </span>
                                    {!wasFavorite && (
                                        <StarFilled 
                                            className="favorite-star"
                                            style={{ color: '#faad14', marginLeft: 4, flexShrink: 0, fontSize: '12px' }} 
                                        />
                                    )}
                                </div>
                            )
                            return { ...node, title: updatedTitle }
                        }
                        if (node.children) {
                            return { ...node, children: updateNodeFavoriteStatus(node.children) }
                        }
                        return node
                    })
                }
                return updateNodeFavoriteStatus(prevTreeData)
            })
            
            return newFavorites
        })
        setContextMenu(prev => ({ ...prev, visible: false }))
    }, [getNodePathAndTitle])

    const clearFavorites = useCallback(() => {
        setFavorites(new Map())
    }, [])

    const loadRoot = useCallback(async () => {
        setLoading(true)
        try {
            const params = new URLSearchParams()
            params.set('id', '0')
            if (showRealNumbers) params.set('showRealNumbers', 'true')
            const data: TraceNodeApi[] = await apiCall(`/trace/nodes?${params.toString()}`)
            
            // Store all nodes for favorites lookup
            const nodesMap = new Map<number, TraceNodeApi>()
            data.forEach(n => {
                nodesMap.set(n.key, n)
            })
            setAllNodes(nodesMap)
            
            // Create nodes with current favorite status
            const nodes = Array.isArray(data) ? data.map(n => toTreeNode(n, favorites.has(n.key))) : []
            setTreeData(nodes)
            
            // Auto-expand favorite nodes
            const favoriteKeys = Array.from(favorites.keys())
            if (favoriteKeys.length > 0) {
                setExpandedKeys(favoriteKeys)
            }
        } catch (error) {
            console.error('Failed to load trace root nodes:', error)
            // Fallback to empty tree if API fails
            setTreeData([])
        } finally {
            setLoading(false)
        }
    }, [showRealNumbers])

    useEffect(() => {
        loadRoot()
    }, [loadRoot])

    // Update tree nodes when favorites change (without full reload)
    useEffect(() => {
        setTreeData(prevTreeData => {
            const updateFavoritesInTree = (nodes: DataNode[]): DataNode[] => {
                return nodes.map(node => {
                    const nodeKey = Number(node.key)
                    const isInFavorites = favorites.has(nodeKey)
                    
                    // Get the actual title of the node
                    let nodeTitle = ''
                    if (typeof node.title === 'string') {
                        nodeTitle = node.title
                    } else if (node.title && typeof node.title === 'object' && 'props' in node.title && node.title.props) {
                        // Extract title from React element
                        const titleElement = node.title as any
                        if (titleElement.props.children) {
                            const children = titleElement.props.children
                            if (Array.isArray(children)) {
                                for (const child of children) {
                                    if (typeof child === 'string') {
                                        nodeTitle = child
                                        break
                                    } else if (child && child.props && child.props.children) {
                                        const grandChild = child.props.children
                                        if (typeof grandChild === 'string') {
                                            nodeTitle = grandChild
                                            break
                                        }
                                    }
                                }
                            } else if (typeof children === 'string') {
                                nodeTitle = children
                            }
                        }
                    }
                    

                    
                    // Highlight as favorite if it's in favorites
                    const isFavorite = isInFavorites
                    

                    
                    // Check if the node's favorite status needs updating
                    const currentTitle = node.title
                    let needsUpdate = false
                    
                    if (currentTitle && typeof currentTitle === 'object' && 'props' in currentTitle && currentTitle.props) {
                        // Check if the node has the favorite class
                        const hasFavoriteClass = (currentTitle.props as any).className?.includes('favorite-tree-node')
                        needsUpdate = hasFavoriteClass !== isFavorite
                    } else {
                        // For string titles, we need to check if it should be a favorite
                        needsUpdate = isFavorite
                    }
                    
                    if (needsUpdate) {
                        const nodeData = allNodes.get(Number(node.key))
                        const nodeTitle = nodeData?.title || (typeof currentTitle === 'string' ? currentTitle : `Node ${node.key}`)
                        
                        const updatedTitle = (
                            <div 
                                className={isFavorite ? 'favorite-tree-node' : ''}
                                style={{ 
                                    display: 'flex', 
                                    alignItems: 'center', 
                                    width: '100%'
                                }}
                                data-node-key={node.key}
                                data-node-title={nodeTitle}
                            >
                                <span style={{ 
                                    flex: 1, 
                                    overflow: 'hidden', 
                                    textOverflow: 'ellipsis',
                                    fontWeight: isFavorite ? '500' : 'normal'
                                }}>
                                    {nodeTitle}
                                </span>
                                {isFavorite && (
                                    <StarFilled 
                                        className="favorite-star"
                                        style={{ color: '#faad14', marginLeft: 4, flexShrink: 0, fontSize: '12px' }} 
                                    />
                                )}
                            </div>
                        )
                        return { ...node, title: updatedTitle }
                    }
                    
                    if (node.children) {
                        return { ...node, children: updateFavoritesInTree(node.children) }
                    }
                    return node
                })
            }
            return updateFavoritesInTree(prevTreeData)
        })
    }, [favorites, allNodes])

    const updateTreeData = useCallback((list: DataNode[], key: React.Key, children: DataNode[]): DataNode[] =>
        list.map((node) => {
            if (node.key === key) {
                return {
                    ...node,
                    children,
                }
            }
            if (node.children) {
                return { ...node, children: updateTreeData(node.children, key, children) }
            }
            return node
        })
    , [])

    const loadData = useCallback(async (node: any) => {
        if (node.children && node.children.length > 0) return
        
        try {
            const params = new URLSearchParams()
            params.set('id', String(node.key))
            if (showRealNumbers) params.set('showRealNumbers', 'true')
            const data: TraceNodeApi[] = await apiCall(`/trace/nodes?${params.toString()}`)
            
            // Update all nodes map
            setAllNodes(prev => {
                const newMap = new Map(prev)
                data.forEach(n => {
                    newMap.set(n.key, n)
                })
                return newMap
            })
            
            // Create children with current favorite status
            const children = Array.isArray(data) ? data.map(n => toTreeNode(n, favorites.has(n.key))) : []
            setTreeData((orig) => updateTreeData(orig, node.key, children))
        } catch (error) {
            console.error(`Failed to load trace nodes for key ${node.key}:`, error)
            // Set empty children to prevent infinite loading
            setTreeData((orig) => updateTreeData(orig, node.key, []))
        }
    }, [showRealNumbers, updateTreeData])

    const onSelect = useCallback((keys: React.Key[], info: any) => {
        setSelectedKeys(keys)
        // Expand the clicked node to show nested items
        if (keys.length > 0) {
            const clickedKey = keys[keys.length - 1]
            setExpandedKeys(prev => {
                if (prev.includes(clickedKey)) {
                    return prev.filter(key => key !== clickedKey)
                } else {
                    return [...prev, clickedKey]
                }
            })
        }
    }, [])

    const onExpand = useCallback((expandedKeys: React.Key[]) => {
        setExpandedKeys(expandedKeys)
    }, [])

    // Function to expand tree to show a specific node
    const expandToNode = useCallback(async (nodeKey: number) => {
        const favoriteNode = favorites.get(nodeKey)
        if (!favoriteNode) return

        // Set the selected key
        setSelectedKeys([nodeKey])

        // If we have a path, expand all parent nodes
        if (favoriteNode.path && favoriteNode.path.length > 0) {
            const pathToExpand = [...favoriteNode.path, nodeKey]
            setExpandedKeys(prev => {
                const newExpandedKeys = [...prev]
                pathToExpand.forEach(key => {
                    if (!newExpandedKeys.includes(key)) {
                        newExpandedKeys.push(key)
                    }
                })
                return newExpandedKeys
            })

            // Ensure all parent nodes are loaded
            for (const parentKey of favoriteNode.path) {
                try {
                    const params = new URLSearchParams()
                    params.set('id', String(parentKey))
                    if (showRealNumbers) params.set('showRealNumbers', 'true')
                    const data: TraceNodeApi[] = await apiCall(`/trace/nodes?${params.toString()}`)
                    
                    // Update tree data to include the loaded children
                    setTreeData(prev => {
                        const updateNode = (nodes: DataNode[]): DataNode[] => {
                            return nodes.map(node => {
                                if (node.key === parentKey) {
                                    const children = Array.isArray(data) ? data.map(n => toTreeNode(n, favorites.has(n.key))) : []
                                    return { ...node, children }
                                }
                                if (node.children) {
                                    return { ...node, children: updateNode(node.children) }
                                }
                                return node
                            })
                        }
                        return updateNode(prev)
                    })
                } catch (error) {
                    console.error(`Failed to load parent node ${parentKey}:`, error)
                }
            }
        }
    }, [favorites, showRealNumbers])

    const loadNodeDetails = useCallback(async (nodeKey: number) => {
        setDetailsLoading(true)
        try {
            // For now, use mock details data since we don't have the details API endpoint
            // TODO: Replace with actual API call when available
            const details = mockDetailsData[nodeKey] || {
                inputParameters: {},
                resultParameters: {},
                spreadsheetSteps: []
            }
            setSelectedNodeDetails(details)
        } catch (error) {
            console.error(`Failed to load details for node ${nodeKey}:`, error)
            setSelectedNodeDetails(null)
        } finally {
            setDetailsLoading(false)
        }
    }, [])

    const selectedKey = useMemo(() => (selectedKeys && selectedKeys.length > 0 ? selectedKeys[0] : undefined), [selectedKeys])

    useEffect(() => {
        if (selectedKey !== undefined) {
            loadNodeDetails(Number(selectedKey))
        }
    }, [selectedKey, loadNodeDetails])

    const spreadsheetStepsColumns = [
        {
            title: 'Step',
            dataIndex: 'step',
            key: 'step',
            width: '30%',
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
            width: '40%',
        },
        {
            title: 'Value',
            dataIndex: 'value',
            key: 'value',
            width: '30%',
        },
    ]

    const renderTreeView = (data: any) => {
        const renderNode = (obj: any, path: string = '') => {
            return Object.entries(obj).map(([key, value]) => {
                const currentPath = path ? `${path}.${key}` : key
                if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                    return (
                        <Collapse key={currentPath} size="small" style={{ marginBottom: 8 }}>
                            <Collapse.Panel header={key} key={currentPath}>
                                {renderNode(value, currentPath)}
                            </Collapse.Panel>
                        </Collapse>
                    )
                } else {
                    return (
                        <div key={currentPath} style={{ marginBottom: 4 }}>
                            <Typography.Text strong>{key}:</Typography.Text>
                            <Typography.Text style={{ marginLeft: 8 }}>
                                {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                            </Typography.Text>
                        </div>
                    )
                }
            })
        }
        return renderNode(data)
    }

    const favoriteNodes = useMemo(() => {
        const nodes = Array.from(favorites.keys()).map(key => {
            const node = allNodes.get(key)
            return node
        }).filter(Boolean) as TraceNodeApi[]
        
        return nodes
    }, [favorites, allNodes])

    // Load missing favorite nodes
    const loadMissingFavoriteNodes = useCallback(async () => {
        const missingKeys = Array.from(favorites.keys()).filter(key => !allNodes.has(key))
        
        if (missingKeys.length > 0) {
            for (const key of missingKeys) {
                try {
                    const favoriteNode = favorites.get(key)
                    const nodeTitle = favoriteNode?.title || `Node ${key}`
                    
                    // For now, create mock node data since the API returns child nodes, not the node itself
                    // TODO: Replace with actual API call when proper endpoint is available
                    const mockNode: TraceNodeApi = {
                        key: key,
                        title: nodeTitle,
                        tooltip: `This is favorite node ${key}`,
                        extraClasses: 'spreadsheet',
                        lazy: false
                    }
                    
                    setAllNodes(prev => {
                        const newMap = new Map(prev)
                        newMap.set(key, mockNode)
                        return newMap
                    })
                    
                } catch (error) {
                    console.error(`Failed to load missing favorite node ${key}:`, error)
                }
            }
        }
    }, [favorites, allNodes])

    // Reconstruct node title from path
    const getNodeTitleFromPath = useCallback((favoriteNode: FavoriteNode): string => {
        if (favoriteNode.title) {
            return favoriteNode.title
        }
        
        // Try to reconstruct title from path
        if (favoriteNode.path.length > 0) {
            // For now, return a path-based title
            return `Node ${favoriteNode.key} (Path: ${favoriteNode.path.join(' → ')})`
        }
        
        return `Favorite Node ${favoriteNode.key}`
    }, [])

    // Load all favorite nodes immediately when component loads
    const loadAllFavoriteNodes = useCallback(async () => {
        if (favorites.size > 0) {
            for (const [key, favoriteNode] of favorites) {
                try {
                    // Use the stored title from the favorite node, or fall back to reconstructed title
                    const nodeTitle = favoriteNode.title || getNodeTitleFromPath(favoriteNode)
                    
                    // Create node with the actual title
                    const mockNode: TraceNodeApi = {
                        key: key,
                        title: nodeTitle,
                        tooltip: `This is favorite node ${key}`,
                        lazy: false
                    }
                    
                    setAllNodes(prev => {
                        const newMap = new Map(prev)
                        newMap.set(key, mockNode)
                        return newMap
                    })
                    
                } catch (error) {
                    console.error(`Failed to load favorite node ${key}:`, error)
                }
            }
        }
    }, [favorites, getNodeTitleFromPath])

    // Load all favorite nodes when component mounts
    useEffect(() => {
        loadAllFavoriteNodes()
    }, [loadAllFavoriteNodes])

    // Load missing favorite nodes when favorites change
    useEffect(() => {
        if (favorites.size > 0) {
            loadMissingFavoriteNodes()
        }
    }, [favorites, loadMissingFavoriteNodes])



    // Handle right-click on tree nodes
    useEffect(() => {
        const handleContextMenu = (event: MouseEvent) => {
            const target = event.target as HTMLElement
            
            const treeNode = target.closest('.ant-tree-node-content-wrapper')
            
            if (treeNode && treeRef.current?.contains(treeNode)) {
                event.preventDefault()
                event.stopPropagation()
                
                // Try multiple ways to find the node key
                let nodeKey: number | null = null
                
                // Method 1: Look for data-node-key attribute
                const nodeKeyElement = treeNode.querySelector('[data-node-key]')
                if (nodeKeyElement) {
                    nodeKey = Number(nodeKeyElement.getAttribute('data-node-key'))
                }
                
                // Method 2: Look for the key in the tree node's parent li element
                if (!nodeKey) {
                    const liElement = treeNode.closest('li')
                    if (liElement) {
                        const keyAttr = liElement.getAttribute('data-node-key')
                        if (keyAttr) {
                            nodeKey = Number(keyAttr)
                        }
                    }
                }
                
                // Method 3: Look for the key in the tree node itself
                if (!nodeKey) {
                    const keyAttr = treeNode.getAttribute('data-node-key')
                    if (keyAttr) {
                        nodeKey = Number(keyAttr)
                    }
                }
                
                // Method 4: Look for the key in any child element
                if (!nodeKey) {
                    const allElements = treeNode.querySelectorAll('[data-node-key]')
                    for (const element of allElements) {
                        const key = Number(element.getAttribute('data-node-key'))
                        if (key) {
                            nodeKey = key
                            break
                        }
                    }
                }
                
                if (nodeKey) {
                    // Calculate the path to this node
                    const path: number[] = []
                    let currentElement = treeNode.closest('li')
                    
                    // Extract the title from the current node
                    let nodeTitle: string | undefined
                    const titleElement = treeNode.querySelector('[data-node-title]')
                    if (titleElement) {
                        nodeTitle = titleElement.getAttribute('data-node-title') || undefined
                    }
                    
                    // Walk up the tree to build the path
                    while (currentElement) {
                        // First, try to find the node key in the current element
                        const currentKeyElement = currentElement.querySelector('[data-node-key]')
                        if (currentKeyElement) {
                            const currentKey = Number(currentKeyElement.getAttribute('data-node-key'))
                            if (currentKey && currentKey !== nodeKey) {
                                path.unshift(currentKey)
                            }
                        }
                        
                        // Find the parent li element
                        const parentLi = currentElement.parentElement?.closest('li') || null
                        if (!parentLi) {
                            break
                        }
                        
                        currentElement = parentLi
                    }
                    
                    setContextMenu({
                        visible: true,
                        x: event.clientX,
                        y: event.clientY,
                        nodeKey,
                        path,
                        title: nodeTitle
                    })
                }
            } else {
                // Hide context menu if clicking outside tree
                setContextMenu(prev => ({ ...prev, visible: false }))
            }
        }

        const handleClick = (event: MouseEvent) => {
            const target = event.target as HTMLElement
            const isContextMenu = target.closest('[data-context-menu]')
            
            if (!isContextMenu) {
                setContextMenu(prev => ({ ...prev, visible: false }))
            }
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                setContextMenu(prev => ({ ...prev, visible: false }))
            }
        }

        document.addEventListener('contextmenu', handleContextMenu, true)
        document.addEventListener('click', handleClick, true)
        document.addEventListener('keydown', handleKeyDown)

        return () => {
            document.removeEventListener('contextmenu', handleContextMenu, true)
            document.removeEventListener('click', handleClick, true)
            document.removeEventListener('keydown', handleKeyDown)
        }
    }, [])

    const contextMenuItems = useMemo(() => {
        if (!contextMenu.visible) return []
        
        const isFavorite = favorites.has(contextMenu.nodeKey)
        return [
            {
                key: 'favorite',
                icon: isFavorite ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />,
                label: isFavorite ? 'Remove from Favorites' : 'Add to Favorites',
                onClick: () => toggleFavorite(contextMenu.nodeKey, contextMenu.path, contextMenu.title)
            },
            {
                key: 'select',
                label: 'Select Node',
                onClick: () => setSelectedKeys([contextMenu.nodeKey])
            }
        ]
    }, [contextMenu, favorites, toggleFavorite])

    return (
        <>
            <style>
                {`
                    .trace-tree .ant-tree-node-content-wrapper {
                        display: flex !important;
                        align-items: center !important;
                        flex-wrap: nowrap !important;
                    }
                    .trace-tree .ant-tree-title {
                        display: flex !important;
                        align-items: center !important;
                        flex-wrap: nowrap !important;
                        white-space: nowrap !important;
                        overflow: hidden !important;
                        text-overflow: ellipsis !important;
                    }
                    .trace-tree .ant-tree-iconEle {
                        display: flex !important;
                        align-items: center !important;
                        flex-shrink: 0 !important;
                    }
                    .trace-tree .ant-tree-switcher {
                        flex-shrink: 0 !important;
                    }
                    .trace-tree .ant-tree-node-content-wrapper:hover {
                        background-color: #f5f5f5 !important;
                    }
                    .trace-tree .favorite-tree-node {
                        background-color: #fff7e6 !important;
                        border-left: 2px solid #faad14 !important;
                        position: relative;
                    }
                    .trace-tree .favorite-tree-node::before {
                        content: '';
                        position: absolute;
                        left: -2px;
                        top: 0;
                        bottom: 0;
                        width: 2px;
                        background-color: #faad14;
                        border-radius: 1px 0 0 1px;
                    }
                    .trace-tree .ant-tree-node-content-wrapper:hover .favorite-tree-node {
                        background-color: #fff2d9 !important;
                    }
                    .favorite-star {
                        animation: favoritePulse 0.6s ease-in-out;
                    }
                    @keyframes favoritePulse {
                        0% { transform: scale(1); }
                        50% { transform: scale(1.2); }
                        100% { transform: scale(1); }
                    }
                `}
            </style>
            <div style={{ display: 'flex', height: '100vh', width: '100%' }}>
                {/* Left Panel - Trace Tree */}
                <div style={{ minWidth: 260, width: showFavorites ? '25%' : '35%', overflow: 'auto', margin: 5 }}>
                    <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Typography.Text strong>Trace Tree</Typography.Text>
                        <Checkbox 
                            checked={showDetailed} 
                            onChange={(e) => setShowDetailed(e.target.checked)}
                            style={{ marginLeft: 'auto' }}
                        >
                            Detailed
                        </Checkbox>
                        <Typography.Text>Hide ms</Typography.Text>
                        <Button 
                            size="small" 
                            onClick={() => setShowFavorites(!showFavorites)}
                            icon={<StarOutlined />}
                        >
                            <Badge count={favorites.size} size="small">
                                Favorites
                            </Badge>
                        </Button>
                        <Button size="small" onClick={clearFavorites}>Clear</Button>
                    </div>
                    <div ref={treeRef} style={{ paddingTop: 8, height: 'calc(100vh - 120px)', overflow: 'auto' }}>
                        {loading ? (
                            <Spin />
                        ) : (
                            <Tree
                                treeData={treeData}
                                loadData={loadData}
                                onSelect={onSelect}
                                onExpand={onExpand}
                                selectedKeys={selectedKeys}
                                expandedKeys={expandedKeys}
                                showLine
                                showIcon
                                style={{ height: '100%' }}
                                className="trace-tree"
                            />
                        )}
                    </div>
                </div>

                {/* Favorites Panel */}
                {showFavorites && (
                    <>
                        <div style={{ width: 10, cursor: 'ew-resize', background: 'gray', border: '5px solid white' }} />
                        <div style={{ minWidth: 200, width: '15%', overflow: 'auto', margin: 5 }}>
                            <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8 }}>
                                <Typography.Text strong>Favorites ({favorites.size})</Typography.Text>
                            </div>
                            <div style={{ paddingTop: 8, height: 'calc(100vh - 120px)', overflow: 'auto' }}>
                                {favoriteNodes.length > 0 ? (
                                    <div>
                                        {favoriteNodes.map(node => (
                                            <div 
                                                key={node.key}
                                                style={{ 
                                                    padding: '8px', 
                                                    border: '1px solid #d9d9d9', 
                                                    marginBottom: '4px', 
                                                    borderRadius: '4px',
                                                    cursor: 'pointer',
                                                    backgroundColor: selectedKeys.includes(node.key) ? '#e6f7ff' : 'white'
                                                }}
                                                onClick={() => expandToNode(node.key)}
                                            >
                                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                                    <span style={{ fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                        {node.title}
                                                    </span>
                                                    <Button 
                                                        size="small" 
                                                        type="text" 
                                                        icon={<StarFilled style={{ color: '#faad14' }} />}
                                                        onClick={(e) => {
                                                            e.stopPropagation()
                                                            toggleFavorite(node.key)
                                                        }}
                                                    />
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <Typography.Text type="secondary">No favorites yet. Right-click on tree nodes to add them.</Typography.Text>
                                )}
                            </div>
                        </div>
                    </>
                )}

                {/* Resizer */}
                <div style={{ width: 10, cursor: 'ew-resize', background: 'gray', border: '5px solid white' }} />

                {/* Right Panel - Details */}
                <div style={{ minWidth: 300, flex: 1, overflow: 'auto', margin: 5 }}>
                    <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8 }}>
                        <Typography.Text strong>Details Panel</Typography.Text>
                        <div style={{ float: 'right', display: 'flex', gap: 8 }}>
                            <Switch 
                                checked={detailsView === 'json'} 
                                onChange={(checked) => setDetailsView(checked ? 'json' : 'tree')}
                                checkedChildren="Raw JSON"
                                unCheckedChildren="Tree View"
                            />
                        </div>
                    </div>
                    <Divider />
                    <div style={{ height: 'calc(100vh - 120px)', overflow: 'auto' }}>
                        {selectedKey === undefined ? (
                            <Typography.Text>Select a trace element on the left to see its details.</Typography.Text>
                        ) : detailsLoading ? (
                            <Spin />
                        ) : selectedNodeDetails ? (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                                {/* Input and Result Parameters */}
                                <div style={{ display: 'flex', gap: 16 }}>
                                    <div style={{ flex: 1 }}>
                                        <Typography.Text strong>Input parameters:</Typography.Text>
                                        <div style={{ marginTop: 8 }}>
                                            {detailsView === 'json' ? (
                                                <pre style={{ fontSize: 12, backgroundColor: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                                    {JSON.stringify(selectedNodeDetails.inputParameters, null, 2)}
                                                </pre>
                                            ) : (
                                                renderTreeView(selectedNodeDetails.inputParameters)
                                            )}
                                        </div>
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <Typography.Text strong>Result parameters:</Typography.Text>
                                        <div style={{ marginTop: 8 }}>
                                            {detailsView === 'json' ? (
                                                <pre style={{ fontSize: 12, backgroundColor: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                                    {JSON.stringify(selectedNodeDetails.resultParameters, null, 2)}
                                                </pre>
                                            ) : (
                                                renderTreeView(selectedNodeDetails.resultParameters)
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* Spreadsheet Steps */}
                                <div>
                                    <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8, marginBottom: 8 }}>
                                        <Typography.Text strong>Spreadsheet Steps</Typography.Text>
                                        <div style={{ float: 'right', display: 'flex', gap: 8 }}>
                                            <Button size="small">Open in Editor</Button>
                                            <Button size="small">Visible Columns</Button>
                                        </div>
                                    </div>
                                    {selectedNodeDetails.spreadsheetSteps && selectedNodeDetails.spreadsheetSteps.length > 0 ? (
                                        <Table 
                                            columns={spreadsheetStepsColumns} 
                                            dataSource={selectedNodeDetails.spreadsheetSteps}
                                            pagination={false}
                                            size="small"
                                            scroll={{ y: 200 }}
                                            rowKey="step"
                                        />
                                    ) : (
                                        <Typography.Text type="secondary">No spreadsheet steps available</Typography.Text>
                                    )}
                                </div>
                            </div>
                        ) : null}
                    </div>
                </div>
            </div>

            {/* Context Menu */}
            {contextMenu.visible && (
                <div
                    data-context-menu="true"
                    style={{
                        position: 'fixed',
                        top: contextMenu.y,
                        left: contextMenu.x,
                        zIndex: 1000,
                        backgroundColor: 'white',
                        border: '1px solid #d9d9d9',
                        borderRadius: '6px',
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
                        padding: '4px 0',
                        minWidth: '150px'
                    }}
                >
                    {contextMenuItems.map(item => (
                        <div
                            key={item.key}
                            data-context-menu="true"
                            style={{
                                padding: '8px 16px',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                fontSize: '14px',
                                userSelect: 'none'
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.backgroundColor = '#f5f5f5'
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.backgroundColor = 'transparent'
                            }}
                            onClick={(e) => {
                                e.stopPropagation()
                                item.onClick()
                            }}
                        >
                            {item.icon}
                            {item.label}
                        </div>
                    ))}
                </div>
            )}
        </>
    )
}

export { Trace } 