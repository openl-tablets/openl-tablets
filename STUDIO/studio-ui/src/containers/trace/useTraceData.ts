import { useCallback, useEffect, useMemo, useState } from 'react'
import type { DataNode } from 'antd/es/tree'
import { useUserStore } from '../../store/userStore'
import { TraceNodeApi, FavoriteNode } from './types'

export const useTraceData = () => {
    const { userProfile } = useUserStore()
    const showRealNumbers = !!userProfile?.showRealNumbers

    const [treeData, setTreeData] = useState<DataNode[]>([])
    const [loading, setLoading] = useState(false)
    const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
    const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
    const [favorites, setFavorites] = useState<Map<number, FavoriteNode>>(new Map())
    const [allNodes, setAllNodes] = useState<Map<number, TraceNodeApi>>(new Map())

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

    const clearFavorites = useCallback(() => {
        setFavorites(new Map())
    }, [])

    const toggleFavorite = useCallback((nodeKey: number, path: number[] = [], title?: string) => {
        setFavorites(prev => {
            const newFavorites = new Map(prev)
            const wasFavorite = newFavorites.has(nodeKey)
            
            if (wasFavorite) {
                newFavorites.delete(nodeKey)
            } else {
                newFavorites.set(nodeKey, { 
                    key: nodeKey, 
                    path: path.length > 0 ? path : [],
                    title: title || `Node ${nodeKey}`
                })
            }
            
            return newFavorites
        })
    }, [])

    const favoriteNodes = useMemo(() => {
        const nodes = Array.from(favorites.keys()).map(key => {
            const node = allNodes.get(key)
            return node
        }).filter(Boolean) as TraceNodeApi[]
        
        return nodes
    }, [favorites, allNodes])

    return {
        treeData,
        setTreeData,
        loading,
        setLoading,
        selectedKeys,
        setSelectedKeys,
        expandedKeys,
        setExpandedKeys,
        favorites,
        allNodes,
        setAllNodes,
        showRealNumbers,
        clearFavorites,
        toggleFavorite,
        favoriteNodes
    }
}
