from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session
from sqlalchemy import Column, Integer, String
from sqlalchemy import MetaData
import uuid
from typing import Optional
from caseprocessor.entity.case import Case

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)

class CasesTable(Base):
    __tablename__ = "cases"
    id = Column(Integer, primary_key=True)

    @classmethod
    def find_by_id_with_update_lock(cls, session: Session, case_id: uuid) -> Optional[str]:
        result = session.query(cls).filter_by(id=case_id).with_for_update(skip_locked=True).first()
        return result.id if result else None

    @classmethod
    def exists_by_id(cls, session: Session, case_id: uuid):
        return session.query(cls).filter_by(id=case_id).scalar() is not None

    @classmethod
    def save_and_flush(cls, session: Session, caze: Case):
        session.add(caze)

    @classmethod
    def select_all(cls, session: Session):
        return session.query(cls).all()
