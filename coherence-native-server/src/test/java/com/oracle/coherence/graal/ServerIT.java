/*
 * Copyright (c) 28/05/2025, 14:06, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal;

import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.graal.testing.NativeApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerIT {

    @Test
    void shouldStartSimpleClusterMember() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ApplicationConsole console = testLogs.builder()
                .addStdErrListener(s -> s.contains("Started Coherence server"), s -> latch.countDown())
                .build("server");

        try (NativeApplication server = LocalPlatform.get().launch(NativeApplication.class,
                Arguments.of(
                        // "-agentpath:/home/vjovanov/c/g/lottet/GR-63591-bundles-as-resources/ce/sdk/mxbuild/linux-amd64/GRAALVM_NI_EE_JAVA25/graalvm-ni-ee-jdk-25+23.1/lib/libnative-image-agent.so=config-merge-dir=/tmp/native-metadata-server",
                        "-Dcoherence.lambdas=static",
                        "-Djava.net.preferIPv4Stack=true",
                        "-Dcoherence.cluster=native-image-test-1",
                        "-Dcoherence.localhost=127.0.0.1",
                        "-Dcoherence.wka=127.0.0.1"),
                ClassName.of(Server.class),
                ClassPath.automatic(),
                DisplayName.of("server"),
                Console.of(console))) {
            boolean awaitMessage = latch.await(1, TimeUnit.MINUTES);
            assertThat(awaitMessage, is(true));
        }
    }


    @Test
    void shouldStartTwoMemberCluster() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        ApplicationConsole console1 = testLogs.builder()
                .addStdErrListener(s -> s.contains("Started Coherence server"), s -> latch1.countDown())
                .build("server-1");

        ApplicationConsole console2 = testLogs.builder()
                .addStdErrListener(s -> s.contains("Started Coherence server"), s -> latch2.countDown())
                .addStdErrListener(s -> s.contains("Partition ownership has stabilized with 2 nodes"), s -> latch3.countDown())
                .build("server-2");

        try (NativeApplication server1 = LocalPlatform.get().launch(NativeApplication.class,
                Arguments.of(
                        // "-agentpath:/home/vjovanov/c/g/lottet/GR-63591-bundles-as-resources/ce/sdk/mxbuild/linux-amd64/GRAALVM_NI_EE_JAVA25/graalvm-ni-ee-jdk-25+23.1/lib/libnative-image-agent.so=config-merge-dir=/tmp/native-metadata-server-1",
                        "-Dcoherence.lambdas=static",
                        "-Djava.net.preferIPv4Stack=true",
                        "-Dcoherence.cluster=native-image-test-2",
                        "-Dcoherence.localhost=127.0.0.1",
                        "-Dcoherence.wka=127.0.0.1"),
                ClassName.of(Server.class),
                ClassPath.automatic(),
                DisplayName.of("server-1"),
                Console.of(console1));

             NativeApplication server2 = LocalPlatform.get().launch(NativeApplication.class,
                     Arguments.of(
                             // TODO: we need to enable the agent as a mode for collecting metadata
                             // ISSUE: standard `-agentlib:` doesn't work as expected. The full path is needed.
                             // "-agentpath:/home/vjovanov/c/g/lottet/GR-63591-bundles-as-resources/ce/sdk/mxbuild/linux-amd64/GRAALVM_NI_EE_JAVA25/graalvm-ni-ee-jdk-25+23.1/lib/libnative-image-agent.so=config-merge-dir=/tmp/native-metadata-server-2",

                             // Lambdas must use standard Java serialization
                             "-Dcoherence.lambdas=static",
                             "-Djava.net.preferIPv4Stack=true",
                             "-Dcoherence.cluster=native-image-test-2",
                             "-Dcoherence.localhost=127.0.0.1",
                             "-Dcoherence.wka=127.0.0.1"),
                     ClassName.of(Server.class),
                     ClassPath.automatic(),
                     DisplayName.of("server-2"),
                     Console.of(console2))) {
            assertThat(latch1.await(1, TimeUnit.MINUTES), is(true));
            assertThat(latch1.await(2, TimeUnit.MINUTES), is(true));
        }
    }

    @RegisterExtension
    static TestLogsExtension testLogs = new TestLogsExtension(ServerIT.class);
}
