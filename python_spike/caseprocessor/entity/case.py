from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy import Column, Integer, BIGINT, DateTime, func, Boolean, Sequence
from sqlalchemy import MetaData


metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)


class Case(Base):
    __tablename__ = "cases"
    id = Column(Integer, primary_key=True)
    case_ref = Column(BIGINT)
    collection_exercise_id = Column(Integer)
    sample = Column(JSONB)
    sample_sensitive = Column(JSONB)
    secret_sequence_number = Column(Integer, Sequence('cases_secret_sequence_number_seq',
                                                      metadata=Base.metadata))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    last_updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    invalid = Column(Boolean, default=False, nullable=False)