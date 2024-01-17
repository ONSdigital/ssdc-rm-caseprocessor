from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, relationship, Mapped
from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy import MetaData
from sqlalchemy.dialects.postgresql import JSONB
from caseprocessor.entity.collection_exercise import CollectionExercise
from caseprocessor.entity.survey import Survey
from caseprocessor.validation.column_validator import ColumnValidator
from caseprocessor.validation.rules import create_rule
import uuid

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

        sample_validation_rules = []

        if not result:
            return None

        for sample_validation_rule in result[2]:
            rules = []
            for rule in sample_validation_rule['rules']:
                rules.append(create_rule(rule['className']))
            column_validator = ColumnValidator(column_name=sample_validation_rule['columnName'],
                                               sensitive=sample_validation_rule['sensitive'],
                                               rules=rules)
            sample_validation_rules.append(column_validator)

        survey = Survey(id=result[1], sample_validation_rules=sample_validation_rules)
        return CollectionExercise(id=result[0], survey=survey)


class SurveyTable(Base):
    __tablename__ = "survey"
    id = Column(Integer, primary_key=True)
    sample_validation_rules = Column(JSONB)
