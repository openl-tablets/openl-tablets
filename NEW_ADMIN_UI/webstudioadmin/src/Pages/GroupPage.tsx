import React, { useState } from 'react';
import { Button, Card, Table, Tag, Form, Input, Divider, Row, Col } from 'antd';
import { useNavigate } from 'react-router-dom';
import { CloseCircleOutlined, CloseOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
import TableGroupInfo from 'views/groups/TableGroupInfo';
import { NewGroupModal } from 'views/groups/NewGroupModal';

export const GroupPage: React.FC = () => {

    const navigate = useNavigate();
    const navigateCreateGroup = () => {
        let path = `/groups/create`;
        navigate(path);
    }

    const columns = [
        {
            title: "Name",
            dataIndex: "name",
            key: "name",
        },
        {
            title: "Description",
            dataIndex: "description",
            key: "description",
        },
        {
            title: "Privileges",
            dataIndex: "privileges",
            key: "privileges",
            render: (privileges: string[]) => (
                <>
                    {privileges.map(privilege => {
                        let color = "grey";
                        // privilege === "Administrate" ? color = "red" : privilege === ("Developers" || "Testers" || "Viewers") ? color ="blue" : color = "grey";
                        privilege === "Administrate" ? color = "red" : ((privilege === "Developers") || (privilege === "Testers") || (privilege === "Viewers")) ? color = "blue" : color = "default";

                        return (
                            <Tag color={color} key={privilege} style={{ margin: 2 }}>
                                {privilege}
                            </Tag>
                        );
                    })}
                </>
            ),
        },
        {
            title: "Action",
            dataIndex: "Action",
            key: "Action",
            render: (key: string) => (
                <Button
                    type="text"
                    icon={<CloseCircleOutlined />}
                    onClick={() => setData(data.filter(item => item.key !== key))}
                >
                </Button>
            ),
        },

    ]

    const [data, setData] = useState(TableGroupInfo);

    const addNewGroup = (newGroup: { key: string; name: string; description: string; privileges: []; action: "" }) => {
        setData((data) => [...data, newGroup]);
    }

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Form>
                    <Form.Item label={
                        <span>
                            Default group for all users * &nbsp;
                        </span>
                    }
                    >
                        <Row>
                            <Col flex="auto"><Input /></Col>
                            <Col offset={1}><Button style={{ color: "green", borderColor: "green" }}>Apply and Restart</Button></Col>
                        </Row>
                    </Form.Item>
                </Form>
                <Divider />
                <Table columns={columns} dataSource={data} pagination={{ hideOnSinglePage: true }} />
                <NewGroupModal addNewGroup={addNewGroup} />
                {/* <Button onClick={navigateCreateGroup} style={{ marginTop: 10 }}>Add new group</Button> */}
            </Card>
        </DefaultLayout>
    )
};

