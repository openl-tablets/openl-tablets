import React, { useState } from 'react';
import { RepositoryPage } from './RepositoryPage';
import { Form, Input, Card, Button, Col, List, Row, Select } from 'antd';
import DefaultLayout from '../components/DefaultLayout';
import { deploymentTypeOptions } from 'views/repoDeployment/TypeOptions';
import { TypeAWSS3 } from 'views/repoDesign/TypeAWSS3';
import { TypeAzure } from 'views/repoDesign/TypeAzure';
import { TypeDatabaseJDBC } from 'views/repoDesign/TypeDatabaseJDBC';
import { TypeDatabaseJNDI } from 'views/repoDesign/TypeDatabaseJNDI';
import { TypeGitDeployment } from 'views/repoDeployment/TypeGitDeployment';
import { TypeLocal } from 'views/repoDeployment/TypeLocal';
import { CloseCircleOutlined } from '@ant-design/icons';


export const RepoDeploymentPage: React.FC = () => {
    const [active, setActive] = useState(true);
    const [type, setType] = useState("");

    return (
        <DefaultLayout>
            <Col>
                <RepositoryPage />
                <Row>
                    <Card style={{ width: 205, marginLeft: 20 }}>
                        <Col>
                            <Row>
                                <List style={{ justifyContent: "left", alignContent: "middle" }}>
                                    <List.Item>
                                        <div>Deployment repository 1</div>
                                        <Button
                                            type="text"
                                            icon={<CloseCircleOutlined/>}
                                        >
                                        </Button>
                                    </List.Item>
                                    <List.Item>
                                        <div>Deployment repository 2</div>
                                        <Button
                                            type="text"
                                            icon={<CloseCircleOutlined />}
                                        >
                                        </Button>
                                    </List.Item>
                                </List>
                                <Button >Add repository</Button>
                            </Row>
                        </Col>
                    </Card>
                    {active && (
                        <Card bordered={true}
                            style={{
                                width: 675, marginLeft: 20
                            }}>
                            <Form labelCol={{ span: 8 }}
                                wrapperCol={{ span: 20 }}
                                labelAlign="left">
                                <Form.Item
                                    label={
                                        <span>
                                            Name &nbsp;
                                        </span>
                                    }
                                >
                                    <Input defaultValue="Deployment" />
                                </Form.Item>

                                <Form.Item
                                    label={
                                        <span>
                                            Type &nbsp;
                                        </span>
                                    }
                                >
                                    <Select
                                        value={type}
                                        onChange={(value) => setType(value)}
                                        options={deploymentTypeOptions}
                                        defaultActiveFirstOption={true}
                                    >
                                    </Select>
                                </Form.Item>
                                {type === "Git" ? <TypeGitDeployment /> :
                                    type === "DatabaseJDBC" ? <TypeDatabaseJDBC /> :
                                        type === "DatabaseJNDI" ? <TypeDatabaseJNDI /> :
                                            type === "AWSS3" ? <TypeAWSS3 /> :
                                                type === "AzureBlobStorage" ? <TypeAzure /> :
                                                    type === "Local" ? <TypeLocal /> :
                                                        <TypeGitDeployment />}
                            </Form>
                        </Card>
                    )}
                </Row>
            </Col>
        </DefaultLayout>
    )
};
