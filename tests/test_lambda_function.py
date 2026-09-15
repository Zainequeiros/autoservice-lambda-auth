import json
import sys
from pathlib import Path
from unittest.mock import MagicMock

import jwt
import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import cpf
import lambda_function
from repository import Customer, CustomerRepository


def test_only_digits_strips_mask():
    assert cpf.only_digits("390.533.447-05") == "39053344705"
    assert cpf.only_digits("") == ""
    assert cpf.only_digits(None) == ""


def test_is_valid_cpf_accepts_known_valid():
    assert cpf.is_valid_cpf("390.533.447-05") is True
    assert cpf.is_valid_cpf("11144477735") is True


def test_is_valid_cpf_rejects_invalid():
    assert cpf.is_valid_cpf("123") is False
    assert cpf.is_valid_cpf("11111111111") is False
    assert cpf.is_valid_cpf("39053344700") is False


def test_returns_400_when_cpf_is_missing():
    response = lambda_function.handler({"body": json.dumps({})}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_body_json_is_invalid():
    response = lambda_function.handler({"body": "{not-json"}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_cpf_is_invalid():
    response = lambda_function.handler({"body": json.dumps({"cpf": "123"})}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_cpf_checksum_invalid_and_not_in_db(monkeypatch):
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type("Repo", (), {"find_by_cpf": lambda self, cpf: None})(),
    )
    response = lambda_function.handler({"body": json.dumps({"cpf": "39053344700"})}, None)
    assert response["statusCode"] == 400


def test_returns_404_when_valid_cpf_not_registered(monkeypatch):
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type("Repo", (), {"find_by_cpf": lambda self, cpf: None})(),
    )
    response = lambda_function.handler({"body": json.dumps({"cpf": "39053344705"})}, None)
    assert response["statusCode"] == 404


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


def test_accepts_existing_customer_even_when_cpf_checksum_is_invalid(monkeypatch):
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type(
            "Repo",
            (),
            {"find_by_cpf": lambda self, cpf: Customer(cpf=cpf, status="ACTIVE", active=True)},
        )(),
    )

    response = lambda_function.handler({"body": json.dumps({"cpf": "12345678901"})}, None)

    assert response["statusCode"] == 200


def test_returns_token_for_active_customer(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "test-secret-at-least-32-bytes-long!!")
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
        "test-secret-at-least-32-bytes-long!!",
        algorithms=["HS256"],
        issuer="autoservice-auth-test",
    )
    assert token_payload["cpf"] == "39053344705"
    assert token_payload["sub"] == "39053344705"
    assert payload["token_type"] == "Bearer"
    assert payload["expires_in"] == 120


def test_returns_500_when_repository_raises(monkeypatch):
    def boom(self, cpf):
        raise RuntimeError("db down")

    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type("Repo", (), {"find_by_cpf": boom})(),
    )
    response = lambda_function.handler({"body": json.dumps({"cpf": "39053344705"})}, None)
    assert response["statusCode"] == 500


def test_handler_accepts_dict_body_without_envelope(monkeypatch):
    monkeypatch.setattr(
        lambda_function,
        "REPOSITORY",
        type(
            "Repo",
            (),
            {"find_by_cpf": lambda self, cpf: Customer(cpf=cpf, status="ACTIVE", active=True)},
        )(),
    )
    response = lambda_function.handler({"cpf": "39053344705"}, None)
    assert response["statusCode"] == 200


def _configured_repo(monkeypatch) -> CustomerRepository:
    monkeypatch.setenv("DB_HOST", "localhost")
    monkeypatch.setenv("DB_NAME", "autoservice")
    monkeypatch.setenv("DB_USER", "postgres")
    monkeypatch.setenv("DB_PASSWORD", "postgres")
    monkeypatch.setenv("DB_PORT", "5432")
    return CustomerRepository()


def test_repository_requires_env_vars(monkeypatch):
    for key in ("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD"):
        monkeypatch.delenv(key, raising=False)
    repo = CustomerRepository()
    with pytest.raises(RuntimeError, match="Variáveis de ambiente"):
        repo.find_by_cpf("39053344705")


def test_repository_returns_none_when_not_found(monkeypatch):
    repo = _configured_repo(monkeypatch)
    cursor = MagicMock()
    cursor.fetchone.return_value = None
    conn = MagicMock()
    conn.cursor.return_value.__enter__.return_value = cursor
    monkeypatch.setattr(repo, "get_connection", lambda: conn)

    assert repo.find_by_cpf("39053344705") is None
    conn.close.assert_called_once()


def test_repository_returns_customer_when_found(monkeypatch):
    repo = _configured_repo(monkeypatch)
    cursor = MagicMock()
    cursor.fetchone.return_value = ("39053344705",)
    conn = MagicMock()
    conn.cursor.return_value.__enter__.return_value = cursor
    monkeypatch.setattr(repo, "get_connection", lambda: conn)

    customer = repo.find_by_cpf("39053344705")
    assert customer == Customer(cpf="39053344705", status="ACTIVE", active=True)


def test_repository_reraises_after_query_error(monkeypatch):
    repo = _configured_repo(monkeypatch)
    cursor = MagicMock()
    cursor.execute.side_effect = [None, RuntimeError("query failed")]
    conn = MagicMock()
    conn.cursor.return_value.__enter__.return_value = cursor
    monkeypatch.setattr(repo, "get_connection", lambda: conn)

    with pytest.raises(RuntimeError, match="query failed"):
        repo.find_by_cpf("39053344705")
    conn.rollback.assert_called()
    conn.close.assert_called_once()
