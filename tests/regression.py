#!/usr/bin/env python3
import os
import sys

from testflows.core import *

append_path(sys.path, "..")


from tests.helpers.parquetify import build




@TestModule
@Name("regression")
def regression(self):
    """Test module for the parquetify regression tests."""
    build()




if main():
    regression()