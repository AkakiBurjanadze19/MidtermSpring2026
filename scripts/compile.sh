#!/usr/bin/env sh
# Compile the project with Maven (pulls in the SLF4J / Hibernate / H2 classpath).
set -eu
mvn -q compile
