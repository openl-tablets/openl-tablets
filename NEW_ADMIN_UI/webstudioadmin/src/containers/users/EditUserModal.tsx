import React, {
    useState, useEffect, useRef, useMemo,
} from 'react'
import { Button, Row, Select } from 'antd'
import { Form as FinalForm, Field, FormSpy } from 'react-final-form'
import arrayMutators from 'final-form-arrays'
import { Input, CheckboxGroup } from '../../components/form'
import './NewUserModal.scss'
import { FormApi } from 'final-form'
import { apiCall } from '../../services'

const DISPLAY_NAME_FIRST_LAST = 'firstLast'
const DISPLAY_NAME_LAST_FIRST = 'lastFirst'
const DISPLAY_NAME_OTHER = 'other'

const displayNameOptions = [
    {
        value: DISPLAY_NAME_FIRST_LAST,
        label: 'First last',
    },
    {
        value: DISPLAY_NAME_LAST_FIRST,
        label: 'Last first',
    },
    {
        value: DISPLAY_NAME_OTHER,
        label: 'Other',
    },
]

interface EditUserProps {
  user: {
    email: string;
    displayName: string;
    firstName: string;
    lastName: string;
    password: string;
    groups: string[];
    username: string;
    internalPassword: {
      password: string;
    };
    currentUser: boolean;
    superUser: boolean;
    unsafePassword: boolean;
    externalFlags: {
      firstNameExternal: boolean;
      displayNameExternal: boolean;
      emailExternal: boolean;
      lastNameExternal: boolean;
      emailVerified: boolean;
    };
    notMatchedExternalGroupsCount: number;
    online: boolean;
    userGroups: {
      name: string;
      type: 'ADMIN' | 'DEFAULT' | 'EXTERNAL';
    }[];
  };
  updateUser: (updatedUser: any) => void;
  onSave: () => void;
}

export const EditUserModal: React.FC<EditUserProps> = ({ user, updateUser, onSave }) => {
    const [ groupNames, setGroupNames ] = useState<string[]>([])
    const [ isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled ] = useState<boolean>(true)
    const formRef = useRef<FormApi>()

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            if (user.displayName === `${user.firstName} ${user.lastName}`) {
                return DISPLAY_NAME_FIRST_LAST
            }
            if (user.displayName === `${user.lastName} ${user.firstName}`) {
                return DISPLAY_NAME_LAST_FIRST
            }
            return DISPLAY_NAME_OTHER
        }

        return {
            username: user.username,
            email: user.email,
            password: user.password,
            firstName: user.firstName,
            lastName: user.lastName,
            displayName: user.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
            groups: user.userGroups.map((group) => group.name)
        }
    }, [ user ])

    const fetchGroupData = async () => {
        try {
            const response = await apiCall('/admin/management/groups')
            if (response.ok) {
                const responseObject = await response.json()
                const names = Object.keys(responseObject)
                setGroupNames(names)
            } else {
                console.error('Failed to fetch groups:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching groups:', error)
        }
    }

    useEffect(() => {
        fetchGroupData()
    }, [])

    const handleSubmit = async (values: any) => {
        const {
            email, displayName, firstName, lastName, password, groups, username
        } = values

        const updatedUser = {
            email,
            displayName,
            firstName,
            lastName,
            password,
            groups,
        }

        try {
            const response = await apiCall(`/users/${username}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updatedUser),
            })

            if (response.status === 204) {
                updateUser(updatedUser)
                onSave()
            }
        } catch (error) {
            console.error('Error updating user:', error)
        }
    }

    // TODO: values is object with all form values. create a type for it
    const displayNameSetter = (values: any, form: FormApi) => {
        if (values.displayNameSelect === DISPLAY_NAME_OTHER) {
            setIsDisplayNameFieldDisabled(false)
        } else {
            if (values.displayNameSelect === DISPLAY_NAME_FIRST_LAST) {
                form.change('displayName', `${values.firstName} ${values.lastName}`)
            } else {
                form.change('displayName', `${values.lastName} ${values.firstName}`)
            }
            setIsDisplayNameFieldDisabled(true)
        }

        return null
    }

    return (
        <div>
            <FinalForm
                initialValues={initialValues}
                mutators={{ ...arrayMutators }}
                onSubmit={handleSubmit}
            >
                {({ handleSubmit, form }) => {
                    formRef.current = form
                    return (
                        <form onSubmit={handleSubmit}>
                            <br />
                            <label>
                                <b>Account</b>
                            </label>
                            <div className="user-label">
                                <label>Username</label>
                                <Input disabled name="username" />
                            </div>
                            <div className="user-label">
                                <label>Email</label>
                                <Input name="email" />
                            </div>
                            <div className="user-label">
                                <label>Password</label>
                                <Input name="password" type="password" />
                            </div>
                            <br />
                            <label>
                                <b>Name</b>
                            </label>
                            <div className="user-label">
                                <label htmlFor="firstName">First name (given name)</label>
                                <Input name="firstName" />
                            </div>
                            <div className="user-label">
                                <label>Last name (family name)</label>
                                <Input name="lastName" />
                            </div>
                            <div className="user-label">
                                <label>Display name:</label>
                                <div>
                                    <Field name="displayNameSelect">
                                        {({ input }) => (
                                            <Select
                                                options={displayNameOptions}
                                                style={{ width: 100 }}
                                                {...input}
                                            />
                                        )}
                                    </Field>
                                </div>
                            </div>
                            <div className="user-label">
                                <Input disabled={isDisplayNameFieldDisabled} name="displayName" />
                            </div>
                            <br />
                            <label>
                                <b>Groups</b>
                            </label>
                            <CheckboxGroup name="groups" options={groupNames} />
                            <div style={{ display: 'grid', justifyContent: 'end' }}>
                                <Row>
                                    <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: 'green', borderColor: 'green' }}>
                                        Update
                                    </Button>
                                </Row>
                            </div>
                            <FormSpy subscription={{ values: true }}>
                                {({ values }) => displayNameSetter(values, form)}
                            </FormSpy>
                        </form>
                    )
                }}
            </FinalForm>

        </div>
    )
}
