#!/bin/bash
NATIVE_IMAGE_OPTIONS='--exact-reachability-metadata -Ob -H:Preserve=all -H:+MetadataTracingSupport --initialize-at-build-time=org.junit.platform.commons.support.scanning.DefaultClasspathScanner' mvn clean verify -Pnative,native-test
