#!/bin/bash
# =============================================================================
# UltraMusic Player - Automated Build Script
# =============================================================================
# This script handles building, testing, and preparing releases
#
# Usage:
#   ./scripts/build.sh [command]
#
# Commands:
#   clean      - Clean build directories
#   debug      - Build debug APK
#   release    - Build release APK (requires signing config)
#   test       - Run all unit tests
#   lint       - Run lint checks
#   all        - Run tests, lint, and build release
#   bundle     - Build Android App Bundle for Play Store
#   install    - Build and install debug APK on connected device
#
# Environment Variables (for CI/CD):
#   SIGNING_KEY_ALIAS     - Key alias for release signing
#   SIGNING_KEY_PASSWORD  - Key password
#   SIGNING_STORE_FILE    - Path to keystore file
#   SIGNING_STORE_PASSWORD - Keystore password
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Functions
print_header() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}! $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Change to project root
cd "$PROJECT_ROOT"

# Parse command
COMMAND=${1:-help}

case $COMMAND in
    clean)
        print_header "Cleaning Build Directories"
        ./gradlew clean
        print_success "Clean complete"
        ;;

    debug)
        print_header "Building Debug APK"
        ./gradlew assembleDebug
        print_success "Debug APK built: app/build/outputs/apk/debug/app-debug.apk"
        ;;

    release)
        print_header "Building Release APK"

        # Check for signing configuration
        if [ ! -f "keystore.properties" ] && [ -z "$SIGNING_KEY_ALIAS" ]; then
            print_warning "No signing configuration found!"
            print_warning "Create keystore.properties or set environment variables"
            exit 1
        fi

        ./gradlew assembleRelease
        print_success "Release APK built: app/build/outputs/apk/release/app-release.apk"
        ;;

    test)
        print_header "Running Unit Tests"
        ./gradlew test
        print_success "All tests passed"
        ;;

    lint)
        print_header "Running Lint Checks"
        ./gradlew lintDebug
        print_success "Lint checks complete"
        ;;

    all)
        print_header "Full Build Pipeline"

        echo "Step 1: Clean"
        ./gradlew clean
        print_success "Clean complete"

        echo -e "\nStep 2: Run Tests"
        ./gradlew test
        print_success "Tests passed"

        echo -e "\nStep 3: Lint Checks"
        ./gradlew lintDebug
        print_success "Lint passed"

        echo -e "\nStep 4: Build Release"
        if [ -f "keystore.properties" ] || [ -n "$SIGNING_KEY_ALIAS" ]; then
            ./gradlew assembleRelease
            print_success "Release APK built"
        else
            ./gradlew assembleDebug
            print_warning "No signing config - built debug APK instead"
        fi

        print_success "Build pipeline complete!"
        ;;

    bundle)
        print_header "Building Android App Bundle"

        if [ ! -f "keystore.properties" ] && [ -z "$SIGNING_KEY_ALIAS" ]; then
            print_error "Signing configuration required for App Bundle"
            exit 1
        fi

        ./gradlew bundleRelease
        print_success "App Bundle built: app/build/outputs/bundle/release/app-release.aab"
        ;;

    install)
        print_header "Building and Installing Debug APK"
        ./gradlew installDebug
        print_success "Debug APK installed on device"
        ;;

    version)
        print_header "Version Information"
        grep -E "versionCode|versionName" app/build.gradle.kts
        ;;

    help|*)
        echo "UltraMusic Player Build Script"
        echo ""
        echo "Usage: ./scripts/build.sh [command]"
        echo ""
        echo "Commands:"
        echo "  clean    - Clean build directories"
        echo "  debug    - Build debug APK"
        echo "  release  - Build signed release APK"
        echo "  test     - Run all unit tests"
        echo "  lint     - Run lint checks"
        echo "  all      - Full build pipeline (test + lint + build)"
        echo "  bundle   - Build Android App Bundle for Play Store"
        echo "  install  - Build and install debug on device"
        echo "  version  - Show current version info"
        echo "  help     - Show this help message"
        ;;
esac
