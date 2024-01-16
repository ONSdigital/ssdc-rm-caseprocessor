from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, relationship, Mapped
from sqlalchemy import Column, Integer
from sqlalchemy import MetaData


from typing import Optional

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)
#Base = declarative_base()

class SurveyTable(Base):
    __tablename__ = "survey"
    id = Column(Integer, primary_key=True)
    sample_validation_rules = Column()
    collection_exercise = relationship("CollectionExerciseTable", backref="collection_exercise.id")
