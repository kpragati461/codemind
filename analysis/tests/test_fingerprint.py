from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def workspace(tmp_path, monkeypatch):
    monkeypatch.setenv("CODEMIND_WORKSPACE", str(tmp_path))
    return tmp_path


def make_repo(root: Path) -> Path:
    repo = root / "repos" / "p1"
    (repo / "src" / "main" / "java").mkdir(parents=True)
    (repo / "src" / "main" / "java" / "App.java").write_text("class App {}")
    (repo / "src" / "main" / "java" / "Svc.java").write_text("class Svc {}")
    (repo / "web").mkdir()
    (repo / "web" / "page.tsx").write_text("export default 1")
    (repo / "web" / "package.json").write_text("{}")
    (repo / "pom.xml").write_text("<project/>")
    (repo / "Dockerfile").write_text("FROM x")
    (repo / "node_modules" / "lib").mkdir(parents=True)
    (repo / "node_modules" / "lib" / "index.js").write_text("ignored")
    return repo


def test_fingerprint_detects_stack(workspace):
    repo = make_repo(workspace)
    res = TestClient(app).post("/v1/fingerprint", json={"projectId": "p1", "path": str(repo)})
    assert res.status_code == 200
    body = res.json()
    assert body["primaryLanguage"] == "Java"
    assert {lang["name"] for lang in body["languages"]} == {"Java", "TypeScript"}  # node_modules skipped
    assert body["buildSystems"] == ["Maven"]
    assert body["packageManagers"] == ["Maven", "npm"]
    assert body["containerized"] is True
    assert "web/package.json" in body["manifests"]


def test_rejects_path_outside_workspace(workspace, tmp_path_factory):
    outside = tmp_path_factory.mktemp("elsewhere")
    res = TestClient(app).post("/v1/fingerprint", json={"projectId": "p1", "path": str(outside)})
    assert res.status_code == 400


def test_missing_repo_is_404(workspace):
    res = TestClient(app).post(
        "/v1/fingerprint", json={"projectId": "p1", "path": str(workspace / "repos" / "nope")}
    )
    assert res.status_code == 404
