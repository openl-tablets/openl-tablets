import { Input, Divider, Button, Row, Typography } from 'antd'
import TextArea from 'antd/es/input/TextArea'
import React, { useEffect, useState } from 'react'
import { Trans, useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { Tag, TagTable, TagType } from './tags/TagTable'


export const Tags: React.FC = () => {
    const { t } = useTranslation()
    const [tagTypes, setTagTypes] = useState<TagType[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [newTagTypeName, setNewTagTypeName] = useState('')

    const fetchTagTypes = async () => {
        setIsLoading(true)
        const response = await apiCall('/admin/tag-config/types')
        setTagTypes(response)
        setIsLoading(false)
    }

    const createTagType = async (tagTypeName: string) => {
        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        setIsLoading(true)
        await apiCall('/admin/tag-config/types', {
            method: 'POST',
            headers,
            body: JSON.stringify({
                name: tagTypeName,
                nullable: false,
                extensible: false,
            }),
        })
        setIsLoading(false)
    }

    const updateTagType = async (tagType: TagType) => {
        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        setIsLoading(true)
        const result = await apiCall(`/admin/tag-config/types/${tagType.id}`, {
            method: 'PUT',
            headers,
            body: JSON.stringify(tagType),
        })
        setIsLoading(false)
        return result
    }

    const deleteTagType = async (tagType: TagType) => {
        setIsLoading(true)
        await apiCall(`/admin/tag-config/types/${tagType.id}`, {
            method: 'DELETE'
        })
        setIsLoading(false)
    }

    const createTag = async (tagName: string, tagTypeId: number) => {
        setIsLoading(true)
        await apiCall(`/admin/tag-config/types/${tagTypeId}/tags`, {
            method: 'POST',
            body: tagName,
        })
        setIsLoading(false)
    }

    const updateTag = async (tag: Tag) => {
        setIsLoading(true)
        await apiCall(`/admin/tag-config/types/${tag.tagTypeId}/tags/${tag.id}`, {
            method: 'PUT',
            body: tag.name,
        })
        setIsLoading(false)
    }

    const deleteTag = async (tag: Tag) => {
        setIsLoading(true)
        await apiCall(`/admin/tag-config/types/${tag.tagTypeId}/tags/${tag.id}`, {
            method: 'DELETE'
        })
        setIsLoading(false)
    }

    const onCreateTagType = async () => {
        if (newTagTypeName) {
            await createTagType(newTagTypeName)
            await fetchTagTypes()
            setNewTagTypeName('')
        }
    }

    const onUpdateTagType = async (tagType: TagType) => {
        const result = await updateTagType(tagType)
        await fetchTagTypes()
        return result
    }

    const onDeleteTagType = async (tagType: TagType) => {
        setIsLoading(true)
        await deleteTagType(tagType)
        await fetchTagTypes()
        setIsLoading(false)
    }

    const onCreateTag = async (tagName: string, tagTypeId: number) => {
        await createTag(tagName, tagTypeId)
        await fetchTagTypes()
    }

    const onUpdateTag = async (tag: Tag) => {
        await updateTag(tag)
        await fetchTagTypes()
    }

    const onDeleteTag = async (tag: Tag) => {
        await deleteTag(tag)
        await fetchTagTypes()
    }

    useEffect(() => {
        fetchTagTypes()
    }, [])

    return (
        <>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('tags:tag_types_and_values')}
            </Typography.Title>
            <Trans
                components={[<b />, <p />]}
                i18nKey="tags:tag_type_description"
            />
            <ul>
                <li>
                    <Trans
                        components={[<b />]}
                        i18nKey="tags:tag_type_instruction_p1"
                    />
                </li>
                <li>
                    <Trans
                        components={[<b />]}
                        i18nKey="tags:tag_type_instruction_p2"
                    />
                </li>
            </ul>
            <p>
                {t('tags:tag_type_auto_save_notice')}
            </p>
            <TagTable
                createTag={onCreateTag}
                deleteTag={onDeleteTag}
                deleteTagType={onDeleteTagType}
                isLoading={isLoading}
                tagTypes={tagTypes}
                updateTag={onUpdateTag}
                updateTagType={onUpdateTagType}
            />
            <Row justify="end" style={{ marginTop: 20 }}>
                <Input
                    name="tag-type"
                    onBlur={onCreateTagType}
                    onChange={(e) => setNewTagTypeName(e.target.value)}
                    onPressEnter={onCreateTagType}
                    placeholder={t('tags:tag_input_placeholder')}
                    style={{ width: 400 }}
                    value={newTagTypeName}
                />
            </Row>
            <Divider />
            <Typography.Title level={4}>
                {t('tags:tags_from_a_project_name')}
            </Typography.Title>
            <p>
                {t('tags:tag_project_instruction_p1')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p2')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p3')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p4')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p5')}
            </p>
            <p>
                <b>{t('tags:example') + ':'}</b>
            </p>
            <p>
                <Trans
                    components={[<b />]}
                    i18nKey="tags:example_template"
                />
            </p>
            <p>
                {t('tags:project_name_templates') + ':'}
            </p>
            <Row>
                <TextArea style={{ width: 400, height: 100 }} />
            </Row>
            <Row justify="end">
                <Button style={{ marginTop: 20, marginRight: 15 }}>{t('tags:save_templates')}</Button>
                <Button style={{ marginTop: 20, marginRight: 15 }}>{t('tags:fill_tags_for_project')}</Button>
            </Row>
        </>
    )
}
