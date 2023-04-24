import React from 'react';
import { Button, Modal, Form, Input, Radio, Cascader, Checkbox } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';

export function CreateUser(){
// const UserCreateForm = Form.create({ name: 'form_in_modal' })(

//     class extends React.Component {
//     render() {
//       const { visible, onCancel, onCreate, form } = this.props;
//       const { getFieldDecorator } = form;

//       const displayOrder = [
//         {
//             value: "First last",
//             label: "First last",
//         },
//         {
//             value: "Last first",
//             label: "Last first",
//         },
//         {
//             value: "Other",
//             label: "Other",
//         },

//       ]
//       const groupOptions = [
//         {
//             value: "Administrators",
//             label: "Administrators",
//         },
//         {
//             value: "Analysts",
//             label: "Analysts",
//         },
//         {
//             value: "Deployers",
//             label: "Deployers",
//         },
//         {
//             value: "Developers",
//             label: "Developers",
//         },
//         {
//             value: "Testers",
//             label: "Testers",
//         },
//         {
//             value: "Viewers",
//             label: "Viewers",
//         },
//       ]

//       const onChange = (checkedValues: CheckboxValueType[]) => {
//         console.log('checked = ', checkedValues);
//       };

//       return (
//         <Modal
//           visible={visible}
//           title="Create a new User"
//           okText="Create"
//           onCancel={onCancel}
//           onOk={onCreate}
//         >
//           <Form layout="vertical">
//             <Form.Item><b>Account</b></Form.Item>
//             <Form.Item label="Username">
//               {getFieldDecorator('username', {
//                 rules: [{ required: true, message: 'Please input the username of new user!' }],
//               })(<Input />)}
//             </Form.Item>
//             <Form.Item label="Email">
//               {getFieldDecorator('email')(<Input/>)}
//             </Form.Item>
//             <Form.Item label="Password">
//               {getFieldDecorator('password', {
//                 rules: [{ required: true, message: 'Please input the password of new user!' }],
//               })(<Input />)}
//             </Form.Item>
//             <Form.Item><b>Name</b></Form.Item>
//             <Form.Item label="First name (Given name):">
//               {getFieldDecorator('first name')(<Input/>)}
//             </Form.Item>
//             <Form.Item label="Last name (Family name):">
//               {getFieldDecorator('last name')(<Input/>)}
//             </Form.Item>
//             <Form.Item label="Display name:">
//               {getFieldDecorator('display name')(<Cascader options={displayOrder} placeholder="First last" />)}
//             </Form.Item>
//             <Form.Item><b>Group</b></Form.Item>

//             <Form.Item className="user-create-form_last-form-item">
//               {getFieldDecorator('modifier', {
//                 initialValue: 'public',
//               })(
//                 <Checkbox.Group options={groupOptions} onChange={onChange} />,
//               )}
//             </Form.Item>
//           </Form>
//         </Modal>
//       );
//     }
//   },
// );

// class UsersPage extends React.Component {
//   state = {
//     visible: false,
//   };

//   showModal = () => {
//     this.setState({ visible: true });
//   };

//   handleCancel = () => {
//     this.setState({ visible: false });
//   };

//   handleCreate = () => {
//     const { form } = this.formRef.props;
//     form.validateFields((err, values) => {
//       if (err) {
//         return;
//       }

//       console.log('Received values of form: ', values);
//       form.resetFields();
//       this.setState({ visible: false });
//     });
//   };

//   saveFormRef = formRef => {
//     this.formRef = formRef;
//   };

//   render() {
//     return (
//       <div>
//         <Button type="primary" onClick={this.showModal}>
//           New Collection
//         </Button>
//         <UserCreateForm
//           wrappedComponentRef={this.saveFormRef}
//           visible={this.state.visible}
//           onCancel={this.handleCancel}
//           onCreate={this.handleCreate}
//         />
//       </div>
//     );
//   }
// }
// }

return (
    <div>a</div>
)
}