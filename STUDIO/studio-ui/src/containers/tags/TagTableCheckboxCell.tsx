import React, { FC } from 'react'
import { Checkbox } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'

interface TagTableCheckboxCellProps {
    isChecked: boolean;
    onChange: (e: CheckboxChangeEvent) => void;
}

export const TagTableCheckboxCell: FC<TagTableCheckboxCellProps> = ({ isChecked, onChange }) => {
    return (
        <Checkbox checked={isChecked} onChange={onChange} />
    )
}
