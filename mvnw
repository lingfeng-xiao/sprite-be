#!/bin/sh
# Maven wrapper script

# Resolve project base directory
BASEDIR=$(dirname "$0")
MAVEN_PROJECTBASEDIR=${MAVEN_PROJECTBASEDIR:-"$BASEDIR"}
export MAVEN_PROJECTBASEDIR

# Download maven wrapper jar if not present
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading maven-wrapper.jar..."
    if command -v curl > /dev/null 2>&1; then
        curl -sL "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -o "$WRAPPER_JAR"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -O "$WRAPPER_JAR"
    fi
fi

# Read wrapper properties to get distribution URL
if [ -f "$WRAPPER_PROPERTIES" ]; then
    DISTRIBUTION_URL=$(grep "distributionUrl=" "$WRAPPER_PROPERTIES" | cut -d'=' -f2)
fi

# Download maven if not present
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.6"
if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    if command -v curl > /dev/null 2>&1; then
        curl -sL "${DISTRIBUTION_URL}" -o "/tmp/maven.zip"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "${DISTRIBUTION_URL}" -O "/tmp/maven.zip"
    fi
    unzip -q "/tmp/maven.zip" -d "$MAVEN_HOME"
    rm "/tmp/maven.zip"
fi

# Execute maven
exec "$MAVEN_HOME/apache-maven-3.9.6/bin/mvn" "$@"
