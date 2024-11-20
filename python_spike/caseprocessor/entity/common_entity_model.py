# coding: utf-8
from sqlalchemy import BigInteger, Boolean, CHAR, CheckConstraint, Column, DateTime, ForeignKey, Integer, LargeBinary, String, text, Text, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base

"""
SPIKE NOTES:
 I had to import sqlalchemy.Text since it didn't already have it imported
 I had to add default(func.now) to event - this isn't in the hibernate definition either just needed it as I didn't want to spend time messing with the old spike code
"""


Base = declarative_base()
metadata = Base.metadata


class ClusterLeader(Base):
    __tablename__ = 'cluster_leader'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    host_last_seen_alive_at = Column(DateTime(True), nullable=False)
    host_name = Column(String(255), nullable=False)


class EmailTemplate(Base):
    __tablename__ = 'email_template'
    __table_args__ = {'schema': 'casev3'}

    pack_code = Column(String(255), primary_key=True)
    description = Column(String(255), nullable=False)
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    notify_service_ref = Column(String(255), nullable=False)
    notify_template_id = Column(UUID, nullable=False)
    template = Column(JSONB(astext_type=Text()), nullable=False)


class ExportFileRow(Base):
    __tablename__ = 'export_file_row'
    __table_args__ = {'schema': 'casev3'}

    id = Column(BigInteger, primary_key=True, server_default=text("nextval('casev3.export_file_row_id_seq'::regclass)"))
    batch_id = Column(UUID, nullable=False)
    batch_quantity = Column(Integer, nullable=False)
    export_file_destination = Column(String(255), nullable=False)
    pack_code = Column(String(255), nullable=False)
    row = Column(String(5000), nullable=False)


class ExportFileTemplate(Base):
    __tablename__ = 'export_file_template'
    __table_args__ = {'schema': 'casev3'}

    pack_code = Column(String(255), primary_key=True)
    description = Column(String(255), nullable=False)
    export_file_destination = Column(String(255), nullable=False)
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    template = Column(JSONB(astext_type=Text()), nullable=False)


class FulfilmentNextTrigger(Base):
    __tablename__ = 'fulfilment_next_trigger'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    trigger_date_time = Column(DateTime(True), nullable=False)


class MessageToSend(Base):
    __tablename__ = 'message_to_send'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    destination_topic = Column(String(255), nullable=False)
    message_body = Column(LargeBinary, nullable=False)


class SmsTemplate(Base):
    __tablename__ = 'sms_template'
    __table_args__ = {'schema': 'casev3'}

    pack_code = Column(String(255), primary_key=True)
    description = Column(String(255), nullable=False)
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    notify_service_ref = Column(String(255), nullable=False)
    notify_template_id = Column(UUID, nullable=False)
    template = Column(JSONB(astext_type=Text()), nullable=False)


class Survey(Base):
    __tablename__ = 'survey'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    name = Column(String(255), nullable=False)
    sample_definition_url = Column(String(255), nullable=False)
    sample_separator = Column(CHAR(255), nullable=False)
    sample_validation_rules = Column(JSONB(astext_type=Text()), nullable=False)
    sample_with_header_row = Column(Boolean, nullable=False)


class UserGroup(Base):
    __tablename__ = 'user_group'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    description = Column(String(255))
    name = Column(String(255), nullable=False, unique=True)


class User(Base):
    __tablename__ = 'users'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    email = Column(String(255), nullable=False, unique=True)


class ActionRuleSurveyEmailTemplate(Base):
    __tablename__ = 'action_rule_survey_email_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    email_template_pack_code = Column(ForeignKey('casev3.email_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    email_template = relationship('EmailTemplate')
    survey = relationship('Survey')


class ActionRuleSurveyExportFileTemplate(Base):
    __tablename__ = 'action_rule_survey_export_file_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    export_file_template_pack_code = Column(ForeignKey('casev3.export_file_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    export_file_template = relationship('ExportFileTemplate')
    survey = relationship('Survey')


class ActionRuleSurveySmsTemplate(Base):
    __tablename__ = 'action_rule_survey_sms_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    sms_template_pack_code = Column(ForeignKey('casev3.sms_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    sms_template = relationship('SmsTemplate')
    survey = relationship('Survey')


class CollectionExercise(Base):
    __tablename__ = 'collection_exercise'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    collection_instrument_selection_rules = Column(JSONB(astext_type=Text()), nullable=False)
    end_date = Column(DateTime(True), nullable=False)
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    name = Column(String(255), nullable=False)
    reference = Column(String(255), nullable=False)
    start_date = Column(DateTime(True), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    survey = relationship('Survey')


class FulfilmentSurveyEmailTemplate(Base):
    __tablename__ = 'fulfilment_survey_email_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    email_template_pack_code = Column(ForeignKey('casev3.email_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    email_template = relationship('EmailTemplate')
    survey = relationship('Survey')


class FulfilmentSurveyExportFileTemplate(Base):
    __tablename__ = 'fulfilment_survey_export_file_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    export_file_template_pack_code = Column(ForeignKey('casev3.export_file_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    export_file_template = relationship('ExportFileTemplate')
    survey = relationship('Survey')


class FulfilmentSurveySmsTemplate(Base):
    __tablename__ = 'fulfilment_survey_sms_template'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    sms_template_pack_code = Column(ForeignKey('casev3.sms_template.pack_code'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'), nullable=False)

    sms_template = relationship('SmsTemplate')
    survey = relationship('Survey')


class UserGroupAdmin(Base):
    __tablename__ = 'user_group_admin'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    group_id = Column(ForeignKey('casev3.user_group.id'), nullable=False)
    user_id = Column(ForeignKey('casev3.users.id'), nullable=False)

    group = relationship('UserGroup')
    user = relationship('User')


class UserGroupMember(Base):
    __tablename__ = 'user_group_member'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    group_id = Column(ForeignKey('casev3.user_group.id'), nullable=False)
    user_id = Column(ForeignKey('casev3.users.id'), nullable=False)

    group = relationship('UserGroup')
    user = relationship('User')


class UserGroupPermission(Base):
    __tablename__ = 'user_group_permission'
    __table_args__ = (
        CheckConstraint("(authorised_activity)::text = ANY ((ARRAY['SUPER_USER'::character varying, 'LIST_SURVEYS'::character varying, 'VIEW_SURVEY'::character varying, 'CREATE_SURVEY'::character varying, 'CREATE_EXPORT_FILE_TEMPLATE'::character varying, 'CREATE_SMS_TEMPLATE'::character varying, 'CREATE_EMAIL_TEMPLATE'::character varying, 'LIST_COLLECTION_EXERCISES'::character varying, 'VIEW_COLLECTION_EXERCISE'::character varying, 'CREATE_COLLECTION_EXERCISE'::character varying, 'ALLOW_EXPORT_FILE_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_EXPORT_FILE_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_SMS_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_SMS_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_EMAIL_TEMPLATE_ON_ACTION_RULE'::character varying, 'LIST_ALLOWED_EMAIL_TEMPLATES_ON_ACTION_RULES'::character varying, 'ALLOW_EXPORT_FILE_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_EXPORT_FILE_TEMPLATES_ON_FULFILMENTS'::character varying, 'ALLOW_SMS_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_SMS_TEMPLATES_ON_FULFILMENTS'::character varying, 'ALLOW_EMAIL_TEMPLATE_ON_FULFILMENT'::character varying, 'LIST_ALLOWED_EMAIL_TEMPLATES_ON_FULFILMENTS'::character varying, 'SEARCH_CASES'::character varying, 'VIEW_CASE_DETAILS'::character varying, 'LIST_ACTION_RULES'::character varying, 'CREATE_EXPORT_FILE_ACTION_RULE'::character varying, 'CREATE_FACE_TO_FACE_ACTION_RULE'::character varying, 'CREATE_OUTBOUND_PHONE_ACTION_RULE'::character varying, 'CREATE_DEACTIVATE_UAC_ACTION_RULE'::character varying, 'CREATE_SMS_ACTION_RULE'::character varying, 'CREATE_EMAIL_ACTION_RULE'::character varying, 'CREATE_EQ_FLUSH_ACTION_RULE'::character varying, 'LOAD_SAMPLE'::character varying, 'VIEW_SAMPLE_LOAD_PROGRESS'::character varying, 'LOAD_BULK_REFUSAL'::character varying, 'VIEW_BULK_REFUSAL_PROGRESS'::character varying, 'LOAD_BULK_UPDATE_SAMPLE_SENSITIVE'::character varying, 'LOAD_BULK_INVALID'::character varying, 'LOAD_BULK_UPDATE_SAMPLE'::character varying, 'VIEW_BULK_UPDATE_SAMPLE_SENSITIVE_PROGRESS'::character varying, 'VIEW_BULK_INVALID_PROGRESS'::character varying, 'VIEW_BULK_UPDATE_SAMPLE_PROGRESS'::character varying, 'DEACTIVATE_UAC'::character varying, 'CREATE_CASE_REFUSAL'::character varying, 'CREATE_CASE_INVALID_CASE'::character varying, 'CREATE_CASE_EXPORT_FILE_FULFILMENT'::character varying, 'CREATE_CASE_SMS_FULFILMENT'::character varying, 'CREATE_CASE_EMAIL_FULFILMENT'::character varying, 'UPDATE_SAMPLE'::character varying, 'UPDATE_SAMPLE_SENSITIVE'::character varying, 'LIST_EXPORT_FILE_TEMPLATES'::character varying, 'LIST_EXPORT_FILE_DESTINATIONS'::character varying, 'LIST_SMS_TEMPLATES'::character varying, 'LIST_EMAIL_TEMPLATES'::character varying, 'CONFIGURE_FULFILMENT_TRIGGER'::character varying, 'EXCEPTION_MANAGER_VIEWER'::character varying, 'EXCEPTION_MANAGER_PEEK'::character varying, 'EXCEPTION_MANAGER_QUARANTINE'::character varying, 'LIST_USERS'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    authorised_activity = Column(String(255))
    group_id = Column(ForeignKey('casev3.user_group.id'), nullable=False)
    survey_id = Column(ForeignKey('casev3.survey.id'))

    group = relationship('UserGroup')
    survey = relationship('Survey')


class ActionRule(Base):
    __tablename__ = 'action_rule'
    __table_args__ = (
        CheckConstraint("(type)::text = ANY ((ARRAY['EXPORT_FILE'::character varying, 'OUTBOUND_TELEPHONE'::character varying, 'FACE_TO_FACE'::character varying, 'DEACTIVATE_UAC'::character varying, 'SMS'::character varying, 'EMAIL'::character varying, 'EQ_FLUSH'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    classifiers = Column(LargeBinary)
    created_by = Column(String(255), nullable=False)
    email_column = Column(String(255))
    has_triggered = Column(Boolean, nullable=False, server_default=text("false"))
    phone_number_column = Column(String(255))
    trigger_date_time = Column(DateTime(True), nullable=False)
    type = Column(String(255), nullable=False)
    uac_metadata = Column(JSONB(astext_type=Text()))
    collection_exercise_id = Column(ForeignKey('casev3.collection_exercise.id'), nullable=False)
    email_template_pack_code = Column(ForeignKey('casev3.email_template.pack_code'))
    export_file_template_pack_code = Column(ForeignKey('casev3.export_file_template.pack_code'))
    sms_template_pack_code = Column(ForeignKey('casev3.sms_template.pack_code'))

    collection_exercise = relationship('CollectionExercise')
    email_template = relationship('EmailTemplate')
    export_file_template = relationship('ExportFileTemplate')
    sms_template = relationship('SmsTemplate')


class Case(Base):
    __tablename__ = 'cases'
    __table_args__ = (
        CheckConstraint("(refusal_received)::text = ANY ((ARRAY['HARD_REFUSAL'::character varying, 'EXTRAORDINARY_REFUSAL'::character varying, 'SOFT_REFUSAL'::character varying, 'WITHDRAWAL_REFUSAL'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    case_ref = Column(BigInteger, index=True)
    created_at = Column(DateTime(True))
    invalid = Column(Boolean, nullable=False, server_default=text("false"))
    last_updated_at = Column(DateTime(True))
    refusal_received = Column(String(255))
    sample = Column(JSONB(astext_type=Text()))
    sample_sensitive = Column(JSONB(astext_type=Text()))
    secret_sequence_number = Column(Integer, nullable=False, server_default=text("nextval('casev3.cases_secret_sequence_number_seq'::regclass)"))
    collection_exercise_id = Column(ForeignKey('casev3.collection_exercise.id'), nullable=False)

    collection_exercise = relationship('CollectionExercise')


class Job(Base):
    __tablename__ = 'job'
    __table_args__ = (
        CheckConstraint("(job_status)::text = ANY ((ARRAY['FILE_UPLOADED'::character varying, 'STAGING_IN_PROGRESS'::character varying, 'VALIDATION_IN_PROGRESS'::character varying, 'VALIDATED_OK'::character varying, 'VALIDATED_WITH_ERRORS'::character varying, 'VALIDATED_TOTAL_FAILURE'::character varying, 'PROCESSING_IN_PROGRESS'::character varying, 'PROCESSED'::character varying, 'CANCELLED'::character varying])::text[])"),
        CheckConstraint("(job_type)::text = ANY ((ARRAY['SAMPLE'::character varying, 'BULK_REFUSAL'::character varying, 'BULK_UPDATE_SAMPLE_SENSITIVE'::character varying, 'BULK_INVALID'::character varying, 'BULK_UPDATE_SAMPLE'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    cancelled_at = Column(DateTime(True))
    cancelled_by = Column(String(255))
    created_at = Column(DateTime(True))
    created_by = Column(String(255), nullable=False)
    error_row_count = Column(Integer, nullable=False)
    fatal_error_description = Column(String(255))
    file_id = Column(UUID, nullable=False)
    file_name = Column(String(255), nullable=False)
    file_row_count = Column(Integer, nullable=False)
    job_status = Column(String(255), nullable=False)
    job_type = Column(String(255), nullable=False)
    last_updated_at = Column(DateTime(True))
    processed_at = Column(DateTime(True))
    processed_by = Column(String(255))
    processing_row_number = Column(Integer, nullable=False)
    staging_row_number = Column(Integer, nullable=False)
    validating_row_number = Column(Integer, nullable=False)
    collection_exercise_id = Column(ForeignKey('casev3.collection_exercise.id'), nullable=False)

    collection_exercise = relationship('CollectionExercise')


class CaseToProces(Base):
    __tablename__ = 'case_to_process'
    __table_args__ = {'schema': 'casev3'}

    id = Column(BigInteger, primary_key=True, server_default=text("nextval('casev3.case_to_process_id_seq'::regclass)"))
    batch_id = Column(UUID, nullable=False)
    batch_quantity = Column(Integer, nullable=False)
    action_rule_id = Column(ForeignKey('casev3.action_rule.id'), nullable=False)
    caze_id = Column(ForeignKey('casev3.cases.id'), nullable=False)

    action_rule = relationship('ActionRule')
    caze = relationship('Case')


class FulfilmentToProces(Base):
    __tablename__ = 'fulfilment_to_process'
    __table_args__ = {'schema': 'casev3'}

    id = Column(BigInteger, primary_key=True, server_default=text("nextval('casev3.fulfilment_to_process_id_seq'::regclass)"))
    batch_id = Column(UUID)
    batch_quantity = Column(Integer)
    correlation_id = Column(UUID, nullable=False)
    message_id = Column(UUID, nullable=False, unique=True)
    originating_user = Column(String(255))
    personalisation = Column(JSONB(astext_type=Text()))
    uac_metadata = Column(JSONB(astext_type=Text()))
    caze_id = Column(ForeignKey('casev3.cases.id'), nullable=False)
    export_file_template_pack_code = Column(ForeignKey('casev3.export_file_template.pack_code'), nullable=False)

    caze = relationship('Case')
    export_file_template = relationship('ExportFileTemplate')


class JobRow(Base):
    __tablename__ = 'job_row'
    __table_args__ = (
        CheckConstraint("(job_row_status)::text = ANY ((ARRAY['STAGED'::character varying, 'VALIDATED_OK'::character varying, 'VALIDATED_ERROR'::character varying, 'PROCESSED'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    job_row_status = Column(String(255), nullable=False)
    original_row_data = Column(LargeBinary, nullable=False)
    original_row_line_number = Column(Integer, nullable=False)
    row_data = Column(JSONB(astext_type=Text()))
    validation_error_descriptions = Column(LargeBinary)
    job_id = Column(ForeignKey('casev3.job.id'), nullable=False)

    job = relationship('Job')


class UacQidLink(Base):
    __tablename__ = 'uac_qid_link'
    __table_args__ = {'schema': 'casev3'}

    id = Column(UUID, primary_key=True)
    active = Column(Boolean, nullable=False, server_default=text("true"))
    collection_instrument_url = Column(String(255), nullable=False)
    created_at = Column(DateTime(True))
    eq_launched = Column(Boolean, nullable=False, server_default=text("false"))
    last_updated_at = Column(DateTime(True))
    metadata_ = Column('metadata', JSONB(astext_type=Text()))
    qid = Column(String(255), nullable=False, index=True)
    receipt_received = Column(Boolean, nullable=False, server_default=text("false"))
    uac = Column(String(255), nullable=False)
    uac_hash = Column(String(255), nullable=False)
    caze_id = Column(ForeignKey('casev3.cases.id'), nullable=False)

    caze = relationship('Case')


class Event(Base):
    __tablename__ = 'event'
    __table_args__ = (
        CheckConstraint("(type)::text = ANY ((ARRAY['NEW_CASE'::character varying, 'RECEIPT'::character varying, 'REFUSAL'::character varying, 'ERASE_DATA'::character varying, 'EQ_LAUNCH'::character varying, 'INVALID_CASE'::character varying, 'UAC_AUTHENTICATION'::character varying, 'TELEPHONE_CAPTURE'::character varying, 'PRINT_FULFILMENT'::character varying, 'EXPORT_FILE'::character varying, 'DEACTIVATE_UAC'::character varying, 'UPDATE_SAMPLE'::character varying, 'UPDATE_SAMPLE_SENSITIVE'::character varying, 'SMS_FULFILMENT'::character varying, 'ACTION_RULE_SMS_REQUEST'::character varying, 'EMAIL_FULFILMENT'::character varying, 'ACTION_RULE_EMAIL_REQUEST'::character varying, 'ACTION_RULE_SMS_CONFIRMATION'::character varying, 'ACTION_RULE_EMAIL_CONFIRMATION'::character varying])::text[])"),
        {'schema': 'casev3'}
    )

    id = Column(UUID, primary_key=True)
    channel = Column(String(255), nullable=False)
    correlation_id = Column(UUID, nullable=False)
    created_by = Column(String(255))
    date_time = Column(DateTime(True), nullable=False)
    description = Column(String(255), nullable=False)
    message_id = Column(UUID, nullable=False)
    message_timestamp = Column(DateTime(True), nullable=False)
    payload = Column(JSONB(astext_type=Text()))
    processed_at = Column(DateTime(True), nullable=False, default=func.now())
    source = Column(String(255), nullable=False)
    type = Column(String(255), nullable=False)
    caze_id = Column(ForeignKey('casev3.cases.id'))
    uac_qid_link_id = Column(ForeignKey('casev3.uac_qid_link.id'))

    caze = relationship('Case')
    uac_qid_link = relationship('UacQidLink')
