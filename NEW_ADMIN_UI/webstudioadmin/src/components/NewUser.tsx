import { Button, Card, Cascader, Checkbox, Col, Form, Input, Row } from "antd";
import React, { useState } from "react";
import { AdminMenu } from "./AdminMenu";
import type { CheckboxValueType } from 'antd/es/checkbox/Group';

export const NewUser: React.FC<{ addNewUser: (newUser: any) => void }> = ({ addNewUser }) => {

    const [userName, setUserName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [group, setGroup] = useState<CheckboxValueType[]>([]);

    const handleSubmit = (e) => {
        e.preventDefault();
        const newUser = {
            userName,
            email,
            password,
            firstName,
            lastName,
            displayName,
            groups: group
        };
        addNewUser(newUser);
        setUserName("");
        setEmail("");
        setPassword("");
        setFirstName("");
        setLastName("");
        setDisplayName("");
        setGroup([]);
    };

    const onChange = (checkedValues: CheckboxValueType[]) => {
        // console.log('checked = ', checkedValues);
        setGroup(checkedValues);
        // console.log(group);
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



    // const App: React.FC = () => {
    //     const [checkedList, setCheckedList] = useState<CheckboxValueType[]>(defaultCheckedList);
    //     const [indeterminate, setIndeterminate] = useState(true);
    //     const [checkAll, setCheckAll] = useState(false);




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
                                <Form.Item>
                                    {displayName === "first last" ? { firstName } + " " + { lastName } : displayName === "Last first" ? { lastName } + " " + { firstName } : "asd"}
                                </Form.Item>
                            </Col>

                            <Col span={8} style={{ padding: 12 }}>
                                <Form.Item><b>Group</b></Form.Item>
                                <Form.Item className="user-create-form_last-form-item" id="group" >
                                    <Checkbox.Group onChange={onChange} >

                                        <Row>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value={group}>Administrators</Checkbox>
                                            </Col>
                                            <Col span={9} style={{ padding: 5 }}>
                                                <Checkbox value={group}>Analysts</Checkbox>
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
};
