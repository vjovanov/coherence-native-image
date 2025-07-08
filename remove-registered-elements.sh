#! /bin/bash

tmpdir=$(mktemp -d)

echo Tmp dir $tmpdir
jq --slurpfile rawTypes registered-elements.json '
  # Extract just the "type" strings from the input objects
  ($rawTypes[0] | map(.type)) as $typesToRemove
  | if has("reflection") then
      .reflection |= map(select(.type as $t | $typesToRemove | index($t) | not))
    else
      .
    end                                      
' coherence-native-server/src/main/resources/META-INF/native-image/com.oracle.coherence/coherenece/reachability-metadata.json > $tmpdir/reachability-metadata.json

cp $tmpdir/reachability-metadata.json  coherence-native-server/src/main/resources/META-INF/native-image/com.oracle.coherence/coherenece/reachability-metadata.json
