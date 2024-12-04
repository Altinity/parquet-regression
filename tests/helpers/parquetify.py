import subprocess
import os

def build(architecture='x86'):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    build_script = os.path.join(script_dir, '../../build')
    try:
        subprocess.run([build_script, architecture], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Error: Command '{e.cmd}' returned non-zero exit status {e.returncode}.")