import os
import re
import urllib.request

BASE = "https://static.nbp.pl/strony/bezpiecznepieniadze"
DENOMS = ["10", "20", "50", "100", "200", "500"]

ROOT = os.path.join("data", "nbp_pln_raw")
os.makedirs(ROOT, exist_ok=True)

for denom in DENOMS:
    page_url = f"{BASE}/{denom}.html"
    try:
        page = urllib.request.urlopen(page_url).read().decode("utf-8", "ignore")
    except Exception as exc:
        print(f"Failed to download page {page_url}: {exc}")
        continue

    regex = re.compile(rf"assets/img/content/(?:front|back)-{denom}[^\"']*\.jpg")
    files = sorted(set(regex.findall(page)))
    if not files:
        print(f"No images found for {denom} zł")
        continue

    target_dir = os.path.join(ROOT, denom)
    os.makedirs(target_dir, exist_ok=True)

    for rel_path in files:
        url = f"{BASE}/{rel_path}"
        filename = os.path.basename(rel_path)
        path = os.path.join(target_dir, filename)
        print(f"Downloading {url} -> {path}")
        try:
            urllib.request.urlretrieve(url, path)
        except Exception as exc:
            print(f"Failed to download {url}: {exc}")
