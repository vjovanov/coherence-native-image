#! /bin/bash

tmpdir=$(mktemp -d)

jq --slurpfile typesToRemove registered-elements.json '
                                        if has("reflection") then
                                          .reflection |= map(select(.type as $t | $typesToRemove | index($t) | not))
                                        else
                                          .
                                        end
                                      ' coherence-native-server/src/main/resources/META-INF/native-image/com.oracle.coherence/coherenece/reachability-metadata.json > $tmpdir/reachability-metadata.json

cp $tmpdir/reachability-metadata.json  coherence-native-server/src/main/resources/META-INF/native-image/com.oracle.coherence/coherenece/reachability-metadata.json
