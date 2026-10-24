import { App, Input, Divider, Button, Row, Typography } from 'antd'
import TextArea from 'antd/es/input/TextArea'
import React, { useEffect, useMemo, useState } from 'react'
import { Trans, useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { defineTags, getProjectTagCounts } from '../services/repositories'
import type { TagFacetSummary } from '../types/projects'
import { Tag, TagTable, TagType } from './tags/TagTable'
import { useStyles } from './tags/TagTable.styles'
import { FillTagsModal } from './tags/FillTagsModal'
import { TagTypesFoundInProjects, type DefineTags } from './tags/TagsFoundInProjects'
import { tagsInProjects } from './tags/tagsInProjects'


export const Tags: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const [tagTypes, setTagTypes] = useState<TagType[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [newTagTypeName, setNewTagTypeName] = useState('')
    const [templates, setTemplates] = useState('')
    const [isSavingTemplates, setIsSavingTemplates] = useState(false)
    const [isFillingTags, setIsFillingTags] = useState(false)
    const [isPreviewOpen, setIsPreviewOpen] = useState(false)
    // The tags the projects carry. Nothing is found while they are not read, or when they cannot be.
    const [carried, setCarried] = useState<TagFacetSummary[]>([])
    const { styles } = useStyles()
    const found = useMemo(() => tagsInProjects(tagTypes, carried), [tagTypes, carried])

    const fetchCarriedTags = async () => setCarried(await getProjectTagCounts().catch(() => []))

    const fetchTagTypes = async () => {
        setIsLoading(true)
        try {
            const response = await apiCall('/admin/tag-config/types')
            if (Array.isArray(response)) {
                setTagTypes(response)
            }
        } finally {
            setIsLoading(false)
        }
    }

    const fetchTemplates = async () => {
        const response = await apiCall('/admin/tag-config/templates')
        if (Array.isArray(response)) {
            setTemplates(response.join('\n'))
        }
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

    // The new order shows at once, and the catalog is read again once it is stored. A refused order, say a tag type
    // another administrator deleted meanwhile, is said in a message and the order stored before comes back.
    const onReorderTagTypes = async (ordered: TagType[]) => {
        setTagTypes(ordered)
        setIsLoading(true)
        try {
            await apiCall('/admin/tag-config/types/order', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ordered.map(tagType => tagType.id)),
            }, LOCAL_LOAD_API_OPTIONS)
        } catch (e) {
            notification.error({ title: e instanceof Error ? e.message : t('tags:reorder_error') })
        } finally {
            await fetchTagTypes()
        }
    }

    // The tags found in projects go to the configuration; the projects keep them, so only the catalog is read again.
    const onDefineTags = async (type: string, values: string[]) => {
        setIsLoading(true)
        try {
            await defineTags({ [type]: values })
        } catch (e) {
            notification.error({ title: e instanceof Error ? e.message : t('tags:found_add_error') })
        } finally {
            await fetchTagTypes()
        }
    }

    const defineFound: DefineTags = (type, values) => void onDefineTags(type, values)

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

    const saveTemplatesRequest = async () => {
        const templateList = templates.split('\n').filter(line => line.trim() !== '')
        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        await apiCall('/admin/tag-config/templates', {
            method: 'PUT',
            headers,
            body: JSON.stringify(templateList),
        }, { throwError: true })
    }

    const onSaveTemplates = async () => {
        setIsSavingTemplates(true)
        try {
            await saveTemplatesRequest()
            notification.success({ title: t('tags:templates_saved') })
        } catch (e) {
            notification.error({ title: e instanceof Error ? e.message : t('tags:templates_save_error') })
        } finally {
            setIsSavingTemplates(false)
        }
    }

    // The templates the user is looking at decide what the preview shows, so they are saved first.
    const onFillTagsForProject = async () => {
        setIsFillingTags(true)
        try {
            await saveTemplatesRequest()
            setIsPreviewOpen(true)
        } catch (e) {
            notification.error({ title: e instanceof Error ? e.message : t('tags:fill_tags_error') })
        } finally {
            setIsFillingTags(false)
        }
    }

    useEffect(() => {
        void fetchTagTypes()
        void fetchTemplates()
        void fetchCarriedTags()
    }, [])

    return (
        <>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('tags:tag_types_and_values')}
            </Typography.Title>
            <Trans
                components={[<b key="b" />, <p key="p" />]}
                i18nKey="tags:tag_type_description"
            />
            <ul>
                <li>
                    <Trans
                        components={[<b key="b" />]}
                        i18nKey="tags:tag_type_instruction_p1"
                    />
                </li>
                <li>
                    <Trans
                        components={[<b key="b" />]}
                        i18nKey="tags:tag_type_instruction_p2"
                    />
                </li>
                <li>
                    {t('tags:tag_type_order_notice')}
                </li>
                <li>
                    {t('tags:tag_found_notice')}
                </li>
            </ul>
            <p>
                {t('tags:tag_type_auto_save_notice')}
            </p>
            <TagTable
                createTag={onCreateTag}
                defineTags={defineFound}
                deleteTag={onDeleteTag}
                deleteTagType={onDeleteTagType}
                found={found.values}
                isLoading={isLoading}
                reorderTagTypes={ordered => void onReorderTagTypes(ordered)}
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
            <TagTypesFoundInProjects
                onDefine={defineFound}
                styles={styles}
                types={found.types}
            />
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
                    components={[<b key="b" />]}
                    i18nKey="tags:example_template"
                />
            </p>
            <p>
                {t('tags:project_name_templates') + ':'}
            </p>
            <Row>
                <TextArea
                    onChange={(e) => setTemplates(e.target.value)}
                    style={{ width: 400, height: 100 }}
                    value={templates}
                />
            </Row>
            <Row justify="end">
                <Button
                    loading={isSavingTemplates}
                    onClick={onSaveTemplates}
                    style={{ marginTop: 20, marginRight: 15 }}
                >
                    {t('tags:save_templates')}
                </Button>
                <Button
                    loading={isFillingTags}
                    onClick={onFillTagsForProject}
                    style={{ marginTop: 20, marginRight: 15 }}
                >
                    {t('tags:fill_tags_for_project')}
                </Button>
            </Row>
            {/* Filling may create tag values, so the catalog is read again. */}
            <FillTagsModal
                onClose={() => setIsPreviewOpen(false)}
                open={isPreviewOpen}
                onFilled={() => {
                    void fetchTagTypes()
                    void fetchCarriedTags()
                }}
            />
        </>
    )
}
