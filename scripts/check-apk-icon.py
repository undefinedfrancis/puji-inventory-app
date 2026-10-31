#!/usr/bin/env python3
"""Check that APK metadata exposes extractable bitmap launcher icons."""
import argparse
import re
import struct
import subprocess
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("apk")
parser.add_argument("--aapt", required=True)
args = parser.parse_args()
badging = subprocess.check_output([args.aapt, "dump", "badging", args.apk], text=True)
paths = re.findall(r"^application-icon-\d+:'([^']+)'", badging, re.M)
application = re.search(r"^application: .*?icon='([^']+)'", badging, re.M)
assert application is not None and paths, "APK has no application icon metadata"
paths.append(application.group(1))
with zipfile.ZipFile(args.apk) as apk:
    for path in sorted(set(paths)):
        data = apk.read(path)
        assert data[:8] == b"\x89PNG\r\n\x1a\n", f"Icon is not a directly extractable PNG: {path}"
        width, height = struct.unpack(">II", data[16:24])
        assert width == height and width >= 48, f"Invalid icon dimensions: {path} ({width}x{height})"
        print(f"OK: {path} ({width}x{height} PNG)")
print("APK bitmap icon extraction passed")
