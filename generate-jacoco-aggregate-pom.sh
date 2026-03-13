#!/bin/bash
#
# Generates .jacoco-report/pom.xml for JaCoCo report-aggregate goal.
# The POM depends on all JAR modules in the reactor, allowing
# report-aggregate to find their class files and produce a unified XML report.
#
# Prerequisites: the root pom.xml sonar profile includes .jacoco-report as a module.
#
# Usage:
#   ./generate-jacoco-aggregate-pom.sh
#   mvn install -Psonar
#   mvn org.jacoco:jacoco-maven-plugin:report-aggregate -Psonar
#
# Output:
#   .jacoco-report/target/site/jacoco-aggregate/jacoco.xml
#

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$ROOT_DIR/.jacoco-report"
OUTPUT="$REPORT_DIR/pom.xml"

mkdir -p "$REPORT_DIR"

# Extract project-level groupId, artifactId, packaging from a pom.xml.
# Outputs three lines: groupId, artifactId, packaging.
# Falls back to parent groupId when not directly specified.
# Ignores <dependencies>, <build>, <modules>, <profiles> sections.
pom_coords() {
    sed 's/ xmlns[^"]*"[^"]*"//g' "$1" | awk '
        /<dependencies>|<dependencyManagement>|<build>|<modules>|<profiles>/ { stop=1 }
        stop { next }
        /<parent>/  { in_parent=1 }
        /<\/parent>/ { in_parent=0; next }

        in_parent && /<groupId>/ && !parent_gid {
            gsub(/.*<groupId>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/groupId>.*/, "")
            parent_gid = $0
            next
        }
        !in_parent && /<groupId>/ && !gid {
            gsub(/.*<groupId>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/groupId>.*/, "")
            gid = $0
            next
        }
        !in_parent && /<artifactId>/ && !aid {
            gsub(/.*<artifactId>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/artifactId>.*/, "")
            aid = $0
            next
        }
        !in_parent && /<packaging>/ && !pkg {
            gsub(/.*<packaging>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/packaging>.*/, "")
            pkg = $0
            next
        }
        !in_parent && /<version>/ && !ver {
            gsub(/.*<version>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/version>.*/, "")
            ver = $0
            next
        }
        END {
            if (!gid) gid = parent_gid
            if (!pkg) pkg = "jar"
            print gid
            print aid
            print pkg
            print ver
        }
    '
}

# Extract all <module> entries from a pom.xml (from top-level and profiles).
pom_modules() {
    sed 's/ xmlns[^"]*"[^"]*"//g' "$1" | awk '
        /<module>/ {
            gsub(/.*<module>[[:space:]]*/, ""); gsub(/[[:space:]]*<\/module>.*/, "")
            if ($0 != ".jacoco-report") print
        }
    '
}

# ── Collect JAR modules by walking the reactor ──────────────────────

DEP_XML=""
COUNT=0
VISITED_FILE="$REPORT_DIR/.visited.tmp"
: > "$VISITED_FILE"

walk_modules() {
    local dir="$1"
    local real
    real="$(cd "$dir" 2>/dev/null && pwd -P)" || return 0
    if grep -qxF "$real" "$VISITED_FILE" 2>/dev/null; then return 0; fi
    echo "$real" >> "$VISITED_FILE"

    local pom="$dir/pom.xml"
    [ -f "$pom" ] || return 0

    # Skip root module from dependencies
    if [ "$dir" != "$ROOT_DIR" ]; then
        local coords gid aid packaging
        coords=$(pom_coords "$pom")
        gid=$(echo "$coords" | sed -n '1p')
        aid=$(echo "$coords" | sed -n '2p')
        packaging=$(echo "$coords" | sed -n '3p')

        if [ "$packaging" = "jar" ] && [ -n "$gid" ] && [ -n "$aid" ]; then
            DEP_XML+="        <dependency>
            <groupId>${gid}</groupId>
            <artifactId>${aid}</artifactId>
            <version>\${project.version}</version>
        </dependency>
"
            COUNT=$((COUNT + 1))
        fi
    fi

    # Recurse into child modules
    local mod
    while IFS= read -r mod; do
        [ -z "$mod" ] && continue
        walk_modules "$dir/$mod"
    done < <(pom_modules "$pom")
}

walk_modules "$ROOT_DIR"
rm -f "$VISITED_FILE"

# ── Read root POM coordinates ───────────────────────────────────────

ROOT_COORDS=$(pom_coords "$ROOT_DIR/pom.xml")
ROOT_GROUP=$(echo "$ROOT_COORDS" | sed -n '1p')
ROOT_ARTIFACT=$(echo "$ROOT_COORDS" | sed -n '2p')
ROOT_VERSION=$(echo "$ROOT_COORDS" | sed -n '4p')

# ── Write the aggregate POM ─────────────────────────────────────────

cat > "$OUTPUT" << XMLEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>${ROOT_GROUP}</groupId>
        <artifactId>${ROOT_ARTIFACT}</artifactId>
        <version>${ROOT_VERSION}</version>
    </parent>

    <artifactId>jacoco-aggregate-report</artifactId>
    <packaging>pom</packaging>
    <name>JaCoCo Aggregate Report</name>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.14</version>
                <configuration>
                    <!-- All modules write to a shared exec file in the root target dir -->
                    <dataFileIncludes>
                        <include>\${maven.multiModuleProjectDirectory}/target/jacoco.exec</include>
                    </dataFileIncludes>
                    <includes>
                        <include>org/openl/**/*</include>
                    </includes>
                    <excludes>
                        <exclude>org/openl/generated/**/*</exclude>
                    </excludes>
                    <formats>
                        <format>XML</format>
                    </formats>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
${DEP_XML}    </dependencies>
</project>
XMLEOF

echo "Generated: $OUTPUT"
echo "  ${COUNT} JAR dependencies included"
