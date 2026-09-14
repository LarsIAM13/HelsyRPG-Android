import os
import sys


def run_server(runtime_dir: str):
    runtime_dir = os.path.abspath(runtime_dir)
    os.chdir(runtime_dir)
    if runtime_dir not in sys.path:
        sys.path.insert(0, runtime_dir)
    os.environ["HELSY_PORT"] = "8765"

    import server
    server.main()
