import React, { useState } from 'react';
import { Button, Checkbox, Col, Form, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { NavLink, useHref } from "react-router-dom";


const JSON_HEADERS = {
    "Content-Type": "application/json",
};

export const ModalNewUser: React.FC<{ fetchUsers: () => void }> = ({ fetchUsers }) => {
    // React.FC<{ addNewUser: (newUser: any) => void }> = ({ addNewUser }) => {

    const apiURL = "http://localhost:8080/webstudio/rest";
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [groups, setGroups] = useState<CheckboxValueType[]>([]);

    const showModal = () => {
        setIsModalOpen(true);
    };

    const hideModal = () => {
        setIsModalOpen(false);
    };

    const createUser = async () => {
        try {
            const requestBody = {
                email,
                displayName,
                firstName,
                lastName,
                password,
                groups: groups.map(group => group.toString()),
                username,
                internalPassword: {
                    password: password
                }
            };

            await fetch(`${apiURL}/users`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: "Basic YWRtaW46YWRtaW4="
                },
                body: JSON.stringify(requestBody)
            }).then(applyResult);
            setUsername("");
            setEmail("");
            setPassword("");
            setFirstName("");
            setLastName("");
            setDisplayName("");
            setGroups([]);
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

    const onChange = (checkedValues: CheckboxValueType[]) => {
        setGroups(checkedValues);
    };

    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        createUser();
    };

    return (
        <>
            <Button onClick={showModal} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                Add new user
            </Button>
            <Modal title="Create new user" open={isModalOpen}
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
                        >
                        </Select>
                    </Form.Item>
                    <Form.Item><b>Groups</b></Form.Item>

                    <Form.Item className="user-create-form_last-form-item">
                        <Checkbox.Group value={groups} onChange={onChange}>
                            <Row>
                                <Col span={8}>
                                    <Checkbox value="Administrators">Administrators</Checkbox>
                                </Col>
                                <Col span={8}>
                                    <Checkbox value="Analysts">Analysts</Checkbox>
                                </Col>
                                <Col span={8}>
                                    <Checkbox value="Deployers">Deployers</Checkbox>
                                </Col>
                                <Col span={8}>
                                    <Checkbox value="Developers">Developers</Checkbox>
                                </Col>
                                <Col span={8}>
                                    <Checkbox value="Testers">Testers</Checkbox>
                                </Col>
                                <Col span={8}>
                                    <Checkbox value="Viewers">Viewers</Checkbox>
                                </Col>
                            </Row>
                        </Checkbox.Group>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
};