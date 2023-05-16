import React, { useState } from 'react';
import { Card, Form, Checkbox, Input, Col, List, Row, Select } from 'antd';
import { RepositoryPage } from './RepositoryPage';
import DefaultLayout from '../components/DefaultLayout';
import {typeOptions} from 'views/repoDeployment/TypeOptions';
import { TypeAWSS3 } from 'views/repoDesign/TypeAWSS3';
import { TypeAzure } from 'views/repoDesign/TypeAzure';
import { TypeDatabaseJDBC } from 'views/repoDesign/TypeDatabaseJDBC';
import { TypeDatabaseJNDI } from 'views/repoDesign/TypeDatabaseJNDI';
import { TypeGitDeployment } from 'views/repoDeployment/TypeGitDeployment';

export const RepoDeployConfPage: React.FC = () => {

    const [active, setActive] = useState(true);
    const [hide, setHide] = useState(true);
    const [type, setType] = useState("");


    const items = [
        {
            value: "Design",
            label: "Design",
        }
    ];

    return (
        <DefaultLayout>
            <Col>
                <RepositoryPage />


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
                                        Use design repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox checked onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Repository &nbsp;
                                    </span>
                                }
                            >
                                <Select options={items}
                                // placeholder="Design" 
                                />
                            </Form.Item>
                        </Form>
                    </Card>
                )}
                {!active && hide && (
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
                                        Use design repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Name &nbsp;
                                    </span>
                                }
                            >
                                <Input value="Deploy configuration" />
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

                            {type === "Git" ? (<div><TypeGitDeployment /> <Form.Item
                                label={
                                    <span>
                                        Path &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="DESIGN/deployments/" />
                            </Form.Item> </div>) :
                                type === "DatabaseJDBC" ? <TypeDatabaseJDBC /> :
                                    type === "DatabaseJNDI" ? <TypeDatabaseJNDI /> :
                                        type === "AWSS3" ? <TypeAWSS3 /> :
                                            type === "AzureBlobStorage" ? <TypeAzure /> :
                                                <TypeGitDeployment />}
                        </Form>
                    </Card>
                )}

            </Col>
        </DefaultLayout>
    )
};
