#!/bin/bash
NATIVE_IMAGE_OPTIONS='--exact-reachability-metadata -Ob' mvn clean verify -Pnative,native-test
