import React from 'react'
import { render, screen } from '@testing-library/react'
import { Form } from 'antd'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

jest.mock('components', () => ({
    Select: ({ label, name, mode, options, ...rest }: any) => (
        <div data-testid={`select-${name}`}>
            <label>{label}</label>
            {mode === 'tags' ? (
                <input data-testid={`input-${name}`} name={name} />
            ) : (
                <select data-testid={`dropdown-${name}`} name={name}>
                    {options?.map((opt: any) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
            )}
        </div>
    ),
}))

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { InitialUsers } = require('containers/security/InitialUsers')

const userGroups = [
    { label: 'None', value: '' },
    { label: 'Admins', value: 'Admins' },
    { label: 'Viewers', value: 'Viewers' },
]

const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [form] = Form.useForm()
    return <Form form={form}>{children}</Form>
}

describe('InitialUsers', () => {
    it('renders administrators field', () => {
        render(
            <Wrapper>
                <InitialUsers userGroups={userGroups} />
            </Wrapper>
        )
        expect(screen.getByText('security:administrators')).toBeInTheDocument()
    })

    it('renders default group select when showDefaultGroup is true', () => {
        render(
            <Wrapper>
                <InitialUsers showDefaultGroup={true} userGroups={userGroups} />
            </Wrapper>
        )
        expect(screen.getByText('security:default_group')).toBeInTheDocument()
        expect(screen.getByTestId('select-defaultGroup')).toBeInTheDocument()
    })

    it('hides default group select when showDefaultGroup is false', () => {
        render(
            <Wrapper>
                <InitialUsers showDefaultGroup={false} userGroups={userGroups} />
            </Wrapper>
        )
        expect(screen.queryByTestId('select-defaultGroup')).not.toBeInTheDocument()
    })

    it('renders divider and description text', () => {
        render(
            <Wrapper>
                <InitialUsers userGroups={userGroups} />
            </Wrapper>
        )
        expect(screen.getByText('security:configure_initial_users')).toBeInTheDocument()
        expect(screen.getByText('security:configure_initial_users_info')).toBeInTheDocument()
    })

    it('renders user group options in default group dropdown', () => {
        render(
            <Wrapper>
                <InitialUsers showDefaultGroup={true} userGroups={userGroups} />
            </Wrapper>
        )
        const dropdown = screen.getByTestId('dropdown-defaultGroup')
        const options = dropdown.querySelectorAll('option')
        expect(options).toHaveLength(3)
        expect(options[0]).toHaveTextContent('None')
        expect(options[1]).toHaveTextContent('Admins')
        expect(options[2]).toHaveTextContent('Viewers')
    })
})
