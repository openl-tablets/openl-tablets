import { Button, Card, Cascader, Checkbox, Col, Form, Input, Row } from "antd";
import React, { useContext, useEffect, useState, } from "react";
import type { CheckboxValueType } from "antd/es/checkbox/Group";
import { useNavigate } from "react-router-dom";
import { DataContext } from "../../components/DataContext";


export const NewUser: React.FC<{ addNewUser: (newUser: any) => void }> = ({ addNewUser }) => {

    const [userName, setUserName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [group, setGroup] = useState<CheckboxValueType[]>([]);
    const [checkedValues, setCheckedValues] = useState<CheckboxValueType[]>([]);

    const navigate = useNavigate();
    const navigateUserList = () => {
        let path = `/users`;
        navigate(path);
    }

    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        const newUser = {
            userName,
            email,
            password,
            firstName,
            lastName,
            displayName,
            groups: group,
        };
        addNewUser(newUser);
        setUserName("");
        setEmail("");
        setPassword("");
        setFirstName("");
        setLastName("");
        setDisplayName("");
        setGroup([]);
        setCheckedValues([]);
        navigateUserList();
    };


    const onChange = (checkedValues: CheckboxValueType[]) => {
        setGroup(checkedValues);
    };

    const displayOrder = [
        {
            value: ({ firstName } + " " + { lastName }),
            label: "First last",
        },
        {
            value: ({ lastName } + " " + { firstName }),
            label: "Last first",
        },
        {
            value: " ",
            label: "Other",
        },

    ];

    return (
        <div>

            <div style={{ display: "flex", flexDirection: "row" }}>
                <Card style={{ margin: 20, width: 1200 }} title="Create new user">
                    <Form layout="vertical" >
                        <Row>

                            <Col span={8} style={{ padding: 12 }}>
                                <Form.Item><b>Account</b></Form.Item>
                                <Form.Item label="Username">
                                    <Input id="userName" value={userName} onChange={(e) => setUserName(e.target.value)} />
                                </Form.Item>
                                <Form.Item label="Email">
                                    <Input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                                </Form.Item>
                                <Form.Item label="Password" >
                                    <Input.Password id="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                                </Form.Item>
                            </Col>

                            <Col span={8} style={{ padding: 12 }}>
                                <Form.Item><b>Name</b></Form.Item>
                                <Form.Item label="First name (Given name):">
                                    <Input id="firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
                                </Form.Item>
                                <Form.Item label="Last name (Family name):">
                                    <Input id="lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} />
                                </Form.Item>
                                <Form.Item label="Display name:">
                                    <Cascader options={displayOrder} placeholder="First last" id="displayName" />
                                </Form.Item>
                            </Col>
                            <Col span={8} style={{ padding: 12 }}>
                                <Form.Item><b>Group</b></Form.Item>
                                <Form.Item className="user-create-form_last-form-item" id="group" >
                                    <Checkbox.Group onChange={onChange} >

                                        <Row>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Administrators">Administrators</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Analysts">Analysts</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Deployers">Deployers</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Developers">Developers</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Testers">Testers</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value="Viewers">Viewers</Checkbox>
                                            </Col>
                                        </Row>

                                    </Checkbox.Group>
                                </Form.Item>
                            </Col>
                        </Row>
                        <Row style={{ float: "right" }}>
                            <Button onClick={handleSubmit} >Create</Button>
                        </Row>
                    </Form>
                </Card>
            </div>
        </div >
    );
};
