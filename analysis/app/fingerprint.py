"""Basic repository fingerprinting: languages, manifests, build systems, package managers."""

from __future__ import annotations

import os
from collections import Counter
from pathlib import Path

SKIP_DIRS = {
    ".git", "node_modules", "target", "build", "dist", "out", ".next",
    ".venv", "venv", "__pycache__", ".gradle", ".idea", ".vscode",
}

LANGUAGES = {
    ".java": "Java",
    ".kt": "Kotlin",
    ".js": "JavaScript", ".jsx": "JavaScript", ".mjs": "JavaScript", ".cjs": "JavaScript",
    ".ts": "TypeScript", ".tsx": "TypeScript",
    ".py": "Python",
    ".go": "Go",
    ".rs": "Rust",
    ".cs": "C#",
    ".rb": "Ruby",
    ".php": "PHP",
    ".c": "C", ".h": "C",
    ".cpp": "C++", ".cc": "C++", ".hpp": "C++",
    ".sql": "SQL",
}

# manifest file name -> (build system, package manager)
MANIFESTS = {
    "pom.xml": ("Maven", "Maven"),
    "build.gradle": ("Gradle", "Gradle"),
    "build.gradle.kts": ("Gradle", "Gradle"),
    "package.json": (None, "npm"),
    "yarn.lock": (None, "Yarn"),
    "pnpm-lock.yaml": (None, "pnpm"),
    "requirements.txt": (None, "pip"),
    "pyproject.toml": (None, "pip"),
    "go.mod": ("Go modules", "Go modules"),
    "Cargo.toml": ("Cargo", "Cargo"),
    "Dockerfile": (None, None),
    "docker-compose.yml": (None, None),
    "docker-compose.yaml": (None, None),
}

MAX_FILES = 50_000


def fingerprint_repository(root: Path) -> dict:
    languages: Counter[str] = Counter()
    manifests: list[str] = []
    build_systems: set[str] = set()
    package_managers: set[str] = set()
    total_files = 0
    truncated = False

    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            total_files += 1
            if total_files > MAX_FILES:
                truncated = True
                break
            lang = LANGUAGES.get(Path(name).suffix.lower())
            if lang:
                languages[lang] += 1
            if name in MANIFESTS:
                rel = Path(dirpath, name).relative_to(root).as_posix()
                manifests.append(rel)
                build, pkg = MANIFESTS[name]
                if build:
                    build_systems.add(build)
                if pkg:
                    package_managers.add(pkg)
        if truncated:
            break

    source_files = sum(languages.values())
    return {
        "primaryLanguage": languages.most_common(1)[0][0] if languages else None,
        "languages": [
            {"name": lang, "files": count, "share": round(count / source_files, 3)}
            for lang, count in languages.most_common()
        ],
        "manifests": sorted(manifests),
        "buildSystems": sorted(build_systems),
        "packageManagers": sorted(package_managers),
        "containerized": any(Path(m).name.startswith(("Dockerfile", "docker-compose")) for m in manifests),
        "totalFiles": min(total_files, MAX_FILES),
        "truncated": truncated,
    }
