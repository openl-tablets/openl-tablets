import React from 'react'
import { Card, Row, Input, Checkbox } from 'antd'
import { useTranslation } from 'react-i18next'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import { Tag, TagActions, TagList } from './TagList'

export interface TagTypeActions {
    updateTagType: (tagType: TagType) => void;
}

export interface TagType {
    id: number;
    name: string;
    extensible: boolean;
    nullable: boolean;
    tags: Tag[];
}

interface TagTypeCardProps extends TagActions, TagTypeActions {
    tagType: TagType;
}

export const TagTypeCard: React.FC<TagTypeCardProps> = ({ tagType, createTag, updateTag, deleteTag, updateTagType }) => {
    const { t } = useTranslation()
    const [ tagTypeName, setTagTypeName ] = React.useState(tagType.name)
    const [ tagTypeExtensible, setTagTypeExtensible ] = React.useState(tagType.extensible)
    const [ tagTypeNullable, setTagTypeNullable ] = React.useState(tagType.nullable)

    const onUpdateTagType = (name: string | null = null, extensible: boolean | null = null, nullable: boolean | null = null) => {
        updateTagType({
            ...tagType,
            name: name !== null ? name : tagTypeName,
            extensible: extensible !== null ? extensible : tagTypeExtensible,
            nullable: nullable !== null ? nullable : tagTypeNullable,
        })
    }

    const onChangeTagTypeName = (e: React.ChangeEvent<HTMLInputElement>) => {
        setTagTypeName(e.target.value)
    }

    const onChangeTagTypeExtensible = (e: CheckboxChangeEvent) => {
        setTagTypeExtensible(e.target.checked)
        onUpdateTagType(null, e.target.checked)
    }

    const onChangeTagTypeNullable = (e: CheckboxChangeEvent) => {
        setTagTypeNullable(e.target.checked)
        onUpdateTagType(null, null, e.target.checked)
    }

    const onBlurTagTypeName = () => {
        onUpdateTagType()
    }

    return (
        <Card>
            <Row>
                <Input bordered={false} name="tagTypeName" onBlur={onBlurTagTypeName} onChange={onChangeTagTypeName} value={tagTypeName} />
                <Checkbox checked={tagTypeExtensible} onChange={onChangeTagTypeExtensible}>
                    {t('tags:extensible')}
                </Checkbox>
                <Checkbox checked={tagTypeNullable} onChange={onChangeTagTypeNullable}>
                    {t('tags:nullable')}
                </Checkbox>
            </Row>
            <TagList createTag={createTag} deleteTag={deleteTag} tags={tagType.tags} tagTypeId={tagType.id} updateTag={updateTag} />
        </Card>
    )
}
