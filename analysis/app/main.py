"""CodeMind analysis service.

Week 1 scope: health check and a basic repository fingerprint (languages, manifests,
build systems, package managers). Week 2 extends fingerprinting and adds AST parsing.
"""

from __future__ import annotations

import os
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from app.fingerprint import fingerprint_repository

app = FastAPI(title="CodeMind Analysis", version="0.1.0")


def workspace_root() -> Path:
    default = Path.home() / ".codemind"
    return Path(os.environ.get("CODEMIND_WORKSPACE", default)).expanduser().resolve()


class FingerprintRequest(BaseModel):
    projectId: str
    path: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "codemind-analysis"}


@app.post("/v1/fingerprint")
def fingerprint(request: FingerprintRequest) -> dict:
    root = Path(request.path).resolve()
    # Only analyse repositories the API cloned into the workspace.
    if not root.is_relative_to(workspace_root()):
        raise HTTPException(status_code=400, detail="path must be inside the CodeMind workspace")
    if not root.is_dir():
        raise HTTPException(status_code=404, detail="repository path does not exist")
    return {"projectId": request.projectId, **fingerprint_repository(root)}
