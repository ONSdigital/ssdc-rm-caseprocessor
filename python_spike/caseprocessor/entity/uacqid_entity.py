from typing import Optional

from sqlalchemy import Integer, PrimaryKeyConstraint, String, text, Sequence
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

class Base(DeclarativeBase):
    pass


class UacQid(Base):
    __tablename__ = 'uac_qid'
    __table_args__ = (
        PrimaryKeyConstraint('uac', name='uac_qid_pkey'),
        {'schema': 'uacqid'}
    )

    uac: Mapped[str] = mapped_column(String(255), primary_key=True)
    unique_number: Mapped[int] = mapped_column(Integer, autoincrement=True)
    qid: Mapped[Optional[str]] = mapped_column(String(255))
