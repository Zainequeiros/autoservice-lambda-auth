import json
import os
import sys
from pathlib import Path

import jwt

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))
from lambda_function import handler  # noqa: E402


def test_returns_400_when_cpf_is_missing():
    response = handler({"body": json.dumps({})}, None)
    assert response["statusCode"] == 400


def test_returns_400_when_cpf_is_invalid():
    response = handler({"body": json.dumps({"cpf": "123"})}, None)
    assert response["statusCode"] == 400


def test_returns_403_when_customer_is_inactive():
    response = handler({"body": json.dumps({"cpf": "111.444.777-35"})}, None)
    assert response["statusCode"] == 403


def test_returns_token_for_active_customer():
    os.environ["JWT_SECRET"] = "test-secret"
    os.environ["JWT_ISSUER"] = "autoservice-auth-test"
    os.environ["JWT_EXPIRES_SECONDS"] = "120"

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
