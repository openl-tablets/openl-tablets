import { Button, Card, Cascader, Checkbox, Col, Form, Input, Row } from "antd";
import React, { useState } from "react";
import { AdminMenu } from "./AdminMenu";
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { UserPage } from "../Pages/UserPage";
import { Link } from "react-router-dom";

const NewUser = () => {

    const [userName, setUserName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [group, setGroup] = useState([]);

    const handleSubmit = (e) => {
        e.preventDefault();
        const addUser = {
            userName: userName,
            email: email,
            password: password,
            firstname: firstName,
            lastName: lastName,
            displayName: displayName,
            group: group
        }
        console.log(addUser);
        setUserName("");
        setEmail("");
        setPassword("");
        setFirstName("");
        setLastName("");
        setDisplayName("");
        setGroup([]);
    }

    const onChange = (checkedValues: CheckboxValueType[]) => {
        console.log('checked = ', checkedValues);
        setGroup([]);
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

    ];



    return (
        <div>

            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20, width: 1200 }}>
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
                                <Form.Item label="Password">
                                    <Input id="password" value={password} onChange={(e) => setPassword(e.target.value)} />
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
                            <Button onClick={handleSubmit}>Create</Button>
                        </Row>
                    </Form>
                </Card>
            </div>
        </div >
    );
}
export default NewUser;