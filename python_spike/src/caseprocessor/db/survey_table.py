from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer
from sqlalchemy import MetaData
from sqlalchemy.dialects.postgresql import JSONB

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)


class SurveyTable(Base):
    __tablename__ = "survey"
    id = Column(Integer, primary_key=True)
    sample_validation_rules = Column(JSONB)
