import json
import os
import sys
from datetime import datetime, timedelta, timezone
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import jwt
from cpf import is_valid_cpf
from repository import CustomerRepository


REPOSITORY = CustomerRepository()
CONTENT_TYPE_JSON = "application/json; charset=utf-8"

def normalize_cpf(cpf):
    return "".join(filter(str.isdigit, str(cpf or "")))


def handler(event, context):
    try:
        body = event.get("body") if isinstance(event, dict) and "body" in event else event

        if isinstance(body, str):
            try:
                body = json.loads(body or "{}")
            except json.JSONDecodeError:
                return {
                    "statusCode": 400,
                    "headers": {
                        "Access-Control-Allow-Origin": "*",
                        "Content-Type": CONTENT_TYPE_JSON,
                    },
                    "body": json.dumps({"message": "Corpo da requisição JSON inválido."}, ensure_ascii=False),
                }

        if not isinstance(body, dict):
            body = {}

        cpf = body.get("cpf")
        if not cpf:
            return {
                "statusCode": 400,
                "headers": {
                    "Access-Control-Allow-Origin": "*",
                    "Content-Type": CONTENT_TYPE_JSON,
                },
                "body": json.dumps({"message": "CPF é obrigatório."}, ensure_ascii=False),
            }

        cpf_limpo = normalize_cpf(cpf)
        if len(cpf_limpo) != 11:
            return {
                "statusCode": 400,
                "headers": {
                    "Access-Control-Allow-Origin": "*",
                    "Content-Type": CONTENT_TYPE_JSON,
                },
                "body": json.dumps({"message": "CPF inválido."}, ensure_ascii=False),
            }

        customer = REPOSITORY.find_by_cpf(cpf_limpo)
        if customer is None and not is_valid_cpf(cpf_limpo):
            return {
                "statusCode": 400,
                "headers": {
                    "Access-Control-Allow-Origin": "*",
                    "Content-Type": CONTENT_TYPE_JSON,
                },
                "body": json.dumps({"message": "CPF inválido."}, ensure_ascii=False),
            }

        if customer is None:
            return {
                "statusCode": 404,
                "headers": {
                    "Access-Control-Allow-Origin": "*",
                    "Content-Type": CONTENT_TYPE_JSON,
                },
                "body": json.dumps({"message": "CPF não cadastrado."}, ensure_ascii=False),
            }

        if not customer.active:
            return {
                "statusCode": 403,
                "headers": {
                    "Access-Control-Allow-Origin": "*",
                    "Content-Type": CONTENT_TYPE_JSON,
                },
                "body": json.dumps({"message": "Cliente inativo."}, ensure_ascii=False),
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
            "headers": {
                "Access-Control-Allow-Origin": "*",
                "Content-Type": CONTENT_TYPE_JSON,
            },
            "body": json.dumps({
                "token": token,
                "token_type": "Bearer",
                "expires_in": expires_seconds,
            }, ensure_ascii=False),
        }

    except Exception as e:
        print(f"Erro na execução da Lambda: {str(e)}")
        return {
            "statusCode": 500,
            "headers": {
                "Access-Control-Allow-Origin": "*",
                "Content-Type": CONTENT_TYPE_JSON,
            },
            "body": json.dumps({"message": "Erro interno do servidor."}, ensure_ascii=False),
        }
