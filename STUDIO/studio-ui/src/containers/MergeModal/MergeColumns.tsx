import React from 'react'
import { Button, Col, Divider, Flex, Row, Space, Tag, Tooltip, Typography } from 'antd'
import { DoubleLeftOutlined, DoubleRightOutlined, RollbackOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { Side } from './textMerge'

/** The span of each of the three columns, as merge tools lay them out: your version, the result, their version. */
const SPAN = 8
const GUTTER = 8

/** What each of the three columns of a merge window holds: your version, the result and their version. */
interface ColumnsProps {
    ours?: React.ReactNode
    result?: React.ReactNode
    theirs?: React.ReactNode
}

/** Three cells side by side, in the columns of a merge window: your version, the result and their version. */
export const Columns: React.FC<ColumnsProps> = ({ ours, result, theirs }) => (
    <Row gutter={GUTTER}>
        <Col span={SPAN}>{ours}</Col>
        <Col span={SPAN}>{result}</Col>
        <Col span={SPAN}>{theirs}</Col>
    </Row>
)

/**
 * The buttons of a conflict, each in the column of the version it takes. A button of your version stands at the
 * right of its column and a button of their version at the left, next to the result they point to.
 */
export const ButtonColumns: React.FC<ColumnsProps> = ({ ours, result, theirs }) => (
    <Columns
        ours={<Flex justify="end">{ours}</Flex>}
        result={<Flex justify="center">{result}</Flex>}
        theirs={<Flex justify="start">{theirs}</Flex>}
    />
)

interface ConflictHeadingProps {
    title: React.ReactNode
    /** How the conflict is resolved, or none while it is still to be resolved. */
    status?: string | undefined
    /** What is left to resolve, told in place of Not Resolved while the conflict is still to be resolved. */
    pending?: string | undefined
    /** The identifier of the title, for the conflict to be named by it. */
    titleId?: string | undefined
    /** What else the heading offers, such as a menu of what the conflict can be resolved with. */
    extra?: React.ReactNode
    /** What leads the heading, such as the way to the other conflicts. */
    lead?: React.ReactNode
}

/** The line that heads a conflict: what the conflict is, and how it is resolved or what is still to be resolved. */
export const ConflictHeading: React.FC<ConflictHeadingProps> = ({ title, status, pending, titleId, extra, lead }) => {
    const { t } = useTranslation()
    return (
        <Divider size="small" titlePlacement="start">
            <Space size="small">
                {lead}
                <span id={titleId}>{title}</span>
                {status
                    ? <Tag color="success">{status}</Tag>
                    : <Tag color="warning">{pending ?? t('merge:editor.unresolved')}</Tag>}
                {extra}
            </Space>
        </Divider>
    )
}

/** The title of a column, with what names its version, or a hint on the result. */
const ColumnTitle: React.FC<{ label: string; version?: React.ReactNode }> = ({ label, version }) => (
    <Space wrap size="small">
        <Typography.Text strong>{label}</Typography.Text>
        {version}
    </Space>
)

interface ColumnTitlesProps {
    versionLabels?: Partial<Record<Side, React.ReactNode>> | undefined
    /** What the result lets the reader do, told beside its title. */
    resultHint?: React.ReactNode
}

/** The titles of the three columns of a merge window, each version with what names it, such as its branch. */
export const ColumnTitles: React.FC<ColumnTitlesProps> = ({ versionLabels, resultHint }) => {
    const { t } = useTranslation()
    return (
        <Columns
            ours={<ColumnTitle label={t('merge:editor.your_version')} version={versionLabels?.ours} />}
            result={<ColumnTitle label={t('merge:editor.result')} version={resultHint} />}
            theirs={<ColumnTitle label={t('merge:editor.their_version')} version={versionLabels?.theirs} />}
        />
    )
}

interface TakeButtonProps {
    side: Side
    label: string
    disabled: boolean
    onTake: () => void
    testId: string
}

/** The button that takes a version into the result, pointing from the column of the version to the result. */
export const TakeButton: React.FC<TakeButtonProps> = ({ side, label, disabled, onTake, testId }) => (
    <Button
        data-testid={testId}
        disabled={disabled}
        icon={side === 'ours' ? <DoubleRightOutlined /> : <DoubleLeftOutlined />}
        iconPlacement={side === 'ours' ? 'end' : 'start'}
        onClick={onTake}
        size="small"
    >
        {label}
    </Button>
)

interface TakeBaseButtonProps {
    /** What keeping the base version keeps, told when the reader points at the button. */
    hint: string
    disabled: boolean
    onTake: () => void
    testId: string
}

/** The button that takes the base version into the result, without the changes of either branch. */
export const TakeBaseButton: React.FC<TakeBaseButtonProps> = ({ hint, disabled, onTake, testId }) => {
    const { t } = useTranslation()
    return (
        <Tooltip title={hint}>
            <Button
                data-testid={testId}
                disabled={disabled}
                icon={<RollbackOutlined />}
                onClick={onTake}
                size="small"
            >
                {t('merge:editor.take_base')}
            </Button>
        </Tooltip>
    )
}
