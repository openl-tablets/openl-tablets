import React from 'react'
import { TagTypeCard, TagType, TagTypeActions } from './TagTypeCard'
import { List } from 'antd'
import { TagActions } from './TagList'

interface TagTypeListProps extends TagActions, TagTypeActions {
    tagTypes: TagType[];
}

export const TagTypeList: React.FC<TagTypeListProps> = ({ tagTypes, createTag, updateTag, deleteTag, updateTagType }) => {
    return (
        <div>
            <List
                dataSource={tagTypes}
                grid={{ gutter: 16, column: 3 }}
                renderItem={item => (
                    <List.Item>
                        <TagTypeCard createTag={createTag} deleteTag={deleteTag} tagType={item} updateTag={updateTag} updateTagType={updateTagType} />
                    </List.Item>
                )}
            />
        </div>
    )
}