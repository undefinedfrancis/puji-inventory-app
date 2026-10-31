#!/usr/bin/env python3
"""Optional per-build loopback Maven cache using the system TLS downloader.

This does not change global Gradle settings. Stop this process after the build.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit
import os
import subprocess
import tempfile
import threading

root = Path(__file__).resolve().parents[1] / '.tools'
cache = root / 'maven'
cache.mkdir(parents=True, exist_ok=True)
origins = {'google': 'https://dl.google.com/dl/android/maven2/',
           'central': 'https://repo.maven.apache.org/maven2/',
           'plugins': 'https://plugins.gradle.org/m2/'}
lock = threading.Semaphore(4)
shared_artifacts = Path.home() / '.gradle/caches/modules-2/files-2.1'

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_HEAD(self):
        self.serve(False)

    def do_GET(self):
        self.serve(True)

    def serve(self, body):
        parts = PurePosixPath(unquote(urlsplit(self.path).path)).parts
        if len(parts) < 4 or parts[0] != '/' or parts[1] not in origins or any(p in ('..', '.', '') or '\\' in p or '\0' in p for p in parts[1:]):
            self.send_error(404)
            return
        target = cache.joinpath(*parts[1:])
        with lock:
            if not target.is_file():
                target.parent.mkdir(parents=True, exist_ok=True)
                # Published cache files are immutable. Clone only raw artifacts into this task;
                # never reuse mutable cache metadata or locks from another build.
                group = '.'.join(parts[2:-3])
                source_dir = shared_artifacts / group / parts[-3] / parts[-2]
                matches = list(source_dir.glob('*/' + parts[-1])) if group else []
                if matches:
                    subprocess.run(['/bin/cp', '-c', str(matches[0]), str(target)], capture_output=True)
            if not target.is_file():
                fd, name = tempfile.mkstemp(dir=target.parent, prefix='.download-')
                os.close(fd)
                try:
                    result = subprocess.run(['curl', '--fail', '--location', '--silent', '--show-error',
                        '--connect-timeout', '10', '--max-time', '60', '--retry', '1',
                        '--max-filesize', '268435456', '--output', name,
                        origins[parts[1]] + '/'.join(parts[2:])], capture_output=True)
                    if result.returncode:
                        self.send_error(404)
                        return
                    os.replace(name, target)
                finally:
                    if os.path.exists(name):
                        os.unlink(name)
        self.send_response(200)
        self.send_header('Content-Length', str(target.stat().st_size))
        self.send_header('Content-Type', 'application/octet-stream')
        self.end_headers()
        if body:
            try:
                with target.open('rb') as source:
                    while chunk := source.read(65536):
                        self.wfile.write(chunk)
            except (BrokenPipeError, ConnectionResetError):
                pass

server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
port = server.server_address[1]
root.joinpath('maven-port').write_text(str(port))
script = '''settingsEvaluated { settings ->
    settings.pluginManagement.repositories {
        clear()
        ['google', 'central', 'plugins'].each { repo ->
            maven { url = uri('http://127.0.0.1:PORT/' + repo); allowInsecureProtocol = true }
        }
    }
    settings.dependencyResolutionManagement.repositories {
        clear()
        ['google', 'central'].each { repo ->
            maven { url = uri('http://127.0.0.1:PORT/' + repo); allowInsecureProtocol = true }
        }
    }
}
'''.replace('PORT', str(port))
root.joinpath('maven.init.gradle').write_text(script)
print(f'Local build cache ready on 127.0.0.1:{port}', flush=True)
try:
    server.serve_forever()
finally:
    server.server_close()
