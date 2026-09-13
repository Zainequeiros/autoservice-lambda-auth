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
            candidates = ["cliente", "clientes", "public.cliente", "public.clientes"]
            last_error = None

            for table_name in candidates:
                try:
                    with conn.cursor() as cursor:
                        cursor.execute(
                            f"SELECT cpf, status, active FROM {table_name} WHERE cpf = %s LIMIT 1;",
                            (cpf,),
                        )
                        record = cursor.fetchone()
                    if record is None:
                        return None

                    cpf_value, status, active = record
                    return Customer(cpf=cpf_value, status=status, active=bool(active))
                except Exception as exc:  # pragma: no cover - dependente do schema do BD
                    last_error = exc
                    if "does not exist" not in str(exc) and "doesn't exist" not in str(exc):
                        raise

            if last_error is not None:
                raise last_error

            return None
        finally:
            conn.close()
