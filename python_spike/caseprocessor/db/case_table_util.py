from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy import Column, Integer, BIGINT, DateTime, func, Boolean, Sequence
from sqlalchemy import MetaData
import uuid
from typing import Optional
#from ..entity.case import Case
from ..entity.common_entity_model import Case

# metadata_obj = MetaData(schema="casev3")
# Base = declarative_base(metadata=metadata_obj)
#
#
# class Case(Base):
#     __tablename__ = "cases"
#     id = Column(Integer, primary_key=True)
#     case_ref = Column(BIGINT)
#     collection_exercise_id = Column(Integer)
#     sample = Column(JSONB)
#     sample_sensitive = Column(JSONB)
#     secret_sequence_number = Column(Integer, Sequence('cases_secret_sequence_number_seq',
#                                                       metadata=Base.metadata))
#     created_at = Column(DateTime(timezone=True), server_default=func.now())
#     last_updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
#     invalid = Column(Boolean, default=False, nullable=False)


def find_by_id_with_update_lock(session: Session, case_id: uuid) -> Optional[str]:
    result = session.query(Case).filter_by(id=case_id).with_for_update(skip_locked=True).first()
    return result.id if result else None


def exists_by_id(session: Session, case_id: uuid) -> bool:
    return session.query(Case).filter_by(id=case_id).scalar() is not None


def add_and_flush(session: Session, caze: Case):
    session.add(caze)
    session.flush([caze])


def update_case_ref(session: Session, caze: Case):
    session.query(Case).filter_by(id=caze.id).update({Case.case_ref: caze.case_ref})


# Used for testing
def select_all(session: Session):
    return session.query(Case).all()