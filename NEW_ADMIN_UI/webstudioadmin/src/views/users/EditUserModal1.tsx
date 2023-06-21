import React, { useState, useEffect, useRef, useMemo } from 'react'
import { Button, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { Form as FinalForm, Field, Form } from 'react-final-form';
import arrayMutators from 'final-form-arrays'
import { Input, CheckboxGroup } from '../../components/form'
import './userModal.css';

const displayOrder = [
    {
        value: "firstLast",
        label: "First last",
    },
    {
        value: "lastFirst",
        label: "Last first",
    },
    {
        value: "Other",
        label: "Other",
    },
];

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
            type: "ADMIN" | "DEFAULT" | "EXTERNAL";
        }[];
    };
    updateUser: (updatedUser: any) => void;
    onSave: () => void;
}
interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
}

export const EditUserModal1: React.FC<EditUserProps> = ({ user, updateUser, onSave }) => {
    const apiURL = "http://localhost:8080/webstudio/rest";

    const [allGroups, setAllGroups] = useState<Group[]>([]);
    const [groupNames, setGroupNames] = useState<string[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(true);
    const [selectedGroupValues, setSelectedGroupValues] = useState<string[]>(user.groups);
    const formRef = useRef(null);

    const initialValues = useMemo(() => ({
        username: user.username,
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        displayName: user.displayName,
        groups: user.userGroups.map((group) => group.name),
    }), [user]);

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
    }, []);

    useEffect(() => {
        if (groupNames && user.groups) {
            setSelectedGroupValues(user.groups.filter((group) => groupNames.includes(group)));
        }
    }, [groupNames, user.groups]);


    const handleSubmit = async (values: any) => {
        const { email, displayName, firstName, lastName, password, groups, username } = values;

        let updatedDisplayName = '';

        if (displayName === 'firstLast') {
            updatedDisplayName = `${firstName} ${lastName}`;
        } else if (displayName === 'lastFirst') {
            updatedDisplayName = `${lastName} ${firstName}`;
        }

        const updatedUser = {
            email,
            displayName: updatedDisplayName,
            firstName,
            lastName,
            password,
            groups,
        };

        console.log("updated user: -> ", updatedUser);
        try {
            const response = await fetch(`${apiURL}/users/${username}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic YWRtaW46YWRtaW4='
                },
                body: JSON.stringify(updatedUser)
            });
            console.log('12. Response:', response);
            if (response.status === 204) {
                console.log("response body: ", response.body);
                console.log('User updated successfully!');
                updateUser(updatedUser);
                setIsModalOpen(false);
                onSave();
                return;
            }
            else {
                console.log('Response status:', response.status);
                console.error('Error updating user:', response.statusText);
                const responseText = await response.text();
                console.log('Response Text:', responseText);
            }
        } catch (error) {
            console.error('Error updating user:', error);
        }
    };

    const handleUserGroupChange = (selectedGroupValues: CheckboxValueType[]) => {
        const groupValues = selectedGroupValues.map((value) => value.toString());
        setSelectedGroupValues(groupValues);
        console.log("selected group values: ", selectedGroupValues);
    };

    return (
        <div>
            <FinalForm
                onSubmit={handleSubmit}
                mutators={{...arrayMutators}}
                initialValues={initialValues} >
                {({ handleSubmit, form }) => (
                    <form onSubmit={handleSubmit}
                    >
                        <br></br>
                        <label><b>Account</b></label>
                        <div className="user-label">
                            <label>Username</label>
                            <Input name="username" disabled />
                        </div>
                        <div className="user-label">
                            <label>Email</label>
                            <Input name="email" />
                        </div>
                        <div className="user-label">
                            <label>Password</label>
                            <Input name="password" type="password"/>
                        </div>
                        <br></br>
                        <label><b>Name</b></label>
                        <div className="user-label">
                            <label >First name (given name)</label>
                            <Input name="firstName" />
                        </div>
                        <div className="user-label">
                            <label >Last name (family name)</label>
                            <Input name="lastName" />
                        </div>
                        <div className="user-label">
                            <label>Display name:</label>
                            <div>
                                <Field name="displayName" component="select" >
                                    {({ input }) => (
                                        <>
                                            <Select
                                                style={{ width: 100 }}
                                                defaultActiveFirstOption
                                                onChange={(value) => {
                                                    input.onChange(value);
                                                }}
                                            >
                                                {displayOrder.map((option) => (
                                                    <Select.Option key={option.value} value={option.value} >
                                                        {option.label}
                                                    </Select.Option>
                                                ))}
                                            </Select>
                                        </>
                                    )}
                                </Field>
                            </div>
                        </div>
                        <div className="user-label">
                            <Field name="displayName" form={form}>
                                {({ input, meta, form }) => {
                                    const { values } = form.getState();
                                    const { firstName, lastName } = values;
                                    const displayOrder = values.displayName;
                                    let displayNameValue = '';
                                    if (displayOrder === 'firstLast') {
                                        displayNameValue = `${firstName} ${lastName}`;
                                    } else if (displayOrder === 'lastFirst') {
                                        displayNameValue = `${lastName} ${firstName}`;
                                    }
                                    return <Input {...input} value={displayNameValue} disabled/>;
                                }}
                            </Field>
                        </div>
                        <br></br>
                        <label><b>Groups</b></label>
                        <CheckboxGroup name="groups" options={groupNames} />
                        <div style={{ display: "grid", justifyContent: "end" }}>
                            <Row>
                                <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                                    Update
                                </Button>
                            </Row>
                        </div>
                    </form>
                )}
            </FinalForm>

        </div>
    );
};