from dataclasses import dataclass
from typing import Dict, Optional


@dataclass(frozen=True)
class Customer:
    cpf: str
    status: str
    active: bool


class CustomerRepository:
    def __init__(self) -> None:
        # Bootstrap local para o commit inicial.
        self._customers: Dict[str, Customer] = {
            "39053344705": Customer(cpf="39053344705", status="ACTIVE", active=True),
            "11144477735": Customer(cpf="11144477735", status="INACTIVE", active=False),
        }

    def find_by_cpf(self, cpf: str) -> Optional[Customer]:
        return self._customers.get(cpf)
