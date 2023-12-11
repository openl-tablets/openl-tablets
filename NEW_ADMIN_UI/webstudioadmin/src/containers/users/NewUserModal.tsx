import React, { useEffect, useRef, useState } from 'react'
import { Button, Checkbox, Col, Input, Modal, Row, Select } from 'antd'
import { Form, Field } from 'react-final-form'
import type { CheckboxValueType } from 'antd/es/checkbox/Group'
import './NewUserModal.scss'
import { apiCall } from '../../services'

const displayOrder = [
    {
        value: 'First last',
        label: 'First last',
    },
    {
        value: 'Last first',
        label: 'Last first',
    },
    {
        value: 'Other',
        label: 'Other',
    },
]

export const NewUserModal: React.FC<{ fetchUsers: () => void }> = ({ fetchUsers }) => {
    const [ isModalOpen, setIsModalOpen ] = useState(false)
    const [ groupNames, setGroupNames ] = useState<string[]>([])
    const [ groups, setGroups ] = useState<CheckboxValueType[]>([])
    const [ selectedDisplayOrder, setSelectedDisplayOrder ] = useState<string>('First last')
    const formRef = useRef(null)

    const showModal = () => {
        setIsModalOpen(true)
    }

    const hideModal = () => {
        setIsModalOpen(false)
    }

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
        setGroups([])
    }, [])

    const createUser = async (values: any) => {
        try {
            const requestBody = {
                email: values.email,
                displayName: values.displayName,
                firstName: values.firstName,
                lastName: values.lastName,
                password: values.password,
                groups: Array.isArray(values.groups)
                    ? values.groups.map((group: CheckboxValueType) => group.toString())
                    : [],
                username: values.username,
                internalPassword: {
                    password: values.password,
                },
            }
            await apiCall('/users', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
            })
            setIsModalOpen(false)
            fetchUsers()
        } catch (error) {
            console.error('Error creating user:', error)
        }
    }

    const handleSubmit = async (values: any, form: any) => {
        let constructedDisplayName = ''
        if (selectedDisplayOrder === 'First last') {
            constructedDisplayName = `${values.firstName} ${values.lastName}`
        } else if (selectedDisplayOrder === 'Last first') {
            constructedDisplayName = `${values.lastName} ${values.firstName}`
        } else {
            constructedDisplayName = values.displayName
        }
        values.displayName = constructedDisplayName
        await createUser(values)
        form.reset()
        setGroups([])
    }

    return (
        <>
            <Button
                style={{ marginTop: 15, color: 'green', borderColor: 'green' }}
                onClick={showModal}
            >
                Add new user
            </Button>
            <Modal
                className="user-modal"
                footer={null}
                open={isModalOpen}
                title="Create new user"
                onCancel={hideModal}
            >
                <Form
                    ref={formRef}
                    render={({ handleSubmit, form }) => (
                        <form onSubmit={handleSubmit}>
                            <br />
                            <label>
                                <b>Account</b>
                            </label>
                            <div className="user-label">
                                <label>Username</label>
                                <Field component="input" name="username" type="text">
                                    {(props) => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <div className="user-label">
                                <label>Email</label>
                                <Field component="input" name="email" type="email">
                                    {(props) => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <div className="user-label">
                                <label>Password</label>
                                <Field component="input" name="password" type="password">
                                    {(props) => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <br />
                            <label>
                                <b>Name</b>
                            </label>
                            <div className="user-label">
                                <label>First name (given name)</label>
                                <Field component="input" name="firstName" type="text">
                                    {(props) => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <div className="user-label">
                                <label>Last name (family name)</label>
                                <Field component="input" name="lastName" type="text">
                                    {(props) => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <div className="user-label">
                                <label>Display name:</label>
                                <div>
                                    <Field component="select" name="displayName">
                                        {({ input }) => (
                                            <Select
                                                defaultActiveFirstOption
                                                style={{ width: 100 }}
                                                onChange={(value) => {input.onChange(value)}}
                                            >
                                                {displayOrder.map((option) => (
                                                    <Select.Option key={option.value} value={option.value}>
                                                        {option.label}
                                                    </Select.Option>
                                                ))}
                                            </Select>
                                        )}
                                    </Field>
                                </div>
                            </div>
                            <div className="user-label">
                                <Field form={form} name="displayName">
                                    {({ input, meta, form }) => {
                                        const { values } = form.getState()
                                        const { firstName, lastName } = values
                                        const displayOrder = values.displayName
                                        let displayNameValue = ''
                                        if (displayOrder === 'firstLast') {
                                            displayNameValue = `${firstName} ${lastName}`
                                        } else if (displayOrder === 'lastFirst') {
                                            displayNameValue = `${lastName} ${firstName}`
                                        }
                                        setSelectedDisplayOrder(displayOrder)
                                        return <Input {...input} value={displayNameValue} />
                                    }}
                                </Field>
                            </div>
                            <br />
                            <label htmlFor="groups">
                                <b>Groups</b>
                            </label>
                            <Field id="groups" name="groups" type="checkbox" value={groups} valuePropName="value">
                                {({ input }) => (
                                    <Checkbox.Group onChange={(values) => input.onChange(values)}>
                                        <Row>
                                            {groupNames.map((groupName) => (
                                                <Col key={groupName} span={8}>
                                                    <Checkbox value={groupName}>
                                                        {groupName}
                                                    </Checkbox>
                                                </Col>
                                            ))}
                                        </Row>
                                    </Checkbox.Group>
                                )}
                            </Field>
                            <div style={{ display: 'grid', justifyContent: 'end' }}>
                                <Row>
                                    <Button key="back" style={{ marginTop: 15, marginRight: 15 }} onClick={hideModal}>
                                        Cancel
                                    </Button>
                                    <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: 'green', borderColor: 'green' }}>
                                        Create
                                    </Button>
                                </Row>
                            </div>
                        </form>
                    )}
                    onSubmit={handleSubmit}
                />
            </Modal>
        </>
    )
}
