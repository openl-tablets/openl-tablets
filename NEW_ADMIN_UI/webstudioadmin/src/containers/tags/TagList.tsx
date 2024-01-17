import React, { useEffect, useRef, useState } from 'react'
import { Input, InputRef, Space, Tag as AntTag, TagType, Tooltip } from 'antd'
import { PlusOutlined } from '@ant-design/icons'

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

interface TagListProps extends TagActions {
    tags: Tag[];
    tagTypeId: number;
}

export const TagList: React.FC<TagListProps> = ({ tags, tagTypeId, createTag, updateTag, deleteTag }) => {
    const [ inputVisible, setInputVisible ] = useState(false)
    const [ inputValue, setInputValue ] = useState('')
    const [ editInputIndex, setEditInputIndex ] = useState(-1)
    const [ editInputValue, setEditInputValue ] = useState('')
    const inputRef = useRef<InputRef>(null)
    const editInputRef = useRef<InputRef>(null)

    useEffect(() => {
        if (inputVisible) {
            inputRef.current?.focus()
        }
    }, [ inputVisible ])

    useEffect(() => {
        editInputRef.current?.focus()
    }, [ editInputValue ])

    const handleClose = (removedTagId: number) => {
        const tag = tags.find((tag) => tag.id === removedTagId)
        if (tag) {
            deleteTag(tag)
        }
    }

    const showInput = () => {
        setInputVisible(true)
    }

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setInputValue(e.target.value)
    }

    const handleInputConfirm = () => {
        if (inputValue) {
            // setTags([ ...tags, inputValue ])
            createTag(inputValue, tagTypeId)
        }
        setInputVisible(false)
        setInputValue('')
    }

    const handleEditInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setEditInputValue(e.target.value)
    }

    const handleEditInputConfirm = () => {
        updateTag({ id: tags[editInputIndex].id, name: editInputValue, tagTypeId  })
        setEditInputIndex(-1)
        setEditInputValue('')
    }

    const tagInputStyle: React.CSSProperties = {
        width: 64,
        height: 22,
        marginInlineEnd: 8,
        verticalAlign: 'top',
    }

    const tagPlusStyle: React.CSSProperties = {
        height: 22,
        borderStyle: 'dashed',
    }

    return (
        <Space size={[ 0, 8 ]} wrap>
            {tags.map((tag, index) => {
                if (editInputIndex === index) {
                    return (
                        <Input
                            key={tag.id}
                            ref={editInputRef}
                            onBlur={handleEditInputConfirm}
                            onChange={handleEditInputChange}
                            onPressEnter={handleEditInputConfirm}
                            size="small"
                            style={tagInputStyle}
                            value={editInputValue}
                        />
                    )
                }
                const isLongTag = tag.name.length > 20
                const tagElem = (
                    <AntTag
                        key={tag.id}
                        closable={index !== 0}
                        onClose={() => handleClose(tag.id)}
                        style={{ userSelect: 'none' }}
                    >
                        <span
                            onDoubleClick={(e) => {
                                if (index !== 0) {
                                    setEditInputIndex(index)
                                    setEditInputValue(tag.name)
                                    e.preventDefault()
                                }
                            }}
                        >
                            {isLongTag ? `${tag.name.slice(0, 20)}...` : tag.name}
                        </span>
                    </AntTag>
                )
                return isLongTag ? (
                    <Tooltip key={tag.id} title={tag.name}>
                        {tagElem}
                    </Tooltip>
                ) : (
                    tagElem
                )
            })}
            {inputVisible ? (
                <Input
                    ref={inputRef}
                    onBlur={handleInputConfirm}
                    onChange={handleInputChange}
                    onPressEnter={handleInputConfirm}
                    size="small"
                    style={tagInputStyle}
                    type="text"
                    value={inputValue}
                />
            ) : (
                <AntTag icon={<PlusOutlined />} onClick={showInput} style={tagPlusStyle}>
                    New Tag
                </AntTag>
            )}
        </Space>
    )
}