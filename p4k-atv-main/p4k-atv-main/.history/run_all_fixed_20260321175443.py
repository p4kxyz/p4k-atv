import shutil
import subprocess
import sys

# 1. Restore file
shutil.copy2('backup PlayerActivity.java', 'app/src/main/java/com/files/codes/view/PlayerActivity.java')

# 2. Run node replacements
try:
    subprocess.run(['node', 'run_replacements.js'], check=True)
    print("Node JS completed.")
except Exception as e:
    print(f"Node err: {e}")

# 3. Run gradlew
try:
    result = subprocess.run(['gradlew.bat', 'compileDebugJavaWithJavac'], capture_output=True, text=True)
    print("Gradle exit code:", result.returncode)
    # save output to file so we can view it
    with open('build_out.txt', 'w', encoding='utf-8') as f:
        f.write(result.stdout)
        f.write("\n")
        f.write(result.stderr)
except Exception as e:
    print(f"Gradle err: {e}")
