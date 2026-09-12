import json
import os
from datetime import datetime, timedelta, timezone

import jwt

from cpf import is_valid_cpf
from repository import CustomerRepository


REPOSITORY = CustomerRepository()


def normalize_cpf(cpf):
    return "".join(filter(str.isdigit, str(cpf or "")))


def handler(event, context):
    try:
        body = event.get("body", "{}") if isinstance(event, dict) else "{}"
        if isinstance(body, str):
            body = json.loads(body or "{}")

        if not isinstance(body, dict):
            body = {}

        cpf = body.get("cpf")
        if not cpf:
            return {
                "statusCode": 400,
                "headers": {"Access-Control-Allow-Origin": "*"},
                "body": json.dumps({"message": "CPF é obrigatório."}),
            }

        cpf_limpo = normalize_cpf(cpf)
        if not is_valid_cpf(cpf_limpo):
            return {
                "statusCode": 400,
                "headers": {"Access-Control-Allow-Origin": "*"},
                "body": json.dumps({"message": "CPF invalido."}),
            }

        customer = REPOSITORY.find_by_cpf(cpf_limpo)
        if customer is None:
            return {
                "statusCode": 404,
                "headers": {"Access-Control-Allow-Origin": "*"},
                "body": json.dumps({"message": "CPF não cadastrado."}),
            }

        if not customer.active:
            return {
                "statusCode": 403,
                "headers": {"Access-Control-Allow-Origin": "*"},
                "body": json.dumps({"message": "Cliente inativo."}),
            }

        secret = os.environ.get("JWT_SECRET", "change-me")
        issuer = os.environ.get("JWT_ISSUER", "autoservice-auth")
        expires_seconds = int(os.environ.get("JWT_EXPIRES_SECONDS", "3600"))

        token_payload = {
            "cpf": cpf_limpo,
            "status": customer.status,
            "iss": issuer,
            "exp": datetime.now(timezone.utc) + timedelta(seconds=expires_seconds),
        }
        token = jwt.encode(token_payload, secret, algorithm="HS256")

        return {
            "statusCode": 200,
            "headers": {"Access-Control-Allow-Origin": "*"},
            "body": json.dumps({
                "token": token,
                "token_type": "Bearer",
                "expires_in": expires_seconds,
            }),
        }

    except Exception as e:
        print(f"Erro na execução da Lambda: {str(e)}")
        return {
            "statusCode": 500,
            "headers": {"Access-Control-Allow-Origin": "*"},
            "body": json.dumps({"message": "Erro interno do servidor."}),
        }
