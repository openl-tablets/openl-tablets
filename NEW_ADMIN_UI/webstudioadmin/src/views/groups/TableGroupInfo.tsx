interface DataType {
    key: React.Key;
    name: string;
    description: string;
    privileges: string[];
    action: string;
}

const TableGroupInfo: DataType[] = [
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
        privileges: ["Viewers", "Create Projects", "Create Tables", "Erase Projects", "Remove Tables", "Edit Projects", "Edit Tables", "Delete Projects"],
        action: "",
    },
    {
        key: "5",
        name: "Testers",
        description: "",
        privileges: ["Viewers", "Trace Tables", "Benchmark Tables", "Run Tables"],
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

export default TableGroupInfo;