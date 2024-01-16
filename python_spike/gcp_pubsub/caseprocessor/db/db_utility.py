import sqlalchemy
from sqlalchemy.orm import Session
from sqlalchemy import inspect

DB_USERNAME = 'appuser'
DB_PASSWORD = 'postgres'
DB_HOST = 'localhost'
DB_PORT = '6432'
DB_NAME = 'rm'

ENGINE = sqlalchemy.create_engine(
    f"postgresql+psycopg2://{DB_USERNAME}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}")
# Bellow might be a better way of doing it but can't get it to work

CONN = ENGINE.connect()


def create_session() -> Session:
    return Session(ENGINE)


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


'''
    DB_URL = URL.create(
        "postgresql + psycopg2",
        username=DB_USERNAME,
        password=DB_PASSWORD,
        host=DB_HOST,
        database=DB_NAME,
    )
    ENGINE = sqlalchemy.create_engine(DB_URL)
    '''
