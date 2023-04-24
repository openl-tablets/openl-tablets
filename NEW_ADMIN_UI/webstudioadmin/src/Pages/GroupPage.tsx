
import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";
import { Card, Table, Tag } from 'antd';
import { ModalNewGroup } from "../components/ModalNewGroup";


export function GroupPage() {

    interface DataType {
        key: React.Key;
        name: string;
        description: string;
        privileges: string[];
        action: string;
      }

    const groupInfo:DataType[] = [
        {
            key: "1",
            name: "Administrators",
            description: "",
            privileges: ["Administrate"],
            action: "",
        },
        {
            key: "2",
            name: "Analysts",
            description: "",
            privileges: ["Developers", "Testers"],
            action: "",
        },
        {
            key: "3",
            name: "Deployers",
            description: "",
            privileges: ["Viewers", "Delete Deploy Configuration", "Erase Deploy Configuration", "Create Deploy Configuration", "Deploy Projects", "Edit Deploy Configuration"],
            action: "",
        },
        {
            key: "4",
            name: "Developers",
            description: "",
            privileges: ["Viewers", "Create Projects","Create Tables","Erase Projects", "Remove Tables", "Edit Projects", "Edit Tables", "Delete Projects"],
            action: "",
        },
        {
            key: "5",
            name: "Testers",
            description: "",
            privileges: ["Viewers","Trace Tables","Benchmark Tables", "Run Tables"],
            action: "",
        },
        {
            key: "6",
            name: "Viewers",
            description: "",
            privileges: ["View projects"],
            action: "",
        },
    ]

    const columns = [
        {
            title: 'Name',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: 'Privileges',
            dataIndex: 'privileges',
            key: 'privileges',
            render: (privileges: string[]) => (
                <>
                    {privileges.map(privilege => {
                        let color = "grey";
                        // privilege === "Administrate" ? color = "red" : privilege === ("Developers" || "Testers" || "Viewers") ? color ="blue" : color = "grey";
                        privilege === "Administrate" ? color = "red" : ((privilege === "Developers") || (privilege === "Testers") || (privilege ==="Viewers")) ? color ="blue" : color = "default";

                        return (
                            <Tag color={color} key={privilege} >
                                {privilege}
                            </Tag>
                        );
                    })}
                </>
            ),
        },
        {
            title: 'Action',
            dataIndex: 'Action',
            key: 'Action',
            // render: <span>
            //     <a>Delete</a>
            // </span>
        },

    ]

    return (
        <div>
            <div>
                <HeaderMenu />
            </div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20 }}>
                    <Table columns={columns} dataSource={groupInfo} />
                    <ModalNewGroup/>
                </Card>
            </div>
        </div>
    )
}