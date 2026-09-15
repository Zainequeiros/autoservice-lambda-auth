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
        conn_kwargs["password"] = self.password
        return psycopg2.connect(**conn_kwargs)

    def find_by_cpf(self, cpf: str) -> Optional[Customer]:
        if not all([self.host, self.database, self.user, self.password]):
            raise RuntimeError(
                "Variáveis de ambiente do banco não configuradas: DB_HOST, DB_NAME, DB_USER, DB_PASSWORD"
            )

        conn = self.get_connection()
        try:
            with conn.cursor() as cursor:
                cursor.execute("SET search_path TO cadastro, public;")

                query = """
                    SELECT pf.cpf
                    FROM cadastro.pessoa_fisica pf
                    JOIN cadastro.pessoa p ON p.id = pf.id
                    JOIN cadastro.cliente c ON c.pessoa_id = p.id
                    WHERE pf.cpf = %s
                    LIMIT 1;
                """
                cursor.execute(query, (cpf,))
                record = cursor.fetchone()

                if record is None:
                    return None

                cpf_value = record[0]
                return Customer(cpf=cpf_value, status="ACTIVE", active=True)

        except Exception as exc:
            try:
                conn.rollback()
            except Exception:
                pass
            raise exc

        finally:
            try:
                conn.rollback()
            except Exception:
                pass
            conn.close()