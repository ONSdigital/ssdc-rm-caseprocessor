from .case_repository import CasesTable
from .collection_exercise_repository import CollectionExerciseTable
from .db_utility import *


def test_database():
    to_string()


    session = create_session()

    for row in CasesTable.select_all(session):
        print(row.id)


    print("\n")

    for row in CollectionExerciseTable.select_all(session):
        print(row)
