import React, { useEffect, useRef, useState } from 'react';
import { Button, Checkbox, Col, Input, Modal, Row, Select } from 'antd';
import { Form, Field } from 'react-final-form';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import './userModal.css';

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
];

interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
}

export const NewUserModal1: React.FC<{ fetchUsers: () => void }> = ({ fetchUsers }) => {
    const apiURL = 'http://localhost:8080/webstudio/rest';

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [allGroups, setAllGroups] = useState<Group[]>([]);
    const [groupNames, setGroupNames] = useState<string[]>([]);
    const [groups, setGroups] = useState<CheckboxValueType[]>([]);
    const [selectedDisplayOrder, setSelectedDisplayOrder] = useState<string>('First last');
    const [displayName, setDisplayName] = useState('');
    const formRef = useRef(null);

    const showModal = () => {
        setIsModalOpen(true);
           };

    const hideModal = () => {
        setIsModalOpen(false);
    };

    const fetchGroupData = async () => {
        try {
            const response = await fetch(`${apiURL}/admin/management/groups`, {
                headers: {
                    "Authorization": "Basic YWRtaW46YWRtaW4=",
                }
            });
            if (response.ok) {
                const responseObject = await response.json();
                setAllGroups(responseObject);
                const names = Object.keys(responseObject);
                setGroupNames(names);
            } else {
                console.error("Failed to fetch groups:", response.statusText);
            }
        } catch (error) {
            console.error("Error fetching groups:", error);
        }
    };

    useEffect(() => {
        fetchGroupData();
        setGroups([]);
    }, []);

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
            };

            await fetch(`${apiURL}/users`, {
                method: 'PUT',
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Basic YWRtaW46YWRtaW4="
                },
                body: JSON.stringify(requestBody),
            });
            setIsModalOpen(false);
            console.log("Created User:", requestBody);
            fetchUsers();
        } catch (error) {
            console.error('Error creating user:', error);
        }
    };

    const handleDisplayNameChange = (value: string) => {
        setSelectedDisplayOrder(value);
    };


    const handleSubmit = async (values: any, form: any) => {
        let constructedDisplayName = '';
        if (selectedDisplayOrder === 'First last') {
            constructedDisplayName = `${values.firstName} ${values.lastName}`;
        } else if (selectedDisplayOrder === 'Last first') {
            constructedDisplayName = `${values.lastName} ${values.firstName}`;
        } else {
            constructedDisplayName = values.displayName;
        }
        values.displayName = constructedDisplayName;
        await createUser(values);
        form.reset();
        setGroups([]);
    };

    return (
        <>
            <Button
                onClick={showModal}
                style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                Add new user
            </Button>
            <Modal
                className='user-modal'
                title="Create new user"
                open={isModalOpen}
                onCancel={hideModal}
                footer={null}
            >
                <Form
                    ref={formRef}
                    onSubmit={handleSubmit}
                    render={({ handleSubmit, form }) => (
                        <form onSubmit={handleSubmit}>
                            <br></br>

                            <label><b>Account</b></label>
                            <div className="user-label">
                                <label>Username</label>
                                <Field name="username" component="input" type="text">
                                    {props => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}</Field>
                            </div>
                            <div className="user-label">
                                <label>Email</label>
                                <Field name="email" component="input" type="email" >
                                    {props => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}</Field>
                            </div>
                            <div className="user-label">
                                <label>Password</label>
                                <Field name="password" component="input" type="password">
                                    {props => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <br></br>
                            <label><b>Name</b></label>
                            <div className="user-label">
                                <label >First name (given name)</label>
                                <Field name="firstName" component="input" type="text">
                                    {props => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>
                            <div className="user-label">
                                <label >Last name (family name)</label>
                                <Field name="lastName" component="input" type="text">
                                    {props => (
                                        <div>
                                            <Input {...props.input} />
                                        </div>
                                    )}
                                </Field>
                            </div>

                            <div className="user-label">
                                <label >Display name:</label>
                                <div>
                                    <Select
                                        defaultValue={selectedDisplayOrder}
                                        onChange={handleDisplayNameChange}
                                    >
                                        {displayOrder.map(option => (
                                            <Select.Option key={option.value} value={option.value}>
                                                {option.label}
                                            </Select.Option>
                                        ))}
                                    </Select>
                                </div>
                            </div>
                            <div className="user-label">
                                <Field name="displayName" subscription={{ value: true }}>
                                    {({ input: { value, onChange } }) => (
                                        <Input value={value} onChange={onChange} />
                                    )}
                                </Field>
                            </div>
                            <br></br>
                            <label><b>Groups</b></label>
                            <Field name="groups" type="checkbox" valuePropName="value" value={groups} >
                                {({ input }) => (
                                    <Checkbox.Group onChange={(values) => input.onChange(values)}>
                                        <Row>
                                            {groupNames.map((groupName) => (
                                                <Col span={8} key={groupName}>
                                                    <Checkbox value={groupName}>{groupName}</Checkbox>
                                                </Col>))}
                                        </Row>
                                    </Checkbox.Group>
                                )}
                            </Field>
                            <div style={{ display: "grid", justifyContent: "end" }}>
                                <Row>
                                    <Button key="back" onClick={hideModal} style={{ marginTop: 15, marginRight: 15 }}>
                                        Cancel
                                    </Button>
                                    <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                                        Create
                                    </Button>
                                </Row>
                            </div>
                        </form>
                    )}
                />
            </Modal >
        </>
    );
};