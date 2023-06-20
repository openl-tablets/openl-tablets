import React, { useState, useEffect, useRef } from 'react';
import { Button, Checkbox, Col, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { Form as FinalForm, Field, FormSpy } from 'react-final-form';
import { stringify } from 'querystring';
import './userModal.css';

const displayOrder = [
    {
        value: "First last",
        label: "First last",
    },
    {
        value: "Last first",
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
    const [username, setUsername] = useState(user.username);
    const [selectedDisplayOrder, setSelectedDisplayOrder] = useState<string>(user.displayName);
    const [selectedGroupValues, setSelectedGroupValues] = useState<string[]>(user.groups);
    const formRef = useRef(null);


    const initialValues = {
        username: user.username,
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        displayName: user.displayName,
        selectedGroupValues: user.groups,
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
    }, []);

    const handleSubmit = async (values: any) => {
        const { email, displayName, firstName, lastName, password, groups } = values;

        const updatedUser = {
            email,
            displayName,
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
        console.log(selectedGroupValues);
    };
  

    const handleDisplayNameChange = (value: string) => {
        setSelectedDisplayOrder(value);
    };

    return (
        <div>
            <FinalForm
                onSubmit={handleSubmit}
                initialValues={initialValues}
                render={({ handleSubmit }) => (
                    <form onSubmit={handleSubmit}
                    >
                        <br></br>
                        <label><b>Account</b></label>
                        <div className="user-label">
                            <label>Username</label>
                            <Field name="username" component="input" type="text">
                                {props => (
                                    <div>
                                        <b>{user.username}</b>
                                    </div>
                                )}</Field>
                        </div>
                        <div className="user-label">
                            <label>Email</label>
                            <Field name="email">
                                {({ input }) => <Input {...input} />}
                            </Field>
                        </div>
                        <div className="user-label">
                            <label>Password</label>
                            <Field name="password" component="input" type="password">
                                {({ input }) => <Input {...input} />}

                            </Field>
                        </div>
                        <br></br>
                        <label><b>Name</b></label>
                        <div className="user-label">
                            <label >First name (given name)</label>
                            <Field name="firstName" component="input" type="text">
                                {({ input }) => <Input {...input} />}
                            </Field>
                        </div>
                        <div className="user-label">
                            <label >Last name (family name)</label>
                            <Field name="lastName" component="input" type="text">
                                {({ input }) => <Input {...input} />}

                            </Field>
                        </div>

                        <div className="user-label">
                            <label >Display name:</label>
                            <div>
                                <Field name="displayName">
                                    {({ input }) => (
                                        <Select
                                            defaultValue={"First last"}
                                            options={displayOrder}
                                        />
                                    )}
                                </Field>
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
                        <Field name="groups" type="checkbox"
                        //  valuePropName="value" value={selectedGroupValues}
                        >
                            {({ input }) => (
                                <Checkbox.Group onChange={handleUserGroupChange} value={selectedGroupValues}>
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
                                <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                                    Update
                                </Button>
                            </Row>
                        </div>
                    </form>
                )}
            />

        </div>
    );
};