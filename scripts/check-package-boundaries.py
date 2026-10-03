#!/usr/bin/env python3
"""Fail if a UI source file breaks a package boundary.

Two rules keep the UI layers pointing one way:

reverse-import   A file under ui/theme or ui/component must not import
                 me.bmax.apatch.ui.screen.*, so the token and component
                 layers stay usable without pulling in a screen.

hardcoded-color  ui/theme is the only layer allowed a colour literal (the
                 palette data and fallbacks live there). Everywhere else a
                 colour must come from the active scheme, apart from the
                 named palette sources pinned in the allowlist.

Exceptions live in a shrink-only allowlist so new code cannot opt out.
"""
import argparse
import os
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
UI_PREFIX = "app/src/main/java/me/bmax/apatch/ui"
THEME_DIR = Path(UI_PREFIX) / "theme"
COMPONENT_DIR = Path(UI_PREFIX) / "component"

REVERSE_IMPORT = re.compile(r"^\s*import\s+me\.bmax\.apatch\.ui\.screen(?:\.|$)", re.M)
HARDCODED_COLOR = re.compile(r"\bColor\(0x[0-9A-Fa-f]+\)")

SKIP_DIRS = {
    "build",
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    ".cxx",
    "node_modules",
    "__pycache__",
    ".research",
    "KernelPatch",
    "target",
}


def load_allowlist(path):
    """Read `rule path` lines; blanks and # comments are ignored."""
    entries = set()
    if path and path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(None, 1)
            if len(parts) == 2:
                entries.add((parts[0], parts[1].replace("\\", "/")))
    return entries


def iter_kt_files(ui_dir):
    for dirpath, dirnames, filenames in os.walk(ui_dir):
        dirnames[:] = sorted(name for name in dirnames if name not in SKIP_DIRS)
        for name in sorted(filenames):
            if Path(name).suffix == ".kt":
                yield Path(dirpath) / name


def find_violations(root, allowlist=None):
    """Return a sorted list of (rule, relative_path, detail)."""
    allowlist = allowlist or set()
    ui_dir = root / UI_PREFIX
    theme_dir = root / THEME_DIR
    component_dir = root / COMPONENT_DIR
    violations = []
    for path in iter_kt_files(ui_dir):
        relative = path.relative_to(root).as_posix()
        text = path.read_text(encoding="utf-8", errors="replace")
        in_theme = theme_dir in path.parents
        in_component = component_dir in path.parents
        if (
            (in_theme or in_component)
            and REVERSE_IMPORT.search(text)
            and ("reverse-import", relative) not in allowlist
        ):
            violations.append(("reverse-import", relative, "imports ui.screen.*"))
        if (
            not in_theme
            and HARDCODED_COLOR.search(text)
            and ("hardcoded-color", relative) not in allowlist
        ):
            violations.append(("hardcoded-color", relative, "hardcoded Color(0x...)"))
    return sorted(violations)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=ROOT / "scripts" / "package-boundary-allowlist.txt",
    )
    args = parser.parse_args(argv)

    allowlist = load_allowlist(args.allowlist)
    violations = find_violations(args.root, allowlist)
    if violations:
        for rule, relative, detail in violations:
            print(f"{relative}: {rule} ({detail})")
        print(f"\n{len(violations)} package boundary violation(s).")
        return 1
    print("OK: no package boundary violations.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
