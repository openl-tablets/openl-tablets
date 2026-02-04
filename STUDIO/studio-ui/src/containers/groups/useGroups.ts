import { useContext } from 'react'
import { GroupsContext } from '../../contexts/GroupsContext'

export const useGroups = () => {
    return useContext(GroupsContext)
}
