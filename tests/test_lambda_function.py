import json
import sys
from pathlib import Path

import jwt

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import src.lambda_function as lambda_function  # noqa: E402
from src.repository import Customer  # noqa: E402


def test_returns_400_when_cpf_is_missing():
    response = lambda_function.handler({"body": json.dumps({})}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_cpf_is_invalid():
    response = lambda_function.handler({"body": json.dumps({"cpf": "123"})}, None)
    assert response["statusCode"] == 400


def test_returns_403_when_customer_is_inactive(monkeypatch):
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type(
            "Repo",
            (),
            {"find_by_cpf": lambda self, cpf: Customer(cpf=cpf, status="INACTIVE", active=False)},
        )(),
    )

    response = lambda_function.handler({"body": json.dumps({"cpf": "111.444.777-35"})}, None)
    assert response["statusCode"] == 403


def test_returns_token_for_active_customer(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "test-secret")
    monkeypatch.setenv("JWT_ISSUER", "autoservice-auth-test")
    monkeypatch.setenv("JWT_EXPIRES_SECONDS", "120")
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type(
            "Repo",
            (),
            {"find_by_cpf": lambda self, cpf: Customer(cpf=cpf, status="ACTIVE", active=True)},
        )(),
    )

    response = lambda_function.handler({"body": json.dumps({"cpf": "390.533.447-05"})}, None)

    assert response["statusCode"] == 200
    payload = json.loads(response["body"])
    token_payload = jwt.decode(
        payload["token"],
        "test-secret",
        algorithms=["HS256"],
        issuer="autoservice-auth-test",
    )
    assert token_payload["cpf"] == "39053344705"
    assert payload["token_type"] == "Bearer"
    assert payload["expires_in"] == 120
