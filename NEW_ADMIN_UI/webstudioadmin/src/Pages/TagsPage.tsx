import { Card, Input, Divider, Button, Checkbox } from "antd";
import TextArea from "antd/es/input/TextArea";
import { AdminMenu } from "components/AdminMenu";
import { HeaderMenu } from "components/HeaderMenu";


export function TagsPage() {
    return (
        <div>
            <div>
                <HeaderMenu />
            </div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />

                <Card style={{ width: 500, margin: 20 }}>
                    <p><b>Tag Types and Values</b></p>
                    <p><b>Tag type</b> is a category that includes tag values of the same group. For example, the Product tag type can include tags Auto, Life, and Home.

                    </p>
                    <p>Proceed as follows:</p>
                    <ul>
                        <li>To add a tag type, in the <b>New Tag Type</b> field, enter the tag type name and press <b>Enter</b> or <b>Tab</b>. The tag type is added, and fields for tag values appear.</li>
                        <li>To add a tag value, in the <b>New Tag</b> field, enter the tag name and press <b>Enter</b>.
                        </li>
                    </ul>
                    <p>All created tag types and values are saved automatically.

                    </p>
                    <Input placeholder="New Tag Type" />
                    <Divider />
                    <p><b>Tags from a Project Name</b></p>
                    <p>Tags can be extracted from a project name using a project name template.</p>
                    <p>Each template must be defined on its own line. The order of the templates is important: the first template has the highest priority, the last template has the lowest priority.

                    </p>
                    <p>Tag types are wrapped with the percentage '%' symbol.

                    </p>
                    <p>'<b>?</b>' stands for any symbol.

                    </p>
                    <p>'<b>*</b>' stands for any text of any length.

                    </p>
                    <p><b>Example:

                    </b></p>
                    <p>For the <b>%Domain%-%LOB%-*</b> template, for the <b>Policy-L&A-rules</b> project, the tags are <b>Policy</b> for the <b>Domain</b> tag type and <b>L&A</b> for <b>LOB</b>.

                    </p>
                    <p>Project name templates:</p>
                    <TextArea />
                    <Button style={{ marginTop: 20, marginRight: 15 }}>Save templates</Button>
                    <Button>Fill tags for projects</Button>
                </Card>
            </div>
        </div>
    )
}