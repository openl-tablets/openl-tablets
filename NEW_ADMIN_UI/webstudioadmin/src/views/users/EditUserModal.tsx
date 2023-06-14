import React, { useState, useEffect } from 'react';
import { Button, Card, Checkbox, Col, Form, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { stringify } from 'querystring';


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

const groupOptions = [
    {
        value: "Administrators",
        label: "Administrators",
    },
    {
        value: "Analysts",
        label: "Analysts",
    },
    {
        value: "Deployers",
        label: "Deployers",
    },
    {
        value: "Developers",
        label: "Developers",
    },
    {
        value: "Testers",
        label: "Testers",
    },
    {
        value: "Viewers",
        label: "Viewers",
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

export const EditUserModal: React.FC<EditUserProps> = ({ user, updateUser, onSave }) => {
    const apiURL = "http://localhost:8080/webstudio/rest";

    const [isModalOpen, setIsModalOpen] = useState(true);
    const [username, setUsername] = useState(user.username);
    const [email, setEmail] = useState(user.email);
    const [password, setPassword] = useState(user.password);
    const [firstName, setFirstName] = useState(user.firstName);
    const [lastName, setLastName] = useState(user.lastName);
    const [displayName, setDisplayName] = useState(user.displayName);
    // const [selectedGroupValues, setSelectedGroupValues] = useState<CheckboxValueType[]>(user.userGroups.map((group) => group.name));
    // const [selectedGroupValues, setSelectedGroupValues] = useState<string[]>(user.userGroups.map((group) => group.name));
    const [selectedGroupValues, setSelectedGroupValues] = useState<string[]>(user.groups);


    useEffect(() => {
        setUsername(user.username);
        setEmail(user.email);
        setPassword(user.password);
        setFirstName(user.firstName);
        setLastName(user.lastName);
        setDisplayName(user.displayName);
        setSelectedGroupValues(user.groups);
        // setSelectedGroupValues(user.userGroups.map((group) => group.name));
    }, [user]);

    const handleEmailInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setEmail(e.target.value);
    };

    const handlePasswordInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setPassword(e.target.value);
    };

    const handleFirstNameInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFirstName(e.target.value);
    };

    const handleLastNameInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setLastName(e.target.value);
    };

    const handleSave = async () => {
        const updatedUser = {
            email: email,
            displayName: displayName,
            firstName: firstName,
            lastName: lastName,
            password: password,
            groups: selectedGroupValues
        };

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
            console.log('Response status:', response.status);
            if (response.status === 204) {
                console.log('User updated successfully!');
                setEmail(updatedUser.email);
                setPassword(updatedUser.password);
                setFirstName(updatedUser.firstName);
                setLastName(updatedUser.lastName);
                setDisplayName(updatedUser.displayName);
                setSelectedGroupValues(updatedUser.groups);
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

    return (
        <div>
            <Form layout="vertical">
                <Form.Item>
                    <b>Account</b>
                </Form.Item>
                <Form.Item label="Username">
                    <p><b>{username}</b></p>
                    {/* <Input id="username" value={username} onChange={handleUsernameInputChange} /> */}
                </Form.Item>
                <Form.Item label="Email">
                    <Input id="email" value={email} onChange={handleEmailInputChange} />
                </Form.Item>
                <Form.Item label="Password">
                    <Input id="password" value={password} onChange={handlePasswordInputChange} />
                </Form.Item>
                <Form.Item>
                    <b>Name</b>
                </Form.Item>
                <Form.Item label="First name (Given name):">
                    <Input id="firstName" value={firstName} onChange={handleFirstNameInputChange} />
                </Form.Item>
                <Form.Item label="Last name (Family name):">
                    <Input id="lastName" value={lastName} onChange={handleLastNameInputChange} />
                </Form.Item>
                <Form.Item label="Display name:">
                    <Select value={displayName} onChange={(order) => setDisplayName(order)} options={displayOrder} defaultActiveFirstOption={true} />
                </Form.Item>
                <Form.Item>
                    <b>Group</b>
                </Form.Item>
                <Form.Item className="user-create-form_last-form-item">
                    <Checkbox.Group onChange={handleUserGroupChange} value={selectedGroupValues}>
                        <Row>
                            {groupOptions.map((option) => (
                                <Col span={8} key={option.value}>
                                    <Checkbox value={option.value}>{option.label}</Checkbox>
                                </Col>
                            ))}
                        </Row>
                    </Checkbox.Group>
                </Form.Item>
                <Form.Item>
                    <Row style={{ float: "right" }}>
                        <Button
                            key="submit"
                            onClick={handleSave}
                            style={{
                                marginLeft: 15,
                                marginTop: 15,
                                color: "green",
                                borderColor: "green"
                            }}
                        >
                            Save
                        </Button>
                    </Row>
                </Form.Item>
            </Form>
        </div>
    );
};