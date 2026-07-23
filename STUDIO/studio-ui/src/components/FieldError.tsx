import { Typography } from 'antd'

interface FieldErrorProps {
    /** The message to show; nothing is rendered when the field is acceptable. */
    message?: string | null
    testId: string
}

/** The rejection message of a field validated as the user types, shown right under the input. */
export const FieldError = ({ message, testId }: FieldErrorProps) => {
    if (!message) {
        return null
    }
    return (
        <Typography.Text data-testid={testId} style={{ fontSize: 12 }} type="danger">
            {message}
        </Typography.Text>
    )
}
