#!/bin/bash

# Define the base directory of the project
APP_DIR="/app"
# Define the output directory for compiled classes
BIN_DIR="bin"

# Navigate to the app directory
cd "${APP_DIR}"

# Create the bin directory if it doesn't exist
mkdir -p "${BIN_DIR}"

# Find all jar files within the app directory, prioritizing BoostNSift/lib
# Put BoostNSift/lib JARs first to avoid conflicts with old Lucene versions
BOOSTNSIFT_JARS=$(find "${APP_DIR}/BoostNSift/lib" -name "*.jar" 2>/dev/null | tr '\n' ':')
# Find other JARs but exclude old Lucene JARs from Corpus-Generator/lib
# Use grep to filter out the old lucene JARs
ALL_OTHER_JARS=$(find "${APP_DIR}" -name "*.jar" ! -path "${APP_DIR}/BoostNSift/lib/*" 2>/dev/null)
OTHER_JARS=$(echo "$ALL_OTHER_JARS" | grep -v "lucene-core-2.9.4.jar" | grep -v "Corpus-Generator/lib/lucene-" | tr '\n' ':')
CLASSPATH="${BOOSTNSIFT_JARS}${OTHER_JARS}"

#save the classpath to a file
echo "${CLASSPATH}" > classpath.txt

# Find all Java source files and save their paths to a file
find "${APP_DIR}" -name "*.java" > sources.txt

# Compile all Java source files into the bin directory
javac -cp "${CLASSPATH}" @sources.txt -d "${BIN_DIR}" || { echo 'Compilation failed'; exit 1; }

# Package the compiled classes and resources into a single jar
# Note: If you have resources (like images or configuration files), ensure they are included correctly.
# The launcher.Launcher is the fully qualified name of your main class
(cd "${BIN_DIR}" && jar cvfe ../BoostNSift_V_1.jar launcher.Launcher .)

# Cleanup the sources list
rm sources.txt

echo "Build completed successfully."

