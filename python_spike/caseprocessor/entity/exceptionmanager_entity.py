from typing import Optional

from sqlalchemy import Boolean, DateTime, LargeBinary, PrimaryKeyConstraint, String, Uuid, text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
import datetime
import uuid

class Base(DeclarativeBase):
    pass


class AutoQuarantineRule(Base):
    __tablename__ = 'auto_quarantine_rule'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='auto_quarantine_rule_pkey'),
        {'schema': 'exceptionmanager'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    quarantine: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    suppress_logging: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    throw_away: Mapped[bool] = mapped_column(Boolean, server_default=text('false'))
    expression: Mapped[Optional[str]] = mapped_column(String(255))
    rule_expiry_date_time: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))


class QuarantinedMessage(Base):
    __tablename__ = 'quarantined_message'
    __table_args__ = (
        PrimaryKeyConstraint('id', name='quarantined_message_pkey'),
        {'schema': 'exceptionmanager'}
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    content_type: Mapped[Optional[str]] = mapped_column(String(255))
    error_reports: Mapped[Optional[dict]] = mapped_column(JSONB)
    headers: Mapped[Optional[dict]] = mapped_column(JSONB)
    message_hash: Mapped[Optional[str]] = mapped_column(String(255))
    message_payload: Mapped[Optional[bytes]] = mapped_column(LargeBinary)
    routing_key: Mapped[Optional[str]] = mapped_column(String(255))
    service: Mapped[Optional[str]] = mapped_column(String(255))
    skipped_timestamp: Mapped[Optional[datetime.datetime]] = mapped_column(DateTime(True))
    skipping_user: Mapped[Optional[str]] = mapped_column(String(255))
    subscription: Mapped[Optional[str]] = mapped_column(String(255))
