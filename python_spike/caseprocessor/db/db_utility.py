import time

import sqlalchemy
from sqlalchemy.orm import scoped_session, sessionmaker
from sqlalchemy import inspect
from config import DatabaseConfig


# Keeps trying to connect to the database until it can
# I know this isn't very nice - I also don't like it but...
# This is just temporary for the spike,
# without it sometimes the container crashes as the postgres database isn't ready
def wait_for_postgres_ready():
    tries = 0
    while tries < 10:
        try:
            ENGINE = sqlalchemy.create_engine(
                f"postgresql+psycopg2://{DatabaseConfig.DB_USERNAME}:{DatabaseConfig.DB_PASSWORD}@{DatabaseConfig.DB_HOST}:{DatabaseConfig.DB_PORT}/{DatabaseConfig.DB_NAME}")
            CONN = ENGINE.connect()
            CONN.close()
            return
        except:
            time.sleep(5)
            tries += 1


wait_for_postgres_ready()


ENGINE = sqlalchemy.create_engine(
            f"postgresql+psycopg2://{DatabaseConfig.DB_USERNAME}:{DatabaseConfig.DB_PASSWORD}@{DatabaseConfig.DB_HOST}:{DatabaseConfig.DB_PORT}/{DatabaseConfig.DB_NAME}")
CONN = ENGINE.connect()
# Bellow might be a better way of doing it but can't get it to work
# DB_URL = URL.create(
#     "postgresql + psycopg2",
#     username=DB_USERNAME,
#     password=DB_PASSWORD,
#     host=DB_HOST,
#     database=DB_NAME,
# )
# ENGINE = sqlalchemy.create_engine(DB_URL)


def create_session() -> Session:
    return Session(ENGINE)

# Allows us to create thread-local sessions, will eventually replace the above function
session_factory = sessionmaker(bind=ENGINE)
Session = scoped_session(session_factory)


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
