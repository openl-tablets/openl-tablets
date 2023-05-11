import React, { useState } from 'react';
import { Button, Card, Table, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { CloseOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
import TableGroupInfo from 'views/groups/TableGroupInfo';

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
                    icon={<CloseOutlined />}
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
            <Card style={{ margin: 20 }}>
                <Table columns={columns} dataSource={data} pagination={{ hideOnSinglePage: true }} />
                <Button onClick={navigateCreateGroup} style={{ marginTop: 10 }}>Add new group</Button>
            </Card>
        </DefaultLayout>
    )
};

