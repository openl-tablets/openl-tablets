import { Input, Divider, Button, Col, Row, Typography } from 'antd'
import TextArea from 'antd/es/input/TextArea'
import React from 'react'
import { Trans, useTranslation } from 'react-i18next'

export const Tags: React.FC = () => {
    const { t } = useTranslation()

    return (
        <>
            <Typography.Title level={4}>
                {t('tags:tag_types_and_values')}
            </Typography.Title>
            <Trans
                components={[ <b />, <p /> ]}
                i18nKey="tags:tag_type_description"
            />
            <ul>
                <li>
                    <Trans
                        components={[ <b /> ]}
                        i18nKey="tags:tag_type_instruction_p1"
                    />
                </li>
                <li>
                    <Trans
                        components={[ <b /> ]}
                        i18nKey="tags:tag_type_instruction_p2"
                    />
                </li>
            </ul>
            <p>
                {t('tags:tag_type_auto_save_notice')}
            </p>
            <Input name="tag" placeholder={t('tags:tag_input_placeholder')} style={{ width: 400 }} />
            <Divider />
            <Typography.Title level={4}>
                {t('tags:tags_from_a_project_name')}
            </Typography.Title>
            <p>
                {t('tags:tag_project_instruction_p1')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p2')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p3')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p4')}
            </p>
            <p>
                {t('tags:tag_project_instruction_p5')}
            </p>
            <p>
                <b>{t('tags:example') + ':'}</b>
            </p>
            <p>
                <Trans
                    components={[ <b /> ]}
                    i18nKey="tags:example_template"
                />
            </p>
            <p>
                {t('tags:project_name_templates') + ':'}
            </p>
            <Row>
                <TextArea style={{ width: 400, height: 100 }} />
            </Row>
            <Row justify="end">
                <Button style={{ marginTop: 20, marginRight: 15 }}>{t('tags:save_templates')}</Button>
                <Button style={{ marginTop: 20, marginRight: 15 }}>{t('tags:fill_tags_for_project')}</Button>
            </Row>
        </>
    )
}
