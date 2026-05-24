#!/bin/sh

if [ -f /app/.env ]; then
    set -a
    . /app/.env
    set +a
fi

exec java -jar /app/app.jar
