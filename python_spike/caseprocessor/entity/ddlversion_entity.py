from sqlalchemy import DateTime, Integer, PrimaryKeyConstraint, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
import datetime

class Base(DeclarativeBase):
    pass


class Patches(Base):
    __tablename__ = 'patches'
    __table_args__ = (
        PrimaryKeyConstraint('patch_number', name='patches_pkey'),
        {'schema': 'ddl_version'}
    )

    patch_number: Mapped[int] = mapped_column(Integer, primary_key=True)
    applied_timestamp: Mapped[datetime.datetime] = mapped_column(DateTime(True))


class Version(Base):
    __tablename__ = 'version'
    __table_args__ = (
        PrimaryKeyConstraint('version_tag', name='version_pkey'),
        {'schema': 'ddl_version'}
    )

    version_tag: Mapped[str] = mapped_column(String(256), primary_key=True)
    updated_timestamp: Mapped[datetime.datetime] = mapped_column(DateTime(True))
