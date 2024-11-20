from sqlalchemy.schema import CreateTable
from caseprocessor.entity.common_entity_model import Case
from sqlalchemy.dialects import postgresql

with open("caseprocessor/db/ddl/ddl.txt", "w") as file:
    file.write(str(CreateTable(Case.__table__).compile(dialect=postgresql.dialect())))
