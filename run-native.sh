#!/bin/bash
NATIVE_IMAGE_OPTIONS='--exact-reachability-metadata -Ob -J-Dcom.oracle.coherence.graal.processedElementsPath=registered-elements.json --initialize-at-build-time=org.junit.platform.commons.support.scanning.DefaultClasspathScanner' mvn clean verify -Pnative,native-test
