import React, { useState } from 'react';
import { Button, Card, Checkbox, Col, Form, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import TableUserInfo from './TableUserInfo';
import { useNavigate } from 'react-router-dom';

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

interface EditUserProps {
    user: {
        key: string;
        userName: string;
        email: string;
        password: string;
        firstName: string;
        lastName: string;
        displayName: string;
        groups: CheckboxValueType[];
    }
    updateUser: (updatedUser: any) => void;
    onUpdateUserData: (userData: any[]) => void;
};

export const ModalEditUser: React.FC<EditUserProps> = ({ user, updateUser, onUpdateUserData }) => {
    const [editModalVisible, setEditModalVisible] = useState(true);
    const [userName, setUserName] = useState(user.userName);
    const [email, setEmail] = useState(user.email);
    const [password, setPassword] = useState(user.password);
    const [firstName, setFirstName] = useState(user.firstName);
    const [lastName, setLastName] = useState(user.lastName);
    const [displayName, setDisplayName] = useState(user.displayName);
    const [groups, setGroups] = useState<CheckboxValueType[]>(user.groups);
    const [state, setState] = useState("")
    const [userData, setUserData] = useState(TableUserInfo);

    const handleUserNameInputChange = (e: any) => {
        user.userName = e.target.value;
        setUserName(e.target.value);
    };

    const handleEmailInputChange = (e: any) => {
        user.email = e.target.value;
        setEmail(e.target.value);
    };

    const handlePasswordInputChange = (e: any) => {
        user.password = e.target.value;
        setPassword(e.target.value);
    };

    const handleFirstNameInputChange = (e: any) => {
        user.firstName = e.target.value;
        setFirstName(e.target.value);
    };

    const handleLastNameInputChange = (e: any) => {
        user.lastName = e.target.value;
        setLastName(e.target.value);
    };

    const navigate = useNavigate();
    const navigateUserList = () => {
        let path = `/users`;
        navigate(path);
    }

    // const handleEditSubmit = (e: React.SyntheticEvent) => {
    //     e.preventDefault();

    //     const editedUser = {
    //         ...user,
    //         userName,
    //         email,
    //         password,
    //         firstName,
    //         lastName,
    //         displayName,
    //         groups,

    //     };
    //     console.log("1");
    //     updateUser(editedUser);
    //     console.log("2" + editedUser);
    //     onUpdateUserData(userData);
    //     console.log("3" + userData);
    //     navigateUserList();
    //     setEditModalVisible(false);
    //     console.log("4" + editModalVisible);
    // };


    const onChange = (checkedValues: CheckboxValueType[]) => {
        setGroups(checkedValues);
    };

    // const handleCancel = () => {
    //     setEditModalVisible(false);
    // };

    return (
        <>
            <div>
                <Form layout="vertical" >
                    <Form.Item><b>Account</b></Form.Item>
                    <Form.Item label="Username">
                        <Input id="userName"
                            value={user.userName}
                            onChange={handleUserNameInputChange}
                        />
                    </Form.Item>
                    <Form.Item label="Email">
                        <Input id="email"
                            value={user.email}
                            onChange={handleEmailInputChange}
                        />
                    </Form.Item>
                    <Form.Item label="Password">
                        <Input id="password"
                            value={user.password}
                            onChange={handlePasswordInputChange}
                        />
                    </Form.Item>
                    <Form.Item><b>Name</b></Form.Item>
                    <Form.Item label="First name (Given name):">
                        <Input id="firstName"
                            value={user.firstName}
                            onChange={handleFirstNameInputChange}
                        />
                    </Form.Item>
                    <Form.Item label="Last name (Family name):">
                        <Input id="lastName"
                            value={user.lastName}
                            onChange={handleLastNameInputChange}
                        />
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
                    <Form.Item><b>Group</b></Form.Item>
                    <Form.Item className="user-create-form_last-form-item">
                        <Checkbox.Group onChange={setGroups} value={groups}>
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
                    {/* <Row style={{ float: "right" }}>
                        <Button key="submit" onClick={handleEditSubmit} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                            Save!
                        </Button>
                    </Row> */}
                </Form>
            </div>
        </>
    );
};