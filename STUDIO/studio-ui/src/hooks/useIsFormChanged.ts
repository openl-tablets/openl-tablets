import { useEffect, useState } from 'react'
import { Form } from 'antd'

interface UseIsFormChangedProps {
    form: any
}

export const useIsFormChanged = ({ form }: UseIsFormChangedProps) => {
    const [isFormChanged, setIsFormChanged] = useState(false)
    const watchedFields = Form.useWatch([], form)

    useEffect(() => {
        if (!form || !watchedFields) {
            setIsFormChanged(false)
            return
        }

        // Use Ant Design's built-in isFieldsTouched method
        const isTouched = form.isFieldsTouched()
        setIsFormChanged(isTouched)
    }, [watchedFields, form])

    return isFormChanged
}
