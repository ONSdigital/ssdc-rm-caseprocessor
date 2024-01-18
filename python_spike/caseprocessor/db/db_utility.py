import time

import sqlalchemy
from sqlalchemy.orm import Session
from sqlalchemy import inspect
from config import DatabaseConfig

# waiting 5 seconds before connecting
# This is just temporary for the spike, without it sometimes it fails as the postgres database docker isn't ready
time.sleep(5)

ENGINE = sqlalchemy.create_engine(
    f"postgresql+psycopg2://{DatabaseConfig.DB_USERNAME}:{DatabaseConfig.DB_PASSWORD}@{DatabaseConfig.DB_HOST}:{DatabaseConfig.DB_PORT}/{DatabaseConfig.DB_NAME}")
# Bellow might be a better way of doing it but can't get it to work
# DB_URL = URL.create(
#     "postgresql + psycopg2",
#     username=DB_USERNAME,
#     password=DB_PASSWORD,
#     host=DB_HOST,
#     database=DB_NAME,
# )
# ENGINE = sqlalchemy.create_engine(DB_URL)

CONN = ENGINE.connect()


def create_session() -> Session:
    return Session(ENGINE)


# Used for testing
# Returns the schema and tables of the database
def to_string():
    inspector = inspect(ENGINE)
    schemas = inspector.get_schema_names()

    for schema in schemas:
        print("schema: %s" % schema)
        for table_name in inspector.get_table_names(schema=schema):
            print("table %s" % table_name)
            for column in inspector.get_columns(table_name, schema=schema):
                print("Column: %s" % column)

    print("\n sequence:")

    sequences = inspector.get_sequence_names()
    for sequence in sequences:
        print(sequence)

