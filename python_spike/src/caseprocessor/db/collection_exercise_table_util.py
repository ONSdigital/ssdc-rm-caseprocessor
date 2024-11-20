from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, relationship, Mapped
from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy import MetaData
from sqlalchemy.dialects.postgresql import JSONB
from caseprocessor.entity.collection_exercise import CollectionExercise
from caseprocessor.entity.survey import Survey
from caseprocessor.validation.column_validator import ColumnValidator
from caseprocessor.validation.rules import create_rule
from caseprocessor.db.survey_table import SurveyTable
from ..entity.common_entity_model import CollectionExercise as CollectionExerciseTable
from ..entity.common_entity_model import Survey as SurveyTable

import uuid

metadata_obj = MetaData(schema="casev3")
Base = declarative_base(metadata=metadata_obj)


# class CollectionExerciseTable(Base):
#     __tablename__ = "collection_exercise"
#     id = Column(Integer, primary_key=True)
#     survey_id = Column(Integer, ForeignKey("survey.id"))
#     survey = relationship("SurveyTable")


def find_collex_by_id(session: Session, collection_exercise_id: uuid) -> CollectionExercise:
    result = (session.query(CollectionExerciseTable.id, SurveyTable.id, SurveyTable.sample_validation_rules)
              .filter_by(id=collection_exercise_id)
              .join(CollectionExerciseTable.survey)
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


# Used for testing
def select_all_collex(session: Session):
    return session.query(CollectionExerciseTable.id, SurveyTable.id).join(CollectionExerciseTable.survey).all()


# I can't seem to get this to work when it's in a different file
# class SurveyTable(Base):
#     __tablename__ = "survey"
#     id = Column(Integer, primary_key=True)
#     sample_validation_rules = Column(JSONB)
