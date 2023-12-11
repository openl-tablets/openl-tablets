import React, { FC, useMemo } from 'react'
import { FieldArray } from 'react-final-form-arrays'
import { Checkbox as AntdCheckbox, Col, Row } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'

type CheckboxProps = {
  name: string,
  option: string,
  fields: any
};

type CheckboxGroupProps = {
  name: string,
  options: string[]
};
const CheckboxGroup: FC<CheckboxGroupProps> = ({
    name,
    options = [],
}) => (
    <FieldArray name={name}>
        {({ fields }) => (
            <Row>
                {options.map((option) => (
                    <Checkbox
                        key={option}
                        fields={fields}
                        name={name}
                        option={option}
                    />
                ))}

            </Row>
        )}

    </FieldArray>
)

const Checkbox: FC<CheckboxProps> = ({ name, fields, option }) => {
    const isChecked = useMemo(() => fields.value.includes(option), [ fields.value, option ])

    const toggleCheckbox = (event: CheckboxChangeEvent) => {
        if (event.target.checked) {
            fields.push(option)
        } else {
            const optionIndex = fields.value.indexOf(option)
            if (optionIndex > -1) {
                fields.remove(optionIndex)
            }
        }
    }

    return (
        <Col key={option} span={8}>
            <AntdCheckbox checked={isChecked} name={name} onChange={toggleCheckbox}>
                {option}
            </AntdCheckbox>
        </Col>
    )
}

export default CheckboxGroup
