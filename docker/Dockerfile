FROM python:3.8-slim

RUN apt-get update -y \
    && apt-get install -y software-properties-common \
    && apt install default-jre -y \
    && apt-get clean

COPY target target
RUN pip install -r target/sire-lib/requirements.txt
