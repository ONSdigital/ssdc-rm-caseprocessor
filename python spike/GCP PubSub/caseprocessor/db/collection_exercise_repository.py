from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, relationship, Mapped
from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy import MetaData
from sqlalchemy.dialects.postgresql import JSONB
from caseprocessor.entity.collection_exercise import CollectionExercise
from caseprocessor.entity.survey import Survey
from caseprocessor.validation.column_validator import ColumnValidator
from dataclass_wizard import fromdict
import uuid

from typing import Optional

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)


class CollectionExerciseTable(Base):
    __tablename__ = "collection_exercise"
    id = Column(Integer, primary_key=True)
    survey_id = Column(Integer, ForeignKey("survey.id"))
    survey: Mapped["SurveyTable"] = relationship()

    @classmethod
    def select_all(cls, session: Session):
        return session.query(cls.id, SurveyTable.id).join(cls.survey).all()

    @classmethod
    def find_by_id(cls, session: Session, collection_exercise_id: uuid) -> CollectionExercise:
        result = (session.query(cls.id, SurveyTable.id, SurveyTable.sample_validation_rules)
                  .filter_by(id=collection_exercise_id)
                  .join(cls.survey)
                  .first())

        # TODO fix this
        # The issue is that it takes in a java class for the validation rules
        # Those rules will have to be added, there is an 'abstract' (Python equivalent) class called rules

        # result = session.query(cls.id, cls.survey).filter_by(id=collection_exercise_id).first()
        sample_validation_rules = [
            fromdict(ColumnValidator, sample_validation_rule) for sample_validation_rule in result[2]]

        survey = Survey(id=result[1], sample_validation_rules=sample_validation_rules)
        return CollectionExercise(id=result[0], survey=survey)


class SurveyTable(Base):
    __tablename__ = "survey"
    id = Column(Integer, primary_key=True)
    sample_validation_rules = Column(JSONB)
