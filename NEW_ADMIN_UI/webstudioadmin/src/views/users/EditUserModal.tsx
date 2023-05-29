import React, { useState } from 'react';
import { Button, Card, Checkbox, Col, Form, Input, Modal, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
// import TableUserInfo from './TableUserInfo';
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
        username: string;
        email: string;
        password: string;
        firstName: string;
        lastName: string;
        displayName: string;
        groups: CheckboxValueType[];
    }
    updateUser: (updatedUser: any) => void;
    onUpdateUserData: (userData: any[]) => any[];
    onSave: () => void;
}
const JSON_HEADERS = {
    "Content-Type": "application/json",
};


export const EditUserModal: React.FC<EditUserProps> = ({ user, updateUser, onUpdateUserData, onSave }) => {
    const apiURL = "https://demo.openl-tablets.org/nightly/webstudio/rest";

    const { username: initialUsername, email: initialEmail, password: initialPassword, firstName: initialFirstName, lastName: initialLastName } = user
    const [isModalOpen, setIsModalOpen] = useState(true);
    const [username, setUsername] = useState(initialUsername);
    const [email, setEmail] = useState(initialEmail);
    const [password, setPassword] = useState(initialPassword);
    const [firstName, setFirstName] = useState(initialFirstName);
    const [lastName, setLastName] = useState(initialLastName);
    const [displayName, setDisplayName] = useState(user.displayName);
    const [groups, setGroups] = useState<CheckboxValueType[]>(user.groups);
    const [editedUser, setEditedUser] = useState(user);

    // const [userData, setUserData] = useState(TableUserInfo);
    const [userData, setUserData] = useState([]);

    const updateUser1 = () => {
        fetch(
            `${apiURL}/users/{username}`,
            {
                method: "PUT",
                headers: JSON_HEADERS,
                body: JSON.stringify(userData),
            }
        )
    };

    const handleUsernameInputChange = (e: any) => {
        setUsername(e.target.value);
    };

    const handleEmailInputChange = (e: any) => {
        setEmail(e.target.value);
    };

    const handlePasswordInputChange = (e: any) => {
        setPassword(e.target.value);
    };

    const handleFirstNameInputChange = (e: any) => {
        setFirstName(e.target.value);
    };

    const handleLastNameInputChange = (e: any) => {
        setLastName(e.target.value);
    };

    const navigate = useNavigate();
    const navigateUserList = () => {
        let path = `/users`;
        navigate(path);
    }

    const handleSave = () => {
        const editedUser = {
            ...user,
            username: username,
            email: email,
            password: password,
            firstName: firstName,
            lastName: lastName,
            displayName: displayName,
            groups: groups,
        };

        // updateUser1(editedUser);
        onUpdateUserData = (userData: any[]): any[] => {
            return userData.map((user: any) => (user.key === editedUser.key ? editedUser : user));
        };
        setIsModalOpen(false);
        onSave();

        console.log(editedUser);
    };

    return (
        <div>
            <Form layout="vertical">
                <Form.Item><b>Account</b></Form.Item>
                <Form.Item label="Username">
                    <Input
                        id="username"
                        value={username}
                        onChange={handleUsernameInputChange}
                    />
                </Form.Item>
                <Form.Item label="Email">
                    <Input
                        id="email"
                        value={email}
                        onChange={handleEmailInputChange}
                    />
                </Form.Item>
                <Form.Item label="Password">
                    <Input
                        id="password"
                        value={password}
                        onChange={handlePasswordInputChange}
                    />
                </Form.Item>
                <Form.Item><b>Name</b></Form.Item>
                <Form.Item label="First name (Given name):">
                    <Input
                        id="firstName"
                        value={firstName}
                        onChange={handleFirstNameInputChange}
                    />
                </Form.Item>
                <Form.Item label="Last name (Family name):">
                    <Input
                        id="lastName"
                        value={lastName}
                        onChange={handleLastNameInputChange}
                    />
                </Form.Item>
                <Form.Item label="Display name:">
                    <Select
                        value={displayName}
                        onChange={(order) => setDisplayName(order)}
                        options={displayOrder}
                        defaultActiveFirstOption={true}
                    />
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
                    <Row style={{ float: "right" }}>
                        <Button
                            key="submit"
                            onClick={updateUser1}
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
