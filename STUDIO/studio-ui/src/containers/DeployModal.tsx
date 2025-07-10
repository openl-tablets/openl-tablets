import React, { useState, useEffect, useMemo } from 'react'
import { Modal, Form, Switch, Button, Row, Col } from 'antd'
import { RocketOutlined, BranchesOutlined } from '@ant-design/icons'
import { useGlobalEvents } from '../hooks'
import { Select } from '../components/form'

/**
 * DeployModal component
 * @example to call this modal, dispatch a custom event 'openDeployModal' with details:
 * window.dispatchEvent(new CustomEvent('openDeployModal', {detail: {test:'test'}}))
 */
export const DeployModal: React.FC = () => {
    const [form] = Form.useForm()
    const { details }: { details: { title?: string, initialValues?: any } | null } = useGlobalEvents('openDeployModal')
    const [visible, setVisible] = useState(false)
    const [searchString, setSearchString] = useState('')
    const [deploymentNames, setDeploymentNames] = useState<string[]>([])

    const fetchDeploymentNames = async () => {
        // TODO: fetch deployment names from the API
        // const response: string[] = await apiCall(?search=${searchString}`)
        setDeploymentNames([])
    }

    useEffect(() => {
        if (searchString) {
            fetchDeploymentNames()
        }
    }, [searchString])

    useEffect(() => {
        const hasDetails = details && Object.keys(details).length > 0
        setVisible(hasDetails)
        if (hasDetails) {
            form.setFieldsValue(details.initialValues || {})
        }
    }, [details, form])

    const handleClose = () => {
        window.dispatchEvent(new CustomEvent('openModal', { detail: null }))
    }

    const handleDeploy = () => {
        form.validateFields()
            .then(values => {
                console.warn('Deploying with values:', values)
                handleClose()
            })
            .catch(info => {
                console.error('Validate Failed:', info)
            })
    }

    const handleSearchDeploymentName = (newValue: string) => {
        setSearchString(newValue)
    }

    const handleChangeDeploymentName = (newValue: string) => {
        if (newValue) {
            setSearchString('')
            form.setFieldsValue({ deploymentName: newValue })
        }
    }

    const onBlurDeploymentName = () => {
        if (searchString) {
            form.setFieldsValue({ deploymentName: searchString })
        }
    }

    const deploymentNameOptions = useMemo(() => {
        return deploymentNames.map(group => ({
            value: group,
            label: group,
        }))
    }, [deploymentNames])

    return (
        <Modal
            onCancel={handleClose}
            open={visible}
            width={600}
            footer={[
                <Button key="cancel" onClick={handleClose}>
                    Cancel
                </Button>,
                <Button key="deploy" onClick={handleDeploy} type="primary">
                    Deploy
                </Button>,
            ]}
            title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <RocketOutlined style={{ marginRight: 8 }} />
                    {details?.title || 'Deploy rules-deploy.xml'}
                </div>
            }
        >
            <Form form={form} layout="vertical" name="deploy_form">
                <Select
                    label="Repository"
                    name="repository"
                    suffixIcon={<BranchesOutlined />}
                    options={[
                        { value: 'dev', label: <div><BranchesOutlined /> Dev</div> },
                    ]}
                />
                <Select
                    showSearch
                    defaultActiveFirstOption={false}
                    filterOption={false}
                    label="Deployment Name"
                    name="deploymentName"
                    notFoundContent={null}
                    onBlur={onBlurDeploymentName}
                    onChange={handleChangeDeploymentName}
                    onSearch={handleSearchDeploymentName}
                    options={deploymentNameOptions}
                    style={{ width: '100%' }}
                    suffixIcon={null}
                />
                <Select
                    label="Revision"
                    name="revision"
                    options={[]} // Options for revisions would be populated here
                    placeholder="Select revision"
                />
                <Form.Item label="" name="dependentProjects" valuePropName="checked">
                    <Row>
                        <Col>
                            <Switch />
                        </Col>
                        <Col>
                            <span style={{ marginLeft: 8 }}>Dependent projects</span>
                        </Col>
                    </Row>
                </Form.Item>
            </Form>
        </Modal>
    )
}
