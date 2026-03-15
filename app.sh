#!/bin/bash

# Ensure we exit if any command fails
set -e

# Load environment variables from .env file if it exists
if [ -f .env ]; then
  echo "Loading environment variables from .env file..."
  # 'set -a' automatically exports all variables, 'set +a' disables it
  set -a
  source .env
  set +a
else
  echo "Warning: .env file not found. Starting without it."
fi

if [ "$1" = "trace" ]; then
  echo "Building JAR..."
  ./gradlew bootJar -PbuildJvm --no-daemon -q

  JAR=$(ls build/libs/*.jar | grep -v plain | head -1)
  METADATA="src/main/resources/META-INF/native-image/kr.devport.backend/devport-api-agent/reachability-metadata.json"

  echo "Starting with GraalVM tracing agent (use Ctrl+C to stop and generate configs)..."
  java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/kr.devport.backend/devport-api-agent/ \
    -jar "$JAR" || true

  # The tracing agent captures ServiceLoader entries that exist on JVM but must be
  # excluded in native images. Spring ORM ships -H:ServiceLoaderFeatureExcludeServices
  # for BytecodeProvider, but the tracing agent's resource glob overrides that exclusion.
  if [ -f "$METADATA" ]; then
    echo "Cleaning up tracing agent output..."
    # Remove the BytecodeProvider ServiceLoader glob that conflicts with Spring ORM's native-image.properties
    sed -i '' '/"glob": "META-INF\/services\/org.hibernate.bytecode.spi.BytecodeProvider"/,+1d' "$METADATA"
    # Clean up any leftover empty lines or trailing commas from the removal
    sed -i '' '/^$/d' "$METADATA"
    echo "Removed BytecodeProvider ServiceLoader entry from reachability-metadata.json"
  fi
else
  echo "Starting Spring Boot application on JVM..."
  exec ./gradlew bootRun -PbuildJvm
fi
