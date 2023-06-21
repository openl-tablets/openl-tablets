import React, { FC, useRef } from 'react'
import { FieldArray } from 'react-final-form-arrays'
import { Checkbox as AntdCheckbox, Col, Row } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'

type CheckboxProps = {
  name: string,
  option: string,
  fields: any
}

type CheckboxGroupProps = {
  name: string,
  options: string[]
}
const CheckboxGroup: FC<CheckboxGroupProps> = ({
  name,
  options = [],
}) => {
  return (
    <FieldArray name={name}>
      {({ fields }) => {
          return (
              <Row>
                {options.map((option) => (
                  <Checkbox
                    name={name}
                    key={option}
                    fields={fields}
                    option={option}
                  />
                )
                )}

              </Row>
          )}}

    </FieldArray>
  )}


const Checkbox: FC<CheckboxProps> = ({ name, fields, option }) => {
  const isChecked = useRef(fields.value.includes(option));

  const toggleCheckbox = (event: CheckboxChangeEvent) => {
    if (event.target.checked) {
      fields.push(option);
    }
    else {
      fields.remove(option);
    }
  }

  return (
      <Col span={8} key={option}>
        <AntdCheckbox name={name} value={option} defaultChecked={isChecked.current} onChange={toggleCheckbox}>{option}</AntdCheckbox>
      </Col>
  )
}

export default CheckboxGroup