import React from 'react'
import { Flex, Typography } from 'antd'
import { fileOfTag } from './bundle'

/** A language as the column of the table and the page of a key head it: its name, with the file it is written in. */
export const LanguageTitle = ({ name, tag, testId }: { name: string, tag: string, testId: string }) => (
    <Flex vertical data-testid={testId}>
        <span>{name}</span>
        <Typography.Text type="secondary">
            <small>{fileOfTag(tag)}</small>
        </Typography.Text>
    </Flex>
)
