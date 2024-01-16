from .case_repository import CasesTable
from .collection_exercise_repository import CollectionExerciseTable
from .db_utility import *


def test_database():
    to_string()


    session = create_session()

    print("here")
    for row in CasesTable.select_all(session):
        print(row.secret_sequence_number)


    print("\n")

    for row in CollectionExerciseTable.select_all(session):
        print(row)
