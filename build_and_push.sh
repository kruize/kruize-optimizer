#!/bin/bash
set -euo pipefail

# Usage function
usage() {
    local exit_code="${1:-1}"
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -i IMAGE_NAME    Full image name (registry/repository:tag)"
    echo "  -t TAG           Image tag (default: project version from pom.xml) - used only if -i is not provided"
    echo "  -b BUILD         Build image true/false (default: true)"
    echo "  -p PUSH          Push image true/false (default: true)"
    echo "  -l PLATFORMS     Target platforms (default: linux/amd64,linux/arm64)"
    echo "  -h               Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -i quay.io/myuser/kruize-optimizer:1.0.0 -b true -p true"
    echo "  $0 -t 0.1_mvp -p false"
    echo "  $0 -b true -p true -l linux/amd64"
    echo ""
    echo "Note: Use either -i for full image name OR -t for tag with default registry/repo"
    exit "${exit_code}"
}

# Validate boolean values
validate_boolean() {
    local value="$1"
    local flag="$2"
    if [[ ! "$value" =~ ^(true|false)$ ]]; then
        echo "Error: $flag must be 'true' or 'false', got: '$value'" >&2
        usage 1
    fi
}

# Default values
IMAGE_TAG=""
QUARKUS_BUILD="true"
QUARKUS_PUSH="true"
QUARKUS_PLATFORMS="linux/amd64,linux/arm64"
IMAGE_NAME=""

# Parse command line arguments
while getopts "i:t:b:p:l:h" opt; do
    case ${opt} in
        i )
            IMAGE_NAME="$OPTARG"
            ;;
        t )
            IMAGE_TAG="$OPTARG"
            ;;
        b )
            QUARKUS_BUILD="$OPTARG"
            ;;
        p )
            QUARKUS_PUSH="$OPTARG"
            ;;
        l )
            QUARKUS_PLATFORMS="$OPTARG"
            ;;
        h )
            usage 0
            ;;
        \? )
            echo "Invalid option: -$OPTARG" >&2
            usage 1
            ;;
    esac
done

# Validate boolean flags
validate_boolean "$QUARKUS_BUILD" "-b"
validate_boolean "$QUARKUS_PUSH" "-p"

# If IMAGE_NAME is not provided via -i, use default registry/repository with tag
# (tag from -t, otherwise the project version from pom.xml)
if [ -z "$IMAGE_NAME" ]; then
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="$(sed -n 's|^    <version>\(.*\)</version>.*|\1|p' pom.xml | head -n 1)"
        if [ -z "$IMAGE_TAG" ]; then
            echo "Error: could not read project version from pom.xml" >&2
            exit 1
        fi
    fi
    IMAGE_NAME="quay.io/kruize/kruize-optimizer:${IMAGE_TAG}"
fi

echo "Building and pushing image: ${IMAGE_NAME}..."
echo "Build: ${QUARKUS_BUILD}"
echo "Push: ${QUARKUS_PUSH}"
echo "Platforms: ${QUARKUS_PLATFORMS}"

# Build container image using Quarkus Jib extension
./mvnw clean package \
  -Dquarkus.container-image.build="${QUARKUS_BUILD}" \
  -Dquarkus.container-image.image="${IMAGE_NAME}" \
  -Dquarkus.container-image.push="${QUARKUS_PUSH}" \
  -Dquarkus.jib.platforms="${QUARKUS_PLATFORMS}"
