from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, registry
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy import Column, Integer, String, BIGINT, Sequence
from sqlalchemy import MetaData
import uuid
from typing import Optional
from caseprocessor.entity.case import Case

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)

class CasesTable(Base):
    __tablename__ = "cases"
    id = Column(Integer, primary_key=True)
    case_ref = Column(BIGINT)
    collection_exercise_id = Column(Integer)
    sample = Column(JSONB)
    sample_sensitive = Column(JSONB)
    secret_sequence_number = Column(Integer, autoincrement=True)  # Not 100% sure if this is what we want


    @classmethod
    def find_by_id_with_update_lock(cls, session: Session, case_id: uuid) -> Optional[str]:
        result = session.query(cls).filter_by(id=case_id).with_for_update(skip_locked=True).first()
        return result.id if result else None

    @classmethod
    def exists_by_id(cls, session: Session, case_id: uuid):
        return session.query(cls).filter_by(id=case_id).scalar() is not None

    @classmethod
    def save_and_flush(cls, session: Session, caze: Case):
        #registry.map_imperatively(Case, CasesTable)
        # TODO: make this more efficient?
        caze = CasesTable(id=caze.id,
                          case_ref=caze.case_ref,
                          collection_exercise_id=caze.collection_exercise_id,
                          sample=caze.sample,
                          sample_sensitive=caze.sample_sensitive,
                          )
        session.add(caze)

    @classmethod
    def select_all(cls, session: Session):
        return session.query(cls).all()

    # This doesn't currently work
    def add_secret_sequence_number(self, session: Session):
        session.add(self)
        print(self.secret_sequence_number)

