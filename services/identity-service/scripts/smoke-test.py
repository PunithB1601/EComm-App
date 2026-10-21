#!/usr/bin/env python3
"""Exercise a running development instance. Creates one disposable customer account."""
import json
import secrets
import sys
import urllib.error
import urllib.request

base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081").rstrip("/")


def request(method, path, body=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=payload, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        status = response.status
        data = json.load(response)
    if status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {status}")
    return data


email = "smoke-" + secrets.token_hex(8) + "@example.test"
password = secrets.token_urlsafe(24)
registered = request("POST", "/api/v1/auth/register", {
    "fullName": "Smoke Test", "email": email, "password": password
}, expected=201)["data"]
assert registered["user"]["roles"] == ["ROLE_USER"]
logged_in = request("POST", "/api/v1/auth/login", {"email": email, "password": password})["data"]
assert request("GET", "/api/v1/users/me", token=logged_in["accessToken"])["data"]["email"] == email
request("GET", "/api/v1/users", token=logged_in["accessToken"], expected=403)
request("GET", "/api/v1/users/me", expected=401)
rotated = request("POST", "/api/v1/auth/refresh", {"refreshToken": logged_in["refreshToken"]})["data"]
request("POST", "/api/v1/auth/refresh", {"refreshToken": logged_in["refreshToken"]}, expected=401)
request("POST", "/api/v1/auth/refresh", {"refreshToken": rotated["refreshToken"]}, expected=401)
request("POST", "/api/v1/auth/logout", {"refreshToken": registered["refreshToken"]})
request("POST", "/api/v1/auth/refresh", {"refreshToken": registered["refreshToken"]}, expected=401)
assert request("GET", "/v3/api-docs")["openapi"].startswith("3.")
keys = request("GET", "/.well-known/jwks.json")["keys"]
assert keys and "d" not in keys[0] and keys[0]["kty"] == "RSA"
print("PASS: registration, login, current user, role restrictions, authentication, rotation, replay, logout, OpenAPI and public keys")
