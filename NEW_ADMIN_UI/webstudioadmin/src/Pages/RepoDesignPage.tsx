import React, { useState } from 'react';
import { RepositoryPage } from './RepositoryPage';
import { Form, Input, Card, Button, List, Col, Row, Select} from 'antd';
import DefaultLayout from '../components/DefaultLayout';
import {typeOptions} from 'views/repoDeployment/TypeOptions';
import { TypeGitDesign } from 'views/repoDesign/TypeGitDesign';
import { TypeAzure } from 'views/repoDesign/TypeAzure';
import { TypeAWSS3 } from 'views/repoDesign/TypeAWSS3';
import { TypeDatabaseJDBC } from 'views/repoDesign/TypeDatabaseJDBC';
import { TypeDatabaseJNDI } from 'views/repoDesign/TypeDatabaseJNDI';
import { GitNewBranch } from 'views/repoDesign/GitNewBranch';

export const RepoDesignPage: React.FC = () => {

    const [active, setActive] = useState(true);
    const [type, setType] = useState("");
    const [hide, setHide] = useState(true);

    return (
        <DefaultLayout>
            <Col>
                <RepositoryPage />
                <Row>
                    <Card style={{ width: 205, marginLeft: 20 }}>
                        <Col>
                            <Row>
                                <List>
                                    <List.Item>
                                        <p>List item</p>
                                    </List.Item>
                                    <List.Item>
                                        <p>Another list item</p>
                                    </List.Item>
                                </List>
                                <Button >Add repository</Button>
                            </Row>
                        </Col>
                    </Card>

                    {active && (
                        <Card bordered={true}
                            style={{
                                width: 550, marginLeft: 20
                            }}>
                            <Form labelCol={{ span: 10 }}
                                wrapperCol={{ span: 18 }}
                                labelAlign="left">
                                <Form.Item
                                    label={
                                        <span>
                                            Name &nbsp;
                                        </span>
                                    }
                                >
                                    <Input defaultValue="Design" />
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
                                        options={typeOptions}
                                        defaultActiveFirstOption={true}
                                    >
                                    </Select>
                                </Form.Item>
                                {type === "Git" ? (<div><TypeGitDesign /><GitNewBranch /></div>) :
                                    type === "DatabaseJDBC" ? <TypeDatabaseJDBC /> :
                                        type === "DatabaseJNDI" ? <TypeDatabaseJNDI /> :
                                            type === "AWSS3" ? <TypeAWSS3 /> :
                                                type === "AzureBlobStorage" ? <TypeAzure /> :
                                                    <TypeGitDesign />}
                            </Form>
                        </Card>
                    )}
                </Row>
            </Col>
        </DefaultLayout>
    )
};

