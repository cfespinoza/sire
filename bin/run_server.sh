#!/usr/bin/bash

export PYTHON_EXECUTABLE="/usr/local/bin/python"
export SIRE_CLI_SCRIPT_PATH="/target/sire-lib/cli.py"
export SIRE_DB_FILE_PATH="/target/sire-lib/data/sapiq.json"
export SIRE_SERVER_HOST="0.0.0.0"
export SIRE_SERVER_PORT=${PORT:-8800}

echo " => SIRE_SERVER_HOST: ${SIRE_SERVER_HOST}"
echo " => SIRE_SERVER_PORT: ${SIRE_SERVER_PORT}"

nohup java -jar target/server.jar & > salida.txt 2>&1 &
tail -f /dev/null