import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";
import { Card, Form, Input, Cascader, Checkbox } from "antd";
import { RepositoryPage } from "./RepositoryPage";


export function RepoDeployConfPage() {

    const options = [
        {
            value: "Design",
            label: "Design",
        }
    ]
    return (
        <div>

            <RepositoryPage />

            <Card bordered={true}
                style={{
                    width: 500, margin: 20
                }}>
                <Form >
                    <Form.Item
                        label={
                            <span>
                                Use design repository &nbsp;
                            </span>
                        }
                    >
                        <Checkbox />
                    </Form.Item>
                    <Form.Item
                        label={
                            <span>
                                Repository &nbsp;
                            </span>
                        }
                    >
                            <Cascader options={options} placeholder="Design" />
                    </Form.Item>
                </Form>
            </Card>

        </div>
    )
}