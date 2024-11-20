from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, DateTime, func, String, MetaData
from sqlalchemy.orm import Session
from sqlalchemy.dialects.postgresql import JSONB
from ..entity_models.common_entity_model import Event

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)


# class Event(Base):
#     __tablename__ = "event"
#     id = Column(Integer, primary_key=True)
#     date_time = Column(DateTime(timezone=True))
#     processed_at = Column(DateTime(timezone=True), default=func.now())
#     description = Column(String)
#     type = Column(String)
#     channel = Column(String)
#     source = Column(String)
#     message_id = Column(Integer)
#     message_timestamp = Column(DateTime(timezone=True))
#     created_by = Column(String)
#     correlation_id = Column(String)
#     payload = Column(JSONB)
#     caze_id = Column(Integer)


def add_event(session: Session, event: Event):
    session.add(event)
