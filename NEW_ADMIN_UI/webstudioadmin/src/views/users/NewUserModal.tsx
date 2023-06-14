import React, { useEffect, useState } from 'react';
import { Button, Checkbox, Col, Form, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import './newUserModal.css';

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
    }
];

interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
};

export const NewUserModal: React.FC<{ fetchUsers: () => void }> = ({ fetchUsers }) => {

    const apiURL = "http://localhost:8080/webstudio/rest";

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [userGroups, setUserGroups] = useState<CheckboxValueType[]>([]);
    const [allGroups, setAllGroups] = useState<Group[]>([]);
    const [groupNames, setGroupNames] = useState<string[]>([]);

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
                console.log("groupnames", groupNames);
            } else {
                console.error("Failed to fetch groups:", response.statusText);
            }
        } catch (error) {
            console.error("Error fetching groups:", error);
        }
    };

    useEffect(() => {
        fetchGroupData();
        console.log("all groups : ", allGroups);
        console.log("groups: ", userGroups);
        console.log("groupnames", groupNames);

        setUserGroups([]);
    }, []);

    const createUser = async (constructedDisplayName: string) => {
        try {
            const requestBody = {
                email,
                displayName: constructedDisplayName,
                firstName,
                lastName,
                password,
                userGroups: userGroups.map(group => group.toString()),
                username,
                internalPassword: {
                    password: password
                }
            };

            await fetch(`${apiURL}/users`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Basic YWRtaW46YWRtaW4="
                },
                body: JSON.stringify(requestBody)
            }).then(applyResult);
            setUsername("");
            setEmail("");
            setPassword("");
            setFirstName("");
            setLastName("");
            setDisplayName("");
            setUserGroups([]);
            setIsModalOpen(false);
            console.log("Created User:", requestBody);
            fetchUsers();
        } catch (error) {
            console.error("Error creating user:", error);
        }
    };

    const applyResult = (result: any) => {
        hideModal();
    };

    const onChange = (checkedValues: CheckboxValueType[]) => {
        setUserGroups(checkedValues as string[]);
    };


    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        let constructedDisplayName = "";
        if (displayName === "First last") {
            constructedDisplayName = `${firstName} ${lastName}`;
        } else if (displayName === "Last first") {
            constructedDisplayName = `${lastName} ${firstName}`;
        } else {
            constructedDisplayName = displayName;
        }
        createUser(constructedDisplayName);
    };

    return (
        <>
            <Button
                onClick={showModal}
                style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                Add new user
            </Button>
            <Modal
                className='new-user-modal'
                title="Create new user"
                open={isModalOpen}
                onCancel={hideModal}
                footer={[
                    <Button key="back" onClick={hideModal}>
                        Cancel
                    </Button>,
                    <Button key="submit" onClick={handleSubmit} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                        Create
                    </Button>]}
            >
                <Form layout="vertical">
                    <Form.Item><b>Account</b></Form.Item>
                    <Form.Item label="Username">
                        <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} />
                    </Form.Item>
                    <Form.Item label="Email">
                        <Input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                    </Form.Item>
                    <Form.Item label="Password">
                        <Input id="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                    </Form.Item>
                    <Form.Item><b>Name</b></Form.Item>
                    <Form.Item label="First name (Given name):">
                        <Input id="firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
                    </Form.Item>
                    <Form.Item label="Last name (Family name):">
                        <Input id="lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} />
                    </Form.Item>
                    <Form.Item label="Display name:">
                        <Select
                            value={displayName}
                            onChange={(order) => setDisplayName(order)}
                            options={displayOrder}
                            defaultActiveFirstOption={true}
                        />
                        <Input style={{ marginTop: 15 }}
                            value={
                                displayName === "First last"
                                    ? `${firstName} ${lastName}`
                                    : displayName === "Last first"
                                        ? `${lastName} ${firstName}`
                                        : displayName
                            }
                        />
                    </Form.Item>
                    <Form.Item><b>Groups</b></Form.Item>

                    <Form.Item className="user-create-form_last-form-item">
                        <Checkbox.Group value={userGroups} onChange={onChange}>
                            <Row>
                                {groupNames.map((groupName) => (
                                    <Col span={8}>
                                        <Checkbox value={groupName}>{groupName}</Checkbox>
                                    </Col>
                                ))}

                            </Row>
                        </Checkbox.Group>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
};