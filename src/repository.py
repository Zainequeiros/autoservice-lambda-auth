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
        return psycopg2.connect(
            host=self.host,
            database=self.database,
            user=self.user,
            password=self.password,
            port=self.port,
            connect_timeout=5,
        )

    def find_by_cpf(self, cpf: str) -> Optional[Customer]:
        if not all([self.host, self.database, self.user, self.password]):
            raise RuntimeError(
                "Variáveis de ambiente do banco não configuradas: DB_HOST, DB_NAME, DB_USER, DB_PASSWORD"
            )

        conn = self.get_connection()
        try:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT cpf, status, active FROM clientes WHERE cpf = %s LIMIT 1;",
                    (cpf,),
                )
                record = cursor.fetchone()

            if record is None:
                return None

            cpf_value, status, active = record
            return Customer(cpf=cpf_value, status=status, active=bool(active))
        finally:
            conn.close()
