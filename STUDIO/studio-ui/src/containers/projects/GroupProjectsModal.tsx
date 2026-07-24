import { useEffect, useState } from 'react'
import { Modal, Select } from 'antd'
import { useTranslation } from 'react-i18next'
import { FieldRow } from '../../components/FieldRow'
import { GROUP_BY_NONE, GROUP_BY_REPOSITORY, type GroupingLevels } from './projectGrouping'

const LABEL_WIDTH = 90

/** The three grouping slots, named so each row keys by its slot rather than a list position. */
const LEVEL_SLOTS = ['first', 'second', 'third'] as const

interface GroupProjectsModalProps {
    open: boolean
    levels: GroupingLevels
    /** The tag types a level can group by; the repository is always offered. */
    tagTypes: string[]
    onApply: (levels: GroupingLevels) => void
    onClose: () => void
}

/**
 * Picks what the project tree groups by: up to three levels, each one the repository or a tag type —
 * the same choice the grouped tree of earlier versions offered. A level set to none ends the grouping.
 */
export const GroupProjectsModal = ({ open, levels, tagTypes, onApply, onClose }: GroupProjectsModalProps) => {
    const { t } = useTranslation('repository')
    const [draft, setDraft] = useState<GroupingLevels>(levels)

    useEffect(() => {
        if (open) {
            setDraft(levels)
        }
    }, [levels, open])

    const setLevel = (index: number, value: string) => setDraft(previous => {
        const next = [...previous] as GroupingLevels
        next[index] = value
        // A level that groups by nothing ends the grouping: the levels below it follow it.
        for (let below = index + 1; below < next.length; below++) {
            if (!value || next[below] === value) {
                next[below] = GROUP_BY_NONE
            }
        }
        return next
    })

    const optionsFor = (index: number) => [
        { value: GROUP_BY_NONE, label: t('home.tree.level_none') },
        { value: GROUP_BY_REPOSITORY, label: t('home.tree.level_repository') },
        ...tagTypes.map(type => ({ value: type, label: type })),
    ].filter(option => !option.value || !draft.some((level, at) => at !== index && level === option.value))

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ 'data-testid': 'grouping-apply' }}
            okText={t('home.tree.apply')}
            onCancel={onClose}
            onOk={() => onApply(draft)}
            open={open}
            title={t('home.tree.group_title')}
        >
            {LEVEL_SLOTS.map((slot, index) => (
                <FieldRow key={slot} label={t('home.tree.level', { level: index + 1 })} labelWidth={LABEL_WIDTH}>
                    <Select
                        data-testid={`grouping-level-${index + 1}`}
                        disabled={index > 0 && !draft[index - 1]}
                        onChange={value => setLevel(index, value as string)}
                        options={optionsFor(index)}
                        style={{ width: '100%' }}
                        value={draft[index] ?? GROUP_BY_NONE}
                    />
                </FieldRow>
            ))}
        </Modal>
    )
}
