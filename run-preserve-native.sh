#!/bin/bash
NATIVE_IMAGE_OPTIONS='--exact-reachability-metadata -Ob -H:Preserve=module=ALL-UNNAMED --initialize-at-build-time=org.junit.platform.commons.support.scanning.DefaultClasspathScanner' mvn clean verify -Pnative,native-test
