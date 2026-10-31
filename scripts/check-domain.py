#!/usr/bin/env python3
"""Run platform-free Kotlin rules with the installed compiler cache; no Gradle daemon."""
import glob
import pathlib
import subprocess
import sys
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
cache = pathlib.Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, artifact, version='*'):
    paths = sorted(glob.glob(str(cache / group / artifact / version / '*' / '*.jar')))
    if not paths:
        raise SystemExit(f'Missing cached tool: {group}:{artifact}:{version}')
    return paths[-1]

compiler = jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.21')
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.21')
junit = jar('junit', 'junit', '4.13.2')
hamcrest = jar('org.hamcrest', 'hamcrest-core')
dependencies = [compiler, stdlib, junit, hamcrest]
for group, artifact in [('org.jetbrains.kotlin', 'kotlin-script-runtime'), ('org.jetbrains.kotlin', 'kotlin-reflect'), ('org.jetbrains.intellij.deps', 'trove4j'), ('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), ('org.jetbrains', 'annotations')]:
    matches = sorted(glob.glob(str(cache / group / artifact / '*' / '*' / '*.jar')))
    if matches:
        dependencies.append(matches[-1])

sources = list((root / 'app/src/main/java/cn/puji/app/domain').glob('*.kt'))
sources += [root / 'app/src/main/java/cn/puji/app/scanner/ScannerController.kt']
sources += [root / 'app/src/main/java/cn/puji/app/export/CsvExporter.kt']
sources += list((root / 'app/src/main/java/cn/puji/app/backup').glob('*.kt'))
sources += [root / 'app/src/main/java/cn/puji/app/scanner/ScanTransactionGate.kt',
            root / 'app/src/main/java/cn/puji/app/scanner/ScannedProductRegistration.kt',
            root / 'app/src/main/java/cn/puji/app/ui/product/ProductFormValidator.kt',
            root / 'app/src/main/java/cn/puji/app/AppIdentity.kt']
tests = [root / 'app/src/test/java/cn/puji/app/scanner/ContinuousScannerTest.kt', root / 'app/src/test/java/cn/puji/app/domain/CartSafetyTest.kt']
if sys.argv[1:] == ['--all']:
    test_root = root / 'app/src/test/java/cn/puji/app'
    tests = [test_root / 'SmokeTest.kt']
    for folder in ['domain', 'scanner', 'export', 'backup', 'ui/product']:
        tests += sorted((test_root / folder).glob('*Test.kt'))
elif sys.argv[1:]:
    tests = [root / path for path in sys.argv[1:]]
output = pathlib.Path(tempfile.mkdtemp(prefix='puji-v2-rules-'))
java = '/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home/bin/java'
classpath = ':'.join([stdlib, junit, hamcrest])
subprocess.run([java, '-Xmx384m', '-XX:ActiveProcessorCount=1', '-cp', ':'.join(dependencies), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-jvm-target', '17', '-classpath', classpath, '-d', str(output), *map(str, sources + tests)], check=True)
names = ['cn.puji.app.' + str(path.relative_to(root / 'app/src/test/java/cn/puji/app')).replace('/', '.').removesuffix('.kt') for path in tests]
result = subprocess.run([java, '-Xmx256m', '-XX:ActiveProcessorCount=1', '-Duser.timezone=Asia/Shanghai', '-cp', str(output) + ':' + classpath, 'org.junit.runner.JUnitCore', *names])
sys.exit(result.returncode)
