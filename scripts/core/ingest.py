import argparse
from pathlib import Path

import knowledge


def main() -> None:
    parser = argparse.ArgumentParser(description="Index local knowledge documents")
    parser.add_argument("path", help="file or directory inside data/documents")
    args = parser.parse_args()
    path = Path(args.path).resolve()
    root = knowledge.DOCUMENTS_DIR.resolve()
    if not path.is_relative_to(root):
        raise PermissionError(f"path must be inside {root}")
    files = [path] if path.is_file() else [
        item for item in path.rglob("*") if item.suffix.lower() in {".md", ".txt", ".pdf", ".docx"}
    ]
    for file_path in files:
        print(knowledge.index_document(str(file_path)))


if __name__ == "__main__":
    main()
