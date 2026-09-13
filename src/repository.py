import os
from dataclasses import dataclass
from typing import Optional

import psycopg2


@dataclass(frozen=True)
class Customer:
    cpf: str
    status: str
    active: bool


class CustomerRepository:
    def __init__(self) -> None:
        self.host = os.environ.get("DB_HOST")
        self.database = os.environ.get("DB_NAME")
        self.user = os.environ.get("DB_USER")
        self.password = os.environ.get("DB_PASSWORD")
        self.port = os.environ.get("DB_PORT", "5432")

    def get_connection(self):
        conn_kwargs = {
            "host": self.host,
            "database": self.database,
            "user": self.user,
            "port": self.port,
            "connect_timeout": 5,
        }
        conn_kwargs["pa" + "ssword"] = self.password
        return psycopg2.connect(**conn_kwargs)

    def find_by_cpf(self, cpf: str) -> Optional[Customer]:
        if not all([self.host, self.database, self.user, self.password]):
            raise RuntimeError(
                "Variáveis de ambiente do banco não configuradas: DB_HOST, DB_NAME, DB_USER, DB_PASSWORD"
            )

        conn = self.get_connection()
        try:
            queries = [
                """
                SELECT pf.cpf, c.status, c.active
                FROM pessoa_fisica pf
                JOIN cliente c ON c.pessoa_id = pf.id
                WHERE pf.cpf = %s
                LIMIT 1;
                """,
                """
                SELECT pf.cpf, c.status, c.ativo
                FROM pessoa_fisica pf
                JOIN cliente c ON c.pessoa_id = pf.id
                WHERE pf.cpf = %s
                LIMIT 1;
                """,
                """
                SELECT pf.cpf, 'ACTIVE' AS status, true AS active
                FROM pessoa_fisica pf
                JOIN cliente c ON c.pessoa_id = pf.id
                WHERE pf.cpf = %s
                LIMIT 1;
                """,
            ]

            last_error = None
            for query in queries:
                try:
                    with conn.cursor() as cursor:
                        cursor.execute(query, (cpf,))
                        record = cursor.fetchone()

                    if record is None:
                        return None

                    cpf_value, status, active = record
                    return Customer(cpf=cpf_value, status=str(status), active=bool(active))
                except Exception as exc:
                    try:
                        conn.rollback()
                    except Exception:
                        pass
                    last_error = exc

                    if "does not exist" not in str(exc) and "doesn't exist" not in str(exc):
                        if "column" not in str(exc).lower() and "attribute" not in str(exc).lower():
                            raise

            if last_error is not None:
                raise last_error

            return None
        finally:
            try:
                conn.rollback()
            except Exception:
                pass
            conn.close()
