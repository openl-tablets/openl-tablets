import React, { FC } from 'react'
import { Popconfirm, Table } from 'antd'
import { useTranslation } from 'react-i18next'
import { TagTableCheckboxCell } from './TagTableCheckboxCell'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import { TagTableNameCell } from './TagTableNameCell'
import { TagTableTagsCell } from './TagTableTagsCell'
import './TagTable.scss'
import { DeleteOutlined } from '@ant-design/icons'

export interface Tag {
    id: number;
    name: string;
    tagTypeId: number;
}

export interface TagActions {
    createTag: (tagName: string, tagTypeId: number) => void;
    updateTag: (tag: Tag) => void;
    deleteTag: (tag: Tag) => void;
}

export interface TagType {
    id: number;
    name: string;
    extensible: boolean;
    nullable: boolean;
    tags: Tag[];
}

export interface TagTypeActions {
    updateTagType: (tagType: TagType) => boolean | Promise<boolean>;
    deleteTagType: (tagType: TagType) => void;
}

interface TagTableProps extends TagActions, TagTypeActions {
    tagTypes: TagType[];
    isLoading: boolean;
}

export const TagTable: FC<TagTableProps> = ({ tagTypes, createTag, updateTag, deleteTag, deleteTagType, updateTagType, isLoading }) => {
    const { t } = useTranslation()

    const onUpdateTagType = (tagType: TagType, name: string | null = null, extensible: boolean | null = null, nullable: boolean | null = null) => {
        if (tagType) {
            return updateTagType({
                ...tagType,
                name: name !== null ? name : tagType.name,
                extensible: extensible !== null ? extensible : tagType.extensible,
                nullable: nullable !== null ? nullable : tagType.nullable,
            })
        }

        return false
    }

    const onChangeTagTypeName = (tagType: TagType) => (name: string) => {
        return onUpdateTagType(tagType, name)
    }

    const onChangeTagTypeExtensible = (tagType: TagType) => (e: CheckboxChangeEvent) => {
        onUpdateTagType(tagType, null, e.target.checked)
    }

    const onChangeTagTypeNullable = (tagType: TagType) => (e: CheckboxChangeEvent) => {
        onUpdateTagType(tagType, null, null, e.target.checked)
    }

    const handleDelete = (tagType: TagType) => {
        deleteTagType(tagType)
    }

    const columns = [
        {
            title: t('tags:tag_type'),
            key: 'name',
            width: '300px',
            render: (tagType: TagType) => (
                <TagTableNameCell name={tagType.name} onChange={onChangeTagTypeName(tagType)} />
            ),
        },
        {
            title: t('tags:extensible'),
            key: 'extensible',
            render: (tagType: TagType) => (
                <TagTableCheckboxCell isChecked={tagType.extensible} onChange={onChangeTagTypeExtensible(tagType)} />
            ),
        },
        {
            title: t('tags:nullable'),
            key: 'nullable',
            render: (tagType: TagType) => (
                <TagTableCheckboxCell isChecked={tagType.nullable} onChange={onChangeTagTypeNullable(tagType)} />
            ),
        },
        {
            title: t('tags:tags'),
            key: 'tags',
            render: (tagType: TagType) => (
                <TagTableTagsCell
                    createTag={createTag}
                    deleteTag={deleteTag}
                    tags={tagType.tags || []}
                    tagTypeId={tagType.id}
                    updateTag={updateTag}
                />
            ),
        },
        {
            title: t('tags:actions'),
            key: 'actions',
            width: '100px',
            render: (tagType: TagType) => (
                <Popconfirm onConfirm={() => handleDelete(tagType)} title="Sure to delete?">
                    <DeleteOutlined style={{ cursor: 'pointer', color: 'red' }} />
                </Popconfirm>
            ),
        },
    ]

    return (
        <div>
            <Table
                className="tag-table"
                columns={columns}
                dataSource={tagTypes}
                loading={isLoading}
                pagination={false}
                rowKey={(record) => record.id}
            />
        </div>
    )
}
