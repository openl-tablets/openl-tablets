import React, { FC, useEffect, useRef, useState } from 'react'
import { Input, InputRef } from 'antd'

interface TagTableNameCellProps {
    name: string;
    onChange: (name: string) => boolean | Promise<boolean>;
}

export const TagTableNameCell: FC<TagTableNameCellProps> = ({ name, onChange }) => {
    const inputRef = useRef<InputRef>(null)
    const [editing, setEditing] = useState(false)
    const [value, setValue] = useState(() => name)

    const toggleEdit = () => {
        setEditing(!editing)
    }

    useEffect(() => {
        if (editing) {
            inputRef.current!.focus()
        }
    }, [editing])

    const handleChangeName = async (nextName: string) => {
        const result = await onChange(nextName)
        if (!result) {
            setValue(name)
        }
    }

    const onChangeName = (e: React.ChangeEvent<HTMLInputElement>) => {
        setValue(e.target.value)
    }

    return editing ? (
        <div>
            <Input
                ref={inputRef}
                onChange={onChangeName}
                value={value}
                onBlur={() => {
                    toggleEdit()
                    handleChangeName(value)
                }}
                onPressEnter={() => {
                    toggleEdit()
                    handleChangeName(value)
                }}
            />
        </div>
    ) : (
        <div className="editable-cell-wrap" onClick={toggleEdit} style={{ paddingRight: 24 }}>
            {value}
        </div>
    )
}
