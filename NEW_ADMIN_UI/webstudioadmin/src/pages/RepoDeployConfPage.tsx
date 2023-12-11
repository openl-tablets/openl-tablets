import React, { useState } from 'react'
import { Card, Form, Checkbox, Input, Col, Select } from 'antd'
import { typeOptions } from 'containers/repoDeployment/TypeOptions'
import { TypeAWSS3 } from 'containers/repoDesign/TypeAWSS3'
import { TypeAzure } from 'containers/repoDesign/TypeAzure'
import { TypeDatabaseJDBC } from 'containers/repoDesign/TypeDatabaseJDBC'
import { TypeDatabaseJNDI } from 'containers/repoDesign/TypeDatabaseJNDI'
import { TypeGitDeployment } from 'containers/repoDeployment/TypeGitDeployment'
import DefaultLayout from 'layouts/DefaultLayout'
import { RepositoryPage } from './RepositoryPage'

export const RepoDeployConfPage: React.FC = () => {
    const [ active, setActive ] = useState(true)
    const [ type, setType ] = useState('')

    const items = [
        {
            value: 'Design',
            label: 'Design',
        },
    ]

    return (
        <DefaultLayout>
            <Col>
                <RepositoryPage />
                {active && (
                    <Card
                        bordered
                        style={{ width: 900, marginLeft: 20 }}
                    >
                        <Form
                            labelAlign="left"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 20 }}
                        >
                            <Form.Item label={(<span>Use design repository &nbsp;</span>)}>
                                <Checkbox checked onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item label={(<span>Repository &nbsp;</span>)}>
                                <Select options={items} />
                            </Form.Item>
                        </Form>
                    </Card>
                )}
                {!active && (
                    <Card
                        bordered
                        style={{ width: 900, marginLeft: 20 }}
                    >
                        <Form
                            labelAlign="left"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 20 }}
                        >
                            <Form.Item label={(<span>Use design repository &nbsp;</span>)}>
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item label={(<span>Name &nbsp;</span>)}>
                                <Input value="Deploy configuration" />
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
                                    <TypeGitDeployment />
                                    {' '}
                                    <Form.Item label={(<span>Path &nbsp;</span>)}>
                                        <Input defaultValue="DESIGN/deployments/" />
                                    </Form.Item>
                                    {' '}

                                </div>
                            )
                                : type === 'DatabaseJDBC' ? <TypeDatabaseJDBC />
                                    : type === 'DatabaseJNDI' ? <TypeDatabaseJNDI />
                                        : type === 'AWSS3' ? <TypeAWSS3 />
                                            : type === 'AzureBlobStorage' ? <TypeAzure />
                                                : <TypeGitDeployment />}
                        </Form>
                    </Card>
                )}
            </Col>
        </DefaultLayout>
    )
}
