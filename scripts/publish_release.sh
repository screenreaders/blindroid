#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <versionNumber> [releaseNotes]" >&2
  exit 1
fi

version="$1"
notes="${2-}"

python3 - <<PY
from pathlib import Path
import re
version = "$version"
path = Path("app/build.gradle.kts")
text = path.read_text()
text = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {version}", text)
text = re.sub(r"versionName\s*=\s*\"[^\"]*\"", f"versionName = \"{version}\"", text)
path.write_text(text)
PY

./gradlew :app:assembleRelease

apk_src="app/build/outputs/apk/release/app-release.apk"
apk_name="blindroid-${version}.apk"
cp -f "$apk_src" "$apk_name"

sha256=$(sha256sum "$apk_name" | awk '{print $1}')
size=$(stat -c "%s" "$apk_name")

python3 - <<PY
import json
from pathlib import Path
version = "$version"
notes = "$notes"
sha256 = "$sha256"
size = int("$size")
name = f"blindroid-{version}.apk"
url = f"https://raw.githubusercontent.com/screenreaders/blindroid/main/{name}"
path = Path("update.json")
data = {
  "tag_name": f"v{version}",
  "name": f"v{version}",
  "body": notes if notes else f"Release v{version}",
  "assets": [
    {
      "name": name,
      "browser_download_url": url,
      "sha256": sha256,
      "size": size,
    }
  ]
}
path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
PY

echo "Release prepared: $apk_name"
