import { Input, Divider, Button, Row, Typography } from 'antd'
import TextArea from 'antd/es/input/TextArea'
import React, { useEffect, useState } from 'react'
import { Trans, useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { TagTypeList } from './tags/TagTypeList'
import { TagType } from './tags/TagTypeCard'
import { Tag } from './tags/TagList'


export const Tags: React.FC = () => {
    const { t } = useTranslation()
    const [ tagTypes, setTagTypes ] = useState<TagType[]>([])

    const fetchTagTypes = async () => {
        try {
            const response = await apiCall('/admin/tag-config/types')
            if (response.ok) {
                const responseObject = await response.json()
                setTagTypes(responseObject)
            } else {
                console.error('Failed to fetch tags:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching tags:', error)
        }
    }

    const updateTagType = async (tagType: TagType) => {
        try {
            const headers = new Headers()
            headers.append('Content-Type', 'application/json')
            // headers.append('Accept', 'application/json')

            const response = await apiCall(`/admin/tag-config/types/${tagType.id}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify(tagType),
            })
            if (response.ok) {
                const responseObject = await response.json()
                console.warn(responseObject)
            } else {
                console.error('Failed to create tag:', response.statusText)
            }
        } catch (error) {
            console.error('Error creating tag:', error)
        }
    }

    const createTag = async (tagName: string, tagTypeId: number) => {
        try {
            const response = await apiCall(`/admin/tag-config/types/${tagTypeId}/tags`, {
                method: 'POST',
                body: tagName,
            })
            if (response.ok) {
                const responseObject = await response.json()
                console.warn(responseObject)
            } else {
                console.error('Failed to create tag:', response.statusText)
            }
        } catch (error) {
            console.error('Error creating tag:', error)
        }
    }

    const updateTag = async (tag: Tag) => {
        try {
            const response = await apiCall(`/admin/tag-config/types/${tag.tagTypeId}/tags/${tag.id}`, {
                method: 'PUT',
                body: tag.name,
            })
            if (!response.ok) {
                console.error('Failed to update tag:', response.statusText)
            }
        } catch (error) {
            console.error('Error updating tag:', error)
        }
    }

    const deleteTag = async (tag: Tag) => {
        try {
            const response = await apiCall(`/admin/tag-config/types/${tag.tagTypeId}/tags/${tag.id}`, {
                method: 'DELETE'
            })
            if (!response.ok) {
                console.error('Failed to delete tag:', response.statusText)
            }
        } catch (error) {
            console.error('Error deleting tag:', error)
        }
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
            <Typography.Title level={4}>
                {t('tags:tag_types_and_values')}
            </Typography.Title>
            <Trans
                components={[ <b />, <p /> ]}
                i18nKey="tags:tag_type_description"
            />
            <ul>
                <li>
                    <Trans
                        components={[ <b /> ]}
                        i18nKey="tags:tag_type_instruction_p1"
                    />
                </li>
                <li>
                    <Trans
                        components={[ <b /> ]}
                        i18nKey="tags:tag_type_instruction_p2"
                    />
                </li>
            </ul>
            <p>
                {t('tags:tag_type_auto_save_notice')}
            </p>
            <TagTypeList createTag={onCreateTag} deleteTag={onDeleteTag} tagTypes={tagTypes} updateTag={onUpdateTag} updateTagType={updateTagType} />
            <Input name="tag" placeholder={t('tags:tag_input_placeholder')} style={{ width: 400 }} />
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
                    components={[ <b /> ]}
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
