import React, { useState } from 'react'
import { Form, Input, Card, Button, List, Col, Row, Select } from 'antd'
import { typeOptions } from 'containers/repoDeployment/TypeOptions'
import { TypeGitDesign } from 'containers/repoDesign/TypeGitDesign'
import { TypeAzure } from 'containers/repoDesign/TypeAzure'
import { TypeAWSS3 } from 'containers/repoDesign/TypeAWSS3'
import { TypeDatabaseJDBC } from 'containers/repoDesign/TypeDatabaseJDBC'
import { TypeDatabaseJNDI } from 'containers/repoDesign/TypeDatabaseJNDI'
import { GitNewBranch } from 'containers/repoDesign/GitNewBranch'
import { CloseCircleOutlined } from '@ant-design/icons'
import DefaultLayout from 'layouts/DefaultLayout'
import { RepositoryPage } from './RepositoryPage'

export const RepoDesignPage: React.FC = () => {
    const [ type, setType ] = useState('')

    return (
        <DefaultLayout>
            <Col>
                <RepositoryPage />
                <Row>
                    <Card style={{ width: 205, marginLeft: 20 }}>
                        <Col>
                            <Row>
                                <List style={{ justifyContent: 'left', alignContent: 'middle' }}>
                                    <List.Item>
                                        <div>Design repository 1</div>
                                        <Button
                                            icon={<CloseCircleOutlined />}
                                            type="text"
                                        />
                                    </List.Item>
                                    <List.Item>
                                        <div>Design repository 2</div>
                                        <Button
                                            icon={<CloseCircleOutlined />}
                                            type="text"
                                        />
                                    </List.Item>
                                </List>
                                <Button>Add repository</Button>
                            </Row>
                        </Col>
                    </Card>
                    <Card
                        bordered
                        style={{ width: 675, marginLeft: 20 }}
                    >
                        <Form
                            labelAlign="left"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 20 }}
                        >
                            <Form.Item label={(<span>Name &nbsp;</span>)}>
                                <Input defaultValue="Design" />
                            </Form.Item>
                            <Form.Item label={(<span>Type &nbsp;</span>)}>
                                <Select
                                    defaultActiveFirstOption
                                    onChange={(value) => setType(value)}
                                    options={typeOptions}
                                    value={type}
                                />
                            </Form.Item>
                            {type === 'Git' ? (
                                <div>
                                    <TypeGitDesign />
                                    <GitNewBranch />
                                </div>
                            )
                                : type === 'DatabaseJDBC' ? <TypeDatabaseJDBC />
                                    : type === 'DatabaseJNDI' ? <TypeDatabaseJNDI />
                                        : type === 'AWSS3' ? <TypeAWSS3 />
                                            : type === 'AzureBlobStorage' ? <TypeAzure />
                                                : <TypeGitDesign />}
                        </Form>
                    </Card>
                </Row>
            </Col>
        </DefaultLayout>
    )
}
