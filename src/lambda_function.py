import json
import os
import time
from typing import Any, Dict

import jwt

from cpf import is_valid_cpf, only_digits
from repository import CustomerRepository


def _response(status_code: int, payload: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(payload),
    }


def _generate_token(cpf: str, status: str) -> str:
    secret = os.getenv("JWT_SECRET", "change-me")
    issuer = os.getenv("JWT_ISSUER", "autoservice-auth")
    expires_in = int(os.getenv("JWT_EXPIRES_SECONDS", "3600"))
    now = int(time.time())
    payload = {
        "sub": cpf,
        "cpf": cpf,
        "customer_status": status,
        "iss": issuer,
        "iat": now,
        "exp": now + expires_in,
    }
    return jwt.encode(payload, secret, algorithm="HS256")


def handler(event: Dict[str, Any], _context: Any) -> Dict[str, Any]:
    body_raw = event.get("body")
    body = json.loads(body_raw) if isinstance(body_raw, str) else (body_raw or {})
    cpf_input = body.get("cpf")

    if not cpf_input:
        return _response(400, {"message": "CPF e obrigatorio."})

    cpf = only_digits(cpf_input)
    if not is_valid_cpf(cpf):
        return _response(400, {"message": "CPF invalido."})

    repository = CustomerRepository()
    customer = repository.find_by_cpf(cpf)
    if customer is None:
        return _response(404, {"message": "Cliente nao encontrado."})

    if not customer.active:
        return _response(403, {"message": "Cliente inativo."})

    token = _generate_token(customer.cpf, customer.status)
    return _response(200, {"token": token, "token_type": "Bearer", "expires_in": int(os.getenv("JWT_EXPIRES_SECONDS", "3600"))})
