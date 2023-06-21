import React, {FC} from 'react'
import { Field } from 'react-final-form'
import { Input as AntdInput } from 'antd';

type InputProps = {
    name: string,
    type?: string
    disabled?: boolean
}

const Input: FC<InputProps> = ({
    name,
    type= 'text',
    disabled
}) => {
    return (
      <Field name={name} type={type}>
          {({ input }) => <AntdInput {...input} disabled={disabled} />}
      </Field>
    )
}

export default Input