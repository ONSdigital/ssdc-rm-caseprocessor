from typing import List, Optional

from sqlalchemy import BigInteger, Boolean, CHAR, CheckConstraint, DateTime, ForeignKeyConstraint, Index, Integer, \
    LargeBinary, PrimaryKeyConstraint, String, UniqueConstraint, Uuid, text, create_engine, create_mock_engine, \
    Sequence, Identity
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
import datetime
import uuid


class Base(DeclarativeBase):
    pass


class ClusterLeader(Base):
    __tablename__ = 'cluster_leader'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='cluster_leader_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    host_last_seen_alive_at: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    host_name: Mapped[str] = mapped_column(String(255))


class EmailTemplate(Base):
    __tablename__ = 'email_template'
    __table_args__ = (
        PrimaryKeyConstraint('pack_code', name='email_template_pkey'),
        {'schema': 'casev3'}
    )

    pack_code: Mapped[str] = mapped_column(String(255), primary_key=True)
    description: Mapped[str] = mapped_column(String(255))
    notify_service_ref: Mapped[str] = mapped_column(String(255))
    notify_template_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    template: Mapped[dict] = mapped_column(JSONB)
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    action_rule_survey_email_template: Mapped[List['ActionRuleSurveyEmailTemplate']] = relationship(
        'ActionRuleSurveyEmailTemplate', back_populates='email_template')
    fulfilment_survey_email_template: Mapped[List['FulfilmentSurveyEmailTemplate']] = relationship(
        'FulfilmentSurveyEmailTemplate', back_populates='email_template')
    action_rule: Mapped[List['ActionRule']] = relationship('ActionRule', back_populates='email_template')


class ExportFileRow(Base):
    __tablename__ = 'export_file_row'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='export_file_row_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    batch_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    batch_quantity: Mapped[int] = mapped_column(Integer)
    export_file_destination: Mapped[str] = mapped_column(String(255))
    pack_code: Mapped[str] = mapped_column(String(255))
    row: Mapped[str] = mapped_column(String(5000))


class ExportFileTemplate(Base):
    __tablename__ = 'export_file_template'
    __table_args__ = (
        PrimaryKeyConstraint('pack_code', name='export_file_template_pkey'),
        {'schema': 'casev3'}
    )

    pack_code: Mapped[str] = mapped_column(String(255), primary_key=True)
    description: Mapped[str] = mapped_column(String(255))
    export_file_destination: Mapped[str] = mapped_column(String(255))
    template: Mapped[dict] = mapped_column(JSONB)
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    action_rule_survey_export_file_template: Mapped[List['ActionRuleSurveyExportFileTemplate']] = relationship(
        'ActionRuleSurveyExportFileTemplate', back_populates='export_file_template')
    fulfilment_survey_export_file_template: Mapped[List['FulfilmentSurveyExportFileTemplate']] = relationship(
        'FulfilmentSurveyExportFileTemplate', back_populates='export_file_template')
    action_rule: Mapped[List['ActionRule']] = relationship('ActionRule', back_populates='export_file_template')
    fulfilment_to_process: Mapped[List['FulfilmentToProcess']] = relationship('FulfilmentToProcess',
                                                                              back_populates='export_file_template')


class FulfilmentNextTrigger(Base):
    __tablename__ = 'fulfilment_next_trigger'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='fulfilment_next_trigger_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    trigger_date_time: Mapped[datetime.datetime] = mapped_column(DateTime(True))


class MessageToSend(Base):
    __tablename__ = 'message_to_send'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='message_to_send_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    destination_topic: Mapped[str] = mapped_column(String(255))
    message_body: Mapped[bytes] = mapped_column(LargeBinary)


class SmsTemplate(Base):
    __tablename__ = 'sms_template'
    __table_args__ = (
        PrimaryKeyConstraint('pack_code', name='sms_template_pkey'),
        {'schema': 'casev3'}
    )

    pack_code: Mapped[str] = mapped_column(String(255), primary_key=True)
    description: Mapped[str] = mapped_column(String(255))
    notify_service_ref: Mapped[str] = mapped_column(String(255))
    notify_template_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    template: Mapped[dict] = mapped_column(JSONB)
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    action_rule_survey_sms_template: Mapped[List['ActionRuleSurveySmsTemplate']] = relationship(
        'ActionRuleSurveySmsTemplate', back_populates='sms_template')
    fulfilment_survey_sms_template: Mapped[List['FulfilmentSurveySmsTemplate']] = relationship(
        'FulfilmentSurveySmsTemplate', back_populates='sms_template')
    action_rule: Mapped[List['ActionRule']] = relationship('ActionRule', back_populates='sms_template')


class Survey(Base):
    __tablename__ = 'survey'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='survey_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    sample_definition_url: Mapped[str] = mapped_column(String(255))
    sample_separator: Mapped[str] = mapped_column(CHAR(1))
    sample_validation_rules: Mapped[dict] = mapped_column(JSONB)
    sample_with_header_row: Mapped[bool] = mapped_column(Boolean)
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    def as_dict(self) -> dict[str, str | bool | dict]:
        """Returns a dictionary representation of the Survey object, in types which are compatible with JSON."""
        return {
            "id": str(self.id),
            "name": self.name,
            "sample_validation_rules": self.sample_validation_rules,
            "metadata": self.metadata_,
            "sample_definition_url": self.sample_definition_url,
            "sample_with_header_row": self.sample_with_header_row,
            "sample_separator": self.sample_separator,
        }

    @classmethod
    def from_dict(cls, survey_dict: dict[str, str | bool | dict]) -> "Survey":
        """Class method to create a Survey object based on values from a dictionary.

        :raises: :class:`KeyError`: When any of the non-nullable columns aren't present in the dictionary.
        """
        new_survey = Survey(
            name=survey_dict["name"],
            sample_validation_rules=survey_dict["sample_validation_rules"],
            sample_definition_url=survey_dict["sample_definition_url"],
            sample_with_header_row=survey_dict["sample_with_header_row"],
            sample_separator=survey_dict["sample_separator"],
            metadata_=survey_dict.get("metadata"),
        )

        return new_survey

    action_rule_survey_email_template: Mapped[List['ActionRuleSurveyEmailTemplate']] = relationship(
        'ActionRuleSurveyEmailTemplate', back_populates='survey')
    action_rule_survey_export_file_template: Mapped[List['ActionRuleSurveyExportFileTemplate']] = relationship(
        'ActionRuleSurveyExportFileTemplate', back_populates='survey')
    action_rule_survey_sms_template: Mapped[List['ActionRuleSurveySmsTemplate']] = relationship(
        'ActionRuleSurveySmsTemplate', back_populates='survey')
    collection_exercise: Mapped[List['CollectionExercise']] = relationship('CollectionExercise',
                                                                           back_populates='survey')
    fulfilment_survey_email_template: Mapped[List['FulfilmentSurveyEmailTemplate']] = relationship(
        'FulfilmentSurveyEmailTemplate', back_populates='survey')
    fulfilment_survey_export_file_template: Mapped[List['FulfilmentSurveyExportFileTemplate']] = relationship(
        'FulfilmentSurveyExportFileTemplate', back_populates='survey')
    fulfilment_survey_sms_template: Mapped[List['FulfilmentSurveySmsTemplate']] = relationship(
        'FulfilmentSurveySmsTemplate', back_populates='survey')
    user_group_permission: Mapped[List['UserGroupPermission']] = relationship('UserGroupPermission',
                                                                              back_populates='survey')


class UserGroup(Base):
    __tablename__ = 'user_group'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='user_group_pkey'),
        UniqueConstraint('name', name='user_group_name_key'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    description: Mapped[Optional[str]] = mapped_column(String(255))

    user_group_admin: Mapped[List['UserGroupAdmin']] = relationship('UserGroupAdmin', back_populates='group')
    user_group_member: Mapped[List['UserGroupMember']] = relationship('UserGroupMember', back_populates='group')
    user_group_permission: Mapped[List['UserGroupPermission']] = relationship('UserGroupPermission',
                                                                              back_populates='group')


class Users(Base):
    __tablename__ = 'users'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='users_pkey'),
        UniqueConstraint('email', name='users_email_idx'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    email: Mapped[str] = mapped_column(String(255))

    user_group_admin: Mapped[List['UserGroupAdmin']] = relationship('UserGroupAdmin', back_populates='user')
    user_group_member: Mapped[List['UserGroupMember']] = relationship('UserGroupMember', back_populates='user')


class ActionRuleSurveyEmailTemplate(Base):
    __tablename__ = 'action_rule_survey_email_template'
    __table_args__ = (
        ForeignKeyConstraint(['email_template_pack_code'], ['casev3.email_template.pack_code'],
                             name='fkfjx53yvq2f07lipml9kcm8qlb'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fkfucekff07exgw9xd5pd6wxc80'),
        PrimaryKeyConstraint('id', name='action_rule_survey_email_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    email_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    email_template: Mapped['EmailTemplate'] = relationship('EmailTemplate',
                                                           back_populates='action_rule_survey_email_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='action_rule_survey_email_template')


class ActionRuleSurveyExportFileTemplate(Base):
    __tablename__ = 'action_rule_survey_export_file_template'
    __table_args__ = (
        ForeignKeyConstraint(['export_file_template_pack_code'], ['casev3.export_file_template.pack_code'],
                             name='fkpeyvyyoxpqh7rvae2hxmg2wd2'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fkmtao7nj3x74iki19rygx5pdcl'),
        PrimaryKeyConstraint('id', name='action_rule_survey_export_file_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    export_file_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    export_file_template: Mapped['ExportFileTemplate'] = relationship('ExportFileTemplate',
                                                                      back_populates='action_rule_survey_export_file_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='action_rule_survey_export_file_template')


class ActionRuleSurveySmsTemplate(Base):
    __tablename__ = 'action_rule_survey_sms_template'
    __table_args__ = (
        ForeignKeyConstraint(['sms_template_pack_code'], ['casev3.sms_template.pack_code'],
                             name='fkrtyhiquv8tgdiv0sc2e5ovqld'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fkcksec9j9chi54k0fuhsywnfne'),
        PrimaryKeyConstraint('id', name='action_rule_survey_sms_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    sms_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    sms_template: Mapped['SmsTemplate'] = relationship('SmsTemplate', back_populates='action_rule_survey_sms_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='action_rule_survey_sms_template')


class CollectionExercise(Base):
    __tablename__ = 'collection_exercise'
    __table_args__ = (
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fkrv1ksptm37exmrbj0yutm6fla'),
        PrimaryKeyConstraint('id', name='collection_exercise_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    collection_instrument_selection_rules: Mapped[dict] = mapped_column(JSONB)
    end_date: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    name: Mapped[str] = mapped_column(String(255))
    reference: Mapped[str] = mapped_column(String(255))
    start_date: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    survey: Mapped['Survey'] = relationship('Survey', back_populates='collection_exercise')
    action_rule: Mapped[List['ActionRule']] = relationship('ActionRule', back_populates='collection_exercise')
    cases: Mapped[List['Case']] = relationship('Case', back_populates='collection_exercise')
    job: Mapped[List['Job']] = relationship('Job', back_populates='collection_exercise')


class FulfilmentSurveyEmailTemplate(Base):
    __tablename__ = 'fulfilment_survey_email_template'
    __table_args__ = (
        ForeignKeyConstraint(['email_template_pack_code'], ['casev3.email_template.pack_code'],
                             name='fk7yn9o3bjnbaor6e15h1cfolj6'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fktbsv7d3607v1drb4vilugvnk8'),
        PrimaryKeyConstraint('id', name='fulfilment_survey_email_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    email_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    email_template: Mapped['EmailTemplate'] = relationship('EmailTemplate',
                                                           back_populates='fulfilment_survey_email_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='fulfilment_survey_email_template')


class FulfilmentSurveyExportFileTemplate(Base):
    __tablename__ = 'fulfilment_survey_export_file_template'
    __table_args__ = (
        ForeignKeyConstraint(['export_file_template_pack_code'], ['casev3.export_file_template.pack_code'],
                             name='fkjit0455kk2vnpbr6cs9wxsggv'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fk5u3w6updqcovaf7p4mkl8wtub'),
        PrimaryKeyConstraint('id', name='fulfilment_survey_export_file_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    export_file_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    export_file_template: Mapped['ExportFileTemplate'] = relationship('ExportFileTemplate',
                                                                      back_populates='fulfilment_survey_export_file_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='fulfilment_survey_export_file_template')


class FulfilmentSurveySmsTemplate(Base):
    __tablename__ = 'fulfilment_survey_sms_template'
    __table_args__ = (
        ForeignKeyConstraint(['sms_template_pack_code'], ['casev3.sms_template.pack_code'],
                             name='fkqpoh4166ajt0h9qxwq43asj48'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fki9auhquvx2gipducjycr08ti1'),
        PrimaryKeyConstraint('id', name='fulfilment_survey_sms_template_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    sms_template_pack_code: Mapped[str] = mapped_column(String(255))
    survey_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    sms_template: Mapped['SmsTemplate'] = relationship('SmsTemplate', back_populates='fulfilment_survey_sms_template')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='fulfilment_survey_sms_template')


class UserGroupAdmin(Base):
    __tablename__ = 'user_group_admin'
    __table_args__ = (
        ForeignKeyConstraint(['group_id'], ['casev3.user_group.id'], name='fkc7secqw35qa62vst6c8fvmnkc'),
        ForeignKeyConstraint(['user_id'], ['casev3.users.id'], name='fk44cbs8vh8ugmfgduvjb9j02kj'),
        PrimaryKeyConstraint('id', name='user_group_admin_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    group_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    user_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    group: Mapped['UserGroup'] = relationship('UserGroup', back_populates='user_group_admin')
    user: Mapped['Users'] = relationship('Users', back_populates='user_group_admin')


class UserGroupMember(Base):
    __tablename__ = 'user_group_member'
    __table_args__ = (
        ForeignKeyConstraint(['group_id'], ['casev3.user_group.id'], name='fknyc05vqmhd9hq1hv6wexhdu4t'),
        ForeignKeyConstraint(['user_id'], ['casev3.users.id'], name='fkjbhg45atfwht2ji7xu241m4qp'),
        PrimaryKeyConstraint('id', name='user_group_member_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    group_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    user_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    group: Mapped['UserGroup'] = relationship('UserGroup', back_populates='user_group_member')
    user: Mapped['Users'] = relationship('Users', back_populates='user_group_member')


class UserGroupPermission(Base):
    __tablename__ = 'user_group_permission'
    __table_args__ = (
        CheckConstraint(
            "authorised_activity::text = ANY (ARRAY['SUPER_USER'::character varying, 'LIST_SURVEYS'::character varying, 'VIEW_SURVEY'::character varying, 'CREATE_SURVEY'::character varying, 'CREATE_EXPORT_FILE_TEMPLATE'::character varying, 'CREATE_SMS_TEMPLATE'::character varying, 'CREATE_EMAIL_TEMPLATE'::character varying, 'LIST_COLLECTION_EXERCISES'::character varying, 'VIEW_COLLECTION_EXERCISE'::character varying, 'CREATE_COLLECTION_EXERCISE'::character varying, 'ALLOW_EXPORT_FILE_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_EXPORT_FILE_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_SMS_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_SMS_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_EMAIL_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_EMAIL_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_EXPORT_FILE_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_EXPORT_FILE_TEMPLATES_ON_FULFILMENTS'::character varying, 'ALLOW_SMS_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_SMS_TEMPLATES_ON_FULFILMENTS'::character varying, 'ALLOW_EMAIL_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_EMAIL_TEMPLATES_ON_FULFILMENTS'::character varying, 'SEARCH_CASES'::character varying, 'VIEW_CASE_DETAILS'::character varying, 'LIST_ACTION_RULES'::character varying, 'CREATE_EXPORT_FILE_ACTION_RULE'::character varying, 'CREATE_FACE_TO_FACE_ACTION_RULE'::character varying, 'CREATE_OUTBOUND_PHONE_ACTION_RULE'::character varying, 'CREATE_DEACTIVATE_UAC_ACTION_RULE'::character varying, 'CREATE_SMS_ACTION_RULE'::character varying, 'CREATE_EMAIL_ACTION_RULE'::character varying, 'CREATE_EQ_FLUSH_ACTION_RULE'::character varying, 'LOAD_SAMPLE'::character varying, 'VIEW_SAMPLE_LOAD_PROGRESS'::character varying, 'LOAD_BULK_REFUSAL'::character varying, 'VIEW_BULK_REFUSAL_PROGRESS'::character varying, 'LOAD_BULK_UPDATE_SAMPLE_SENSITIVE'::character varying, 'LOAD_BULK_INVALID'::character varying, 'LOAD_BULK_UPDATE_SAMPLE'::character varying, 'VIEW_BULK_UPDATE_SAMPLE_SENSITIVE_PROGRESS'::character varying, 'VIEW_BULK_INVALID_PROGRESS'::character varying, 'VIEW_BULK_UPDATE_SAMPLE_PROGRESS'::character varying, 'DEACTIVATE_UAC'::character varying, 'CREATE_CASE_REFUSAL'::character varying, 'CREATE_CASE_INVALID_CASE'::character varying, 'CREATE_CASE_EXPORT_FILE_FULFILMENT'::character varying, 'CREATE_CASE_SMS_FULFILMENT'::character varying, 'CREATE_CASE_EMAIL_FULFILMENT'::character varying, 'UPDATE_SAMPLE'::character varying, 'UPDATE_SAMPLE_SENSITIVE'::character varying, 'LIST_EXPORT_FILE_TEMPLATES'::character varying, 'LIST_EXPORT_FILE_DESTINATIONS'::character varying, 'LIST_SMS_TEMPLATES'::character varying, 'LIST_EMAIL_TEMPLATES'::character varying, 'CONFIGURE_FULFILMENT_TRIGGER'::character varying, 'EXCEPTION_MANAGER_VIEWER'::character varying, 'EXCEPTION_MANAGER_PEEK'::character varying, 'EXCEPTION_MANAGER_QUARANTINE'::character varying, 'LIST_USERS'::character varying]::text[])",
            name='user_group_permission_authorised_activity_check'),
        ForeignKeyConstraint(['group_id'], ['casev3.user_group.id'], name='fkao3eqnwgryopngpoq65744h2m'),
        ForeignKeyConstraint(['survey_id'], ['casev3.survey.id'], name='fkep4hjlw1esp4s8p3row2syxjq'),
        PrimaryKeyConstraint('id', name='user_group_permission_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    group_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    authorised_activity: Mapped[Optional[str]] = mapped_column(String(255))
    survey_id: Mapped[Optional[uuid.UUID]] = mapped_column(Uuid)

    group: Mapped['UserGroup'] = relationship('UserGroup', back_populates='user_group_permission')
    survey: Mapped['Survey'] = relationship('Survey', back_populates='user_group_permission')


class ActionRule(Base):
    __tablename__ = 'action_rule'
    __table_args__ = (
        CheckConstraint(
            "action_rule_status::text = ANY (ARRAY['SCHEDULED'::character varying, 'SELECTING_CASES'::character varying, 'PROCESSING_CASES'::character varying, 'COMPLETED'::character varying, 'ERRORED'::character varying]::text[])",
            name='action_rule_action_rule_status_check'),
        CheckConstraint(
            "type::text = ANY (ARRAY['EXPORT_FILE'::character varying, 'OUTBOUND_TELEPHONE'::character varying, 'FACE_TO_FACE'::character varying, 'DEACTIVATE_UAC'::character varying, 'SMS'::character varying, 'EMAIL'::character varying, 'EQ_FLUSH'::character varying]::text[])",
            name='action_rule_type_check'),
        ForeignKeyConstraint(['collection_exercise_id'], ['casev3.collection_exercise.id'],
                             name='fk6twtf1ksysh99e4g2ejmoy6c1'),
        ForeignKeyConstraint(['email_template_pack_code'], ['casev3.email_template.pack_code'],
                             name='fkssc7f5mlut14gbb20282seiyn'),
        ForeignKeyConstraint(['export_file_template_pack_code'], ['casev3.export_file_template.pack_code'],
                             name='fk9fefdqv5a7vb04vu7gn6cad19'),
        ForeignKeyConstraint(['sms_template_pack_code'], ['casev3.sms_template.pack_code'],
                             name='fktnrm1hhiyehmygso5dsb6dv7a'),
        PrimaryKeyConstraint('id', name='action_rule_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    action_rule_status: Mapped[str] = mapped_column(String(255))
    created_by: Mapped[str] = mapped_column(String(255))
    has_triggered: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    trigger_date_time: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    type: Mapped[str] = mapped_column(String(255))
    collection_exercise_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    classifiers: Mapped[Optional[bytes]] = mapped_column(LargeBinary)
    description: Mapped[Optional[str]] = mapped_column(String(50))
    email_column: Mapped[Optional[str]] = mapped_column(String(255))
    phone_number_column: Mapped[Optional[str]] = mapped_column(String(255))
    selected_case_count: Mapped[Optional[int]] = mapped_column(Integer)
    uac_metadata: Mapped[Optional[dict]] = mapped_column(JSONB)
    email_template_pack_code: Mapped[Optional[str]] = mapped_column(String(255))
    export_file_template_pack_code: Mapped[Optional[str]] = mapped_column(String(255))
    sms_template_pack_code: Mapped[Optional[str]] = mapped_column(String(255))

    collection_exercise: Mapped['CollectionExercise'] = relationship('CollectionExercise', back_populates='action_rule')
    email_template: Mapped['EmailTemplate'] = relationship('EmailTemplate', back_populates='action_rule')
    export_file_template: Mapped['ExportFileTemplate'] = relationship('ExportFileTemplate',
                                                                      back_populates='action_rule')
    sms_template: Mapped['SmsTemplate'] = relationship('SmsTemplate', back_populates='action_rule')
    case_to_process: Mapped[List['CaseToProcess']] = relationship('CaseToProcess', back_populates='action_rule')

serial_seq = Sequence('cases_secret_sequence_number_seq')
class Case(Base):
    __tablename__ = 'cases'
    __table_args__ = (
        CheckConstraint(
            "refusal_received::text = ANY (ARRAY['HARD_REFUSAL'::character varying, 'EXTRAORDINARY_REFUSAL'::character varying, 'SOFT_REFUSAL'::character varying, 'WITHDRAWAL_REFUSAL'::character varying]::text[])",
            name='cases_refusal_received_check'),
        ForeignKeyConstraint(['collection_exercise_id'], ['casev3.collection_exercise.id'],
                             name='fkrl77p02uu7a253tn2ro5mitv5'),
        PrimaryKeyConstraint('id', name='cases_pkey'),
        Index('cases_case_ref_idx', 'case_ref'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    invalid: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    secret_sequence_number: Mapped[int] = mapped_column(Integer, server_default=serial_seq.next_value())
    collection_exercise_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    case_ref: Mapped[Optional[int]] = mapped_column(BigInteger)
    created_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    last_updated_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    refusal_received: Mapped[Optional[str]] = mapped_column(String(255))
    sample: Mapped[Optional[dict]] = mapped_column(JSONB)
    sample_sensitive: Mapped[Optional[dict]] = mapped_column(JSONB)

    collection_exercise: Mapped['CollectionExercise'] = relationship('CollectionExercise', back_populates='cases')
    case_to_process: Mapped[List['CaseToProcess']] = relationship('CaseToProcess', back_populates='caze')
    fulfilment_to_process: Mapped[List['FulfilmentToProcess']] = relationship('FulfilmentToProcess',
                                                                              back_populates='caze')
    uac_qid_link: Mapped[List['UacQidLink']] = relationship('UacQidLink', back_populates='caze')
    event: Mapped[List['Event']] = relationship('Event', back_populates='caze')


class Job(Base):
    __tablename__ = 'job'
    __table_args__ = (
        CheckConstraint(
            "job_status::text = ANY (ARRAY['FILE_UPLOADED'::character varying, 'STAGING_IN_PROGRESS'::character varying, 'VALIDATION_IN_PROGRESS'::character varying, 'VALIDATED_OK'::character varying, 'VALIDATED_WITH_ERRORS'::character varying, 'VALIDATED_TOTAL_FAILURE'::character varying, 'PROCESSING_IN_PROGRESS'::character varying, 'PROCESSED'::character varying, 'CANCELLED'::character varying]::text[])",
            name='job_job_status_check'),
        CheckConstraint(
            "job_type::text = ANY (ARRAY['SAMPLE'::character varying, 'BULK_REFUSAL'::character varying, 'BULK_UPDATE_SAMPLE_SENSITIVE'::character varying, 'BULK_INVALID'::character varying, 'BULK_UPDATE_SAMPLE'::character varying]::text[])",
            name='job_job_type_check'),
        ForeignKeyConstraint(['collection_exercise_id'], ['casev3.collection_exercise.id'],
                             name='fk6hra36ow5xge19dg3w1m7fd4r'),
        PrimaryKeyConstraint('id', name='job_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    created_by: Mapped[str] = mapped_column(String(255))
    error_row_count: Mapped[int] = mapped_column(Integer)
    file_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    file_name: Mapped[str] = mapped_column(String(255))
    file_row_count: Mapped[int] = mapped_column(Integer)
    job_status: Mapped[str] = mapped_column(String(255))
    job_type: Mapped[str] = mapped_column(String(255))
    processing_row_number: Mapped[int] = mapped_column(Integer)
    staging_row_number: Mapped[int] = mapped_column(Integer)
    validating_row_number: Mapped[int] = mapped_column(Integer)
    collection_exercise_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    cancelled_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    cancelled_by: Mapped[Optional[str]] = mapped_column(String(255))
    created_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    fatal_error_description: Mapped[Optional[str]] = mapped_column(String(255))
    last_updated_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    processed_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    processed_by: Mapped[Optional[str]] = mapped_column(String(255))

    collection_exercise: Mapped['CollectionExercise'] = relationship('CollectionExercise', back_populates='job')
    job_row: Mapped[List['JobRow']] = relationship('JobRow', back_populates='job')


class CaseToProcess(Base):
    __tablename__ = 'case_to_process'
    __table_args__ = (
        ForeignKeyConstraint(['action_rule_id'], ['casev3.action_rule.id'], name='fkmqcrb58vhx7a7qcyyjjvm1y31'),
        ForeignKeyConstraint(['caze_id'], ['casev3.cases.id'], name='fk104hqblc26y5xjehv2x8dg4k3'),
        PrimaryKeyConstraint('id', name='case_to_process_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    batch_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    batch_quantity: Mapped[int] = mapped_column(Integer)
    action_rule_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    caze_id: Mapped[uuid.UUID] = mapped_column(Uuid)

    action_rule: Mapped['ActionRule'] = relationship('ActionRule', back_populates='case_to_process')
    caze: Mapped['Case'] = relationship('Case', back_populates='case_to_process')


class FulfilmentToProcess(Base):
    __tablename__ = 'fulfilment_to_process'
    __table_args__ = (
        ForeignKeyConstraint(['caze_id'], ['casev3.cases.id'], name='fk9cu8edtrwirw777f4x1qej03m'),
        ForeignKeyConstraint(['export_file_template_pack_code'], ['casev3.export_file_template.pack_code'],
                             name='fkic5eccg0ms41mlfe7aqyelje9'),
        PrimaryKeyConstraint('id', name='fulfilment_to_process_pkey'),
        UniqueConstraint('message_id', name='fulfilment_to_process_message_id_key'),
        {'schema': 'casev3'}
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    correlation_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    message_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    caze_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    export_file_template_pack_code: Mapped[str] = mapped_column(String(255))
    batch_id: Mapped[Optional[uuid.UUID]] = mapped_column(Uuid)
    batch_quantity: Mapped[Optional[int]] = mapped_column(Integer)
    originating_user: Mapped[Optional[str]] = mapped_column(String(255))
    personalisation: Mapped[Optional[dict]] = mapped_column(JSONB)
    uac_metadata: Mapped[Optional[dict]] = mapped_column(JSONB)

    caze: Mapped['Case'] = relationship('Case', back_populates='fulfilment_to_process')
    export_file_template: Mapped['ExportFileTemplate'] = relationship('ExportFileTemplate',
                                                                      back_populates='fulfilment_to_process')


class JobRow(Base):
    __tablename__ = 'job_row'
    __table_args__ = (
        CheckConstraint(
            "job_row_status::text = ANY (ARRAY['STAGED'::character varying, 'VALIDATED_OK'::character varying, 'VALIDATED_ERROR'::character varying, 'PROCESSED'::character varying]::text[])",
            name='job_row_job_row_status_check'),
        ForeignKeyConstraint(['job_id'], ['casev3.job.id'], name='fk8motlil4mayre4vvdipnjime0'),
        PrimaryKeyConstraint('id', name='job_row_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    job_row_status: Mapped[str] = mapped_column(String(255))
    original_row_data: Mapped[bytes] = mapped_column(LargeBinary)
    original_row_line_number: Mapped[int] = mapped_column(Integer)
    job_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    row_data: Mapped[Optional[dict]] = mapped_column(JSONB)
    validation_error_descriptions: Mapped[Optional[bytes]] = mapped_column(LargeBinary)

    job: Mapped['Job'] = relationship('Job', back_populates='job_row')


class UacQidLink(Base):
    __tablename__ = 'uac_qid_link'
    __table_args__ = (
        ForeignKeyConstraint(['caze_id'], ['casev3.cases.id'], name='fkngo7bm72f0focdujjma78t4nk'),
        PrimaryKeyConstraint('id', name='uac_qid_link_pkey'),
        UniqueConstraint('qid', name='uac_qid_link_qid_key'),
        Index('qid_idx', 'qid'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    active: Mapped[bool] = mapped_column(Boolean, server_default=text('true'))
    collection_instrument_url: Mapped[str] = mapped_column(String(255))
    eq_launched: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    qid: Mapped[str] = mapped_column(String(255))
    receipt_received: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    uac: Mapped[str] = mapped_column(String(255))
    uac_hash: Mapped[str] = mapped_column(String(255))
    caze_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    created_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    last_updated_at: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    metadata_: Mapped[Optional[dict]] = mapped_column('metadata', JSONB)

    caze: Mapped['Case'] = relationship('Case', back_populates='uac_qid_link')
    event: Mapped[List['Event']] = relationship('Event', back_populates='uac_qid_link')


class Event(Base):
    __tablename__ = 'event'
    __table_args__ = (
        CheckConstraint(
            "type::text = ANY (ARRAY['NEW_CASE'::character varying, 'RECEIPT'::character varying, 'REFUSAL'::character varying, 'ERASE_DATA'::character varying, 'EQ_LAUNCH'::character varying, 'INVALID_CASE'::character varying, 'UAC_AUTHENTICATION'::character varying, 'TELEPHONE_CAPTURE'::character varying, 'PRINT_FULFILMENT'::character varying, 'EXPORT_FILE'::character varying, 'DEACTIVATE_UAC'::character varying, 'UPDATE_SAMPLE'::character varying, 'UPDATE_SAMPLE_SENSITIVE'::character varying, 'SMS_FULFILMENT'::character varying, 'ACTION_RULE_SMS_REQUEST'::character varying, 'EMAIL_FULFILMENT'::character varying, 'ACTION_RULE_EMAIL_REQUEST'::character varying, 'ACTION_RULE_SMS_CONFIRMATION'::character varying, 'ACTION_RULE_EMAIL_CONFIRMATION'::character varying]::text[])",
            name='event_type_check'),
        ForeignKeyConstraint(['caze_id'], ['casev3.cases.id'], name='fkhgvw8xq5panw486l3varef7pk'),
        ForeignKeyConstraint(['uac_qid_link_id'], ['casev3.uac_qid_link.id'], name='fkamu77co5m9upj2b3c1oun21er'),
        PrimaryKeyConstraint('id', name='event_pkey'),
        {'schema': 'casev3'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    channel: Mapped[str] = mapped_column(String(255))
    correlation_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    date_time: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    description: Mapped[str] = mapped_column(String(255))
    message_id: Mapped[uuid.UUID] = mapped_column(Uuid)
    message_timestamp: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    processed_at: Mapped[datetime.datetime] = mapped_column(DateTime(True))
    source: Mapped[str] = mapped_column(String(255))
    type: Mapped[str] = mapped_column(String(255))
    created_by: Mapped[Optional[str]] = mapped_column(String(255))
    payload: Mapped[Optional[dict]] = mapped_column(JSONB)
    caze_id: Mapped[Optional[uuid.UUID]] = mapped_column(Uuid)
    uac_qid_link_id: Mapped[Optional[uuid.UUID]] = mapped_column(Uuid)

    caze: Mapped['Case'] = relationship('Case', back_populates='event')
    uac_qid_link: Mapped['UacQidLink'] = relationship('UacQidLink', back_populates='event')

# with open("../../test.sql", mode='a') as f:
#
#     def dump(sql, *multiparams, **params):
#         stmt = sql.compile(dialect=engine.dialect)
#         # Append the statement delimiter, and a newline for legibility.
#         f.write(f'{stmt};\n')
#
#     engine = create_mock_engine("postgresql+psycopg2://appuser:postgres@localhost:6432/rm", dump)
#     Base.metadata.create_all(engine)
#     print("test")
