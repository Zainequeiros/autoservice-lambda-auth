import json
import sys
from pathlib import Path

import jwt

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))
from lambda_function import handler  # noqa: E402


def test_returns_400_when_cpf_is_missing(monkeypatch):
    monkeypatch.delenv("JWT_SECRET", raising=False)
    monkeypatch.delenv("JWT_ISSUER", raising=False)
    monkeypatch.delenv("JWT_EXPIRES_SECONDS", raising=False)

    response = handler({"body": json.dumps({})}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_cpf_is_invalid(monkeypatch):
    monkeypatch.delenv("JWT_SECRET", raising=False)
    monkeypatch.delenv("JWT_ISSUER", raising=False)
    monkeypatch.delenv("JWT_EXPIRES_SECONDS", raising=False)

    response = handler({"body": json.dumps({"cpf": "123"})}, None)
    assert response["statusCode"] == 400


def test_returns_403_when_customer_is_inactive(monkeypatch):
    monkeypatch.delenv("JWT_SECRET", raising=False)
    monkeypatch.delenv("JWT_ISSUER", raising=False)
    monkeypatch.delenv("JWT_EXPIRES_SECONDS", raising=False)

    response = handler({"body": json.dumps({"cpf": "111.444.777-35"})}, None)
    assert response["statusCode"] == 403


def test_returns_token_for_active_customer(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "test-secret")
    monkeypatch.setenv("JWT_ISSUER", "autoservice-auth-test")
    monkeypatch.setenv("JWT_EXPIRES_SECONDS", "120")

    response = handler({"body": json.dumps({"cpf": "390.533.447-05"})}, None)

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


def test_returns_400_when_body_is_missing(monkeypatch):
    monkeypatch.delenv("JWT_SECRET", raising=False)
    monkeypatch.delenv("JWT_ISSUER", raising=False)
    monkeypatch.delenv("JWT_EXPIRES_SECONDS", raising=False)

    response = handler({}, None)
    assert response["statusCode"] == 400


def test_returns_404_when_customer_does_not_exist(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "test-secret")
    monkeypatch.setenv("JWT_ISSUER", "autoservice-auth-test")
    monkeypatch.setenv("JWT_EXPIRES_SECONDS", "120")

    response = handler({"body": json.dumps({"cpf": "123.456.789-09"})}, None)
    assert response["statusCode"] == 404


def test_accepts_cpf_as_plain_digits(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "test-secret")
    monkeypatch.setenv("JWT_ISSUER", "autoservice-auth-test")
    monkeypatch.setenv("JWT_EXPIRES_SECONDS", "120")

    response = handler({"body": json.dumps({"cpf": "39053344705"})}, None)
    assert response["statusCode"] == 200
    payload = json.loads(response["body"])
    token = jwt.decode(
        payload["token"],
        "test-secret",
        algorithms=["HS256"],
        issuer="autoservice-auth-test",
    )
    assert token["cpf"] == "39053344705"
