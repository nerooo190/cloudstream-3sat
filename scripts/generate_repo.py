#!/usr/bin/env python3
"""Generate the Cloudstream repository manifest for this GitHub repository."""
import argparse
import json
import os
from pathlib import Path
import re


def manifest(repository: str) -> dict:
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Repository muss OWNER/NAME entsprechen.")
    if repository.startswith("DEIN-NAME/"):
        raise ValueError("Bitte den tatsächlichen GitHub-Namen verwenden.")
    return {
        "name": "3sat Mediathek",
        "description": "Inoffizielles Cloudstream-Plugin für die öffentlich zugängliche 3sat-Mediathek.",
        "manifestVersion": 1,
        "pluginLists": [f"https://raw.githubusercontent.com/{repository}/builds/plugins.json"],
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("repository", nargs="?", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--output", type=Path, default=Path("repo.json"))
    args = parser.parse_args()
    if not args.repository:
        parser.error("GitHub-Repository OWNER/NAME angeben oder GITHUB_REPOSITORY setzen.")
    try:
        result = manifest(args.repository)
    except ValueError as error:
        parser.error(str(error))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Repository-Manifest: {args.output}")
