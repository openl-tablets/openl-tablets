import React, { useState } from 'react';
import { Button, Cascader, Checkbox, Col, Form, Input, Modal, Row } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';

export function ModalNewUser() {
    const [isModalOpen, setIsModalOpen] = useState(false);

    const showModal = () => {
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setIsModalOpen(false);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
    };


    // const { visible, onCancel, onCreate, form } = this.props;

    // const { getFieldDecorator } = form;


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


    return (
        <>
            <Button onClick={showModal}>
                Add new user
            </Button>
            <Modal title="Create new user" open={isModalOpen} onOk={handleCreate} onCancel={handleCancel}>
                <Form layout="vertical">
                    <Form.Item><b>Account</b></Form.Item>
                    <Form.Item label="Username">
                        <Input />
                    </Form.Item>
                    <Form.Item label="Email">
                        <Input />
                    </Form.Item>
                    <Form.Item label="Password">
                        <Input />
                    </Form.Item>
                    <Form.Item><b>Name</b></Form.Item>
                    <Form.Item label="First name (Given name):">
                        <Input />
                    </Form.Item>
                    <Form.Item label="Last name (Family name):">
                        <Input />
                    </Form.Item>
                    <Form.Item label="Display name:">
                        <Cascader options={displayOrder} placeholder="First last" />
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