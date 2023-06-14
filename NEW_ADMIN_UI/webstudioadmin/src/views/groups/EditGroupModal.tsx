import React, { useEffect, useState } from 'react';
import { Button, Checkbox, Form, Input, Modal, Row, Table } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import CreateGroupDataSource from './CreateGroupDataSource';
import { AlignType } from 'rc-table/lib/interface'
import { stringify } from 'querystring';

interface EditGroupProps {
    group: {
        id: number;
        oldName: string
        name: string,
        description: string;
        roles: string[];
        privileges: string[];

    };
    updateGroup: (updatedGroup: any) => void;
    onSave: () => void;
}

export const EditGroupModal: React.FC<EditGroupProps> = ({ group, updateGroup, onSave }) => {

    const apiURL = process.env.REACT_APP_API_URL;
    const authorization = process.env.REACT_APP_AUTHORIZATION;

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [name, setName] = useState("");
    const [oldName, setOldName] = useState("");
    const [id, setId] = useState(0);
    const [description, setDescription] = useState("");
    const [selectedColumns, setSelectedColumns] = useState<string[]>([]);
    const [privilegesArray, setPrivilegesArray] = useState<string[]>([]);
    const [selectedGroupPrivileges, setSelectedGroupPrivileges] = useState<string[]>([]);
    const [privileges, setPrivileges] = useState<CheckboxValueType[]>(selectedGroupPrivileges);

    interface DataType {
        key: React.Key;
        privilege: string;
        administrators: string;
        analysts: string;
        deployers: string;
        developers: string;
        testers: string;
        viewers: string;
        [key: string]: string | React.Key;
    };

    const showModal = () => {
        setIsModalOpen(true);
    };

    const hideModal = () => {
        setIsModalOpen(false);
    };

    useEffect(() => {
        setName(group.name);
        setId(group.id);
        setDescription(group.description);
        setPrivileges(group.privileges);
        setSelectedGroupPrivileges(group.privileges);
        // setSelectedColumns([]);
    }, [group]);

    const handleNameInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setName(e.target.value);
    };

    const handleDescriptionInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setDescription(e.target.value);
    };

    const handleSave = async () => {
        const updatedGroup = {
            ...group,
            oldName: oldName,
            name: name,
            description: description,
            privileges: privileges.map((privilege) => privilege.toString()),
        };
        const encodedBody = stringify(updatedGroup);

        try {
            const headers = new Headers();
            headers.append('Authorization', authorization || '');
            headers.append('Content-Type', 'application/x-www-form-urlencoded');
            headers.append('Accept', 'application/json');

            const response = await fetch(`${apiURL}/admin/management/groups`, {
                method: "POST",
                headers,
                body: encodedBody
            });
            if (response.ok) {
                const responseData = await response.text();
                const groupData = responseData ? JSON.parse(responseData) : {};

                setName("");
                setId(group.id);
                setDescription(groupData.description);
                setPrivileges(groupData.privileges);
                setSelectedGroupPrivileges(groupData.privileges)
                updateGroup(updatedGroup);
                setIsModalOpen(false);
                console.log(groupData);
                console.log(updatedGroup);
                onSave();
            } else {
                console.error("Error updating group:", response.statusText);
            }
        } catch (error) {
            console.error("Error updating group:", error);
        }
    };

    const applyResult = (result: any) => {
        hideModal();
    };


    const selectPrivileges = (privilege: string) => {
        const selectedRows = CreateGroupDataSource.filter((row) => row[privilege] === "✓");
        const privileges = selectedRows.map((row) => row.key) as CheckboxValueType[];
        setPrivilegesArray(privileges.map(String));
        setPrivileges(privileges);
        console.log(privileges);
    };

    const rowSelection = {
        onChange: (selectedRowKeys: React.Key[], selectedRows: DataType[]) => {
            const privileges = selectedRows.map((row) => row.key) as CheckboxValueType[];
            setPrivilegesArray(privileges.map(String));
            setPrivileges(privileges);
            console.log(privileges);
        }
    };


    const columns = [
        {
            title: "Privilege",
            dataIndex: "privilege",
            key: "privilege",
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 95 }} onClick={() => selectPrivileges("administrators")}>
                        Administrators
                    </Button>
                </div>
            ),
            dataIndex: "administrators",
            key: "administrators",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("analysts")}>
                        Analysts
                    </Button>
                </div>
            ),
            dataIndex: "analysts",
            key: "analysts",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("deployers")} >
                        Deployers
                    </Button>
                </div>
            ),
            dataIndex: "deployers",
            key: "deployers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("developers")}>
                        Developers
                    </Button>
                </div>
            ),
            dataIndex: "developers",
            key: "developers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("testers")}>
                        Testers
                    </Button>
                </div>
            ),
            dataIndex: "testers",
            key: "testers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("viewers")}>
                        Viewers
                    </Button>
                </div>
            ),
            dataIndex: "viewers",
            key: "viewers",
            align: "center" as AlignType,
        },
    ];

    return (
        <div style={{ width: 850 }}>
            <Form layout="vertical" style={{ width: 850 }}>
                <Form.Item><b>Account</b></Form.Item>
                <Form.Item label="Name">
                    <Input id="name" value={name} onChange={handleNameInputChange} />
                </Form.Item>
                <Form.Item label="Description">
                    <Input id="description" value={description} onChange={handleDescriptionInputChange} />
                </Form.Item>
                <Form.Item><b>Group</b></Form.Item>
                <Form.Item className="group-create-form_last-form-item">
                    <Form.Item>
                        <Table
                            rowSelection={rowSelection}
                            dataSource={CreateGroupDataSource}
                            columns={columns}
                            pagination={false}
                            scroll={{ x: "max-content" }}
                        />
                    </Form.Item>
                </Form.Item>
                <Form.Item>
                    <Row style={{ float: "right" }}>
                        <Button
                            key="submit"
                            onClick={handleSave}
                            style={{
                                color: "green",
                                borderColor: "green"
                            }}
                        >
                            Save
                        </Button>
                    </Row>
                </Form.Item>
            </Form>
        </div>
    );
};