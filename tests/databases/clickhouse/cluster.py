from os.path import abspath

from testflows.core import *
from testflows.connect import Shell as ShellBase
from testflows.settings import database


class Shell(ShellBase):
    def __exit__(self, type, value, traceback):
        # send exit and Ctrl-D repeatedly
        # to terminate any open shell commands.
        # This is needed for example
        # to solve a problem with
        # 'docker-compose exec {name} bash --noediting'
        # that does not clean up open bash processes
        # if not exited normally
        for i in range(10):
            if self.child is not None:
                try:
                    self.send("exit\r", eol="")
                    self.send("\x04\r", eol="")
                except OSError:
                    pass
        return super(Shell, self).__exit__(type, value, traceback)

class BaseDatabase:
    def __init__(self, test_dir):
        self.test_dir = test_dir

    def connect(self, database):
        with Given("I bring up the docker compose cluster"):
            with Shell() as bash:
                bash(f"docker-compose -f {self.test_dir}/databases/{database}/docker-compose.yml up -d")

    def execute_query(self, query: str):
        raise NotImplementedError

    def load_parquet(self, file_path: str):
        raise NotImplementedError

    def validate_data(self, expected_data: list):
        raise NotImplementedError


class ClickHouse(BaseDatabase):
    """ClickHouse database class."""
    def connect(self, database="clickhouse"):
        pass

    def load_parquet(self, file_path):
        pass

    def query(self, query):
        pass

    def cleanup(self):
        pass