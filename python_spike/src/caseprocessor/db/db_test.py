from .case_repository import *
from .collection_exercise_table_util import CollectionExerciseTable
from .db_utility import *


def test_database():
    to_string()


    session = create_session()

    print("here")
    for row in select_all(session):
        print(row.secret_sequence_number)


    print("\n")

    for row in CollectionExerciseTable.select_all(session):
        print(row)



    print("\n cases")

    for row in select_all(session):
        print(row)