#!/usr/bin/env python3
"""Run the native-call timeout regression with the Gradle wrapper's Kotlin compiler.

Requires Java and a populated Gradle wrapper cache (run ./gradlew --version).
The test extracts the actual timeout helper and uses a blocking call in place of JNI.
"""
import glob
import os
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def run(args):
    subprocess.run(args, check=True, timeout=60)


def kotlin_tests(tmp):
    properties = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text()
    version = re.search(r"gradle-([\d.]+)-(?:bin|all)\.zip", properties).group(1)
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    libs = glob.glob(str(gradle_home / f"wrapper/dists/gradle-{version}-*/*/gradle-{version}/lib"))
    if not libs:
        raise RuntimeError("Gradle compiler cache is required; run ./gradlew --version first")
    lib = Path(libs[0])
    stdlib = next(lib.glob("kotlin-stdlib-*.jar"))
    text = (ROOT / "app/src/main/java/me/bmax/apatch/util/AppData.kt").read_text()
    body = text[text.index("    private fun <T> runNativeWithTimeout"):text.index("    object DataRefreshManager")]
    source = tmp / "Main.kt"
    source.write_text('''import java.util.concurrent.Semaphore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
object Log { fun w(tag: String, msg: String) {} }
object Probe {
    private const val TAG = "test"
    private val nativeCallPermit = Semaphore(1)
''' + body + '''
    fun call(block: () -> Int) = runNativeWithTimeout(10, -1, block)
}
fun main() {
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    check(Probe.call { started.countDown(); release.await(); 7 } == -1)
    check(started.await(1, TimeUnit.SECONDS))
    repeat(1000) { check(Probe.call { error("extra worker") } == -1) }
    release.countDown()
    var result = -1
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
    while (result == -1 && System.nanoTime() < deadline) {
        Thread.sleep(5)
        result = Probe.call { 42 }
    }
    check(result == 42)
    println("PASS: 1000 retries while JNI is blocked create no extra worker; calls resume after completion")
}
''')
    out = tmp / "kotlin-out"
    run(["java", "-cp", str(lib / "*"), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
         "-no-stdlib", "-no-reflect", "-classpath", str(stdlib), "-d", str(out), str(source)])
    run(["java", "-cp", str(out) + os.pathsep + str(stdlib), "MainKt"])


if __name__ == "__main__":
    with tempfile.TemporaryDirectory(prefix="folkpatch-tests-") as directory:
        kotlin_tests(Path(directory))
