import React, { useState } from 'react';
import { Button, Cascader, Checkbox, Col, Form, Input, Modal, Row } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';

export function ModalNewUser() {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [userName, setUserName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [group, setGroup] = useState([]);

    const showModal = () => {
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setIsModalOpen(false);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
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
        },

    ]

    const onChange = (checkedValues: CheckboxValueType[]) => {
        console.log('checked = ', checkedValues);
    };

    const handleInputChange = (e:any) => {
        const { id, value } = e.target;
        if (id === "userName") {
            setUserName(value);
        }
        if (id === "email") {
            setEmail(value);
        }
        if (id === "password") {
            setPassword(value);
        }
        if (id === "firstName") {
            setFirstName(value);
        }
        if (id === "lastName") {
            setLastName(value);
        }
        if (id === "displayName") {
            setDisplayName(value);
        }
        if (id === "group") {
            setGroup(value);
        }
    }

    return (
        <>
            <Button onClick={showModal}>
                Add new user
            </Button>
            <Modal title="Create new user" open={isModalOpen} onOk={handleCreate} onCancel={handleCancel}>
                <Form layout="vertical">
                    <Form.Item><b>Account</b></Form.Item>
                    <Form.Item label="Username">
                        <Input id="userName" value={userName} onChange={(e) => handleInputChange(e)} />
                    </Form.Item>
                    <Form.Item label="Email">
                        <Input id="email" value={email} onChange={(e) => handleInputChange(e)}/>
                    </Form.Item>
                    <Form.Item label="Password">
                        <Input id="password" value={password} onChange={(e) => handleInputChange(e)}/>
                    </Form.Item>
                    <Form.Item><b>Name</b></Form.Item>
                    <Form.Item label="First name (Given name):">
                        <Input id="firstName" value={firstName} onChange={(e) => handleInputChange(e)}/>
                    </Form.Item>
                    <Form.Item label="Last name (Family name):">
                        <Input id="lastName" value={lastName} onChange={(e) => handleInputChange(e)}/>
                    </Form.Item>
                    <Form.Item label="Display name:">
                        <Cascader options={displayOrder} placeholder="First last" id="displayName"/>
                    </Form.Item>
                    <Form.Item><b>Group</b></Form.Item>

                    <Form.Item className="user-create-form_last-form-item">
                        <Checkbox.Group onChange={onChange}>
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