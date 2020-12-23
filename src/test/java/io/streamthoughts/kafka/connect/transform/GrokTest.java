/*
 * Copyright 2020 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.kafka.connect.transform;

import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class GrokTest<R extends ConnectRecord<R>>  {

    @Test
    public void should_extract_named_captured_given_config_with_single_pattern() {
        final Grok.Value<SourceRecord> grok = new Grok.Value<>();
        grok.configure(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERN_CONFIG, "%{EMAILADDRESS}");
            put(GrokConfig.GROK_NAMED_CAPTURES_ONLY_CONFIG, false);
        }});
        final SourceRecord record = new SourceRecord(
            null,
            null,
            "topic",
            Schema.STRING_SCHEMA,
            null,
            Schema.STRING_SCHEMA,
            "test@apache.kafka.org"
        );
        final SourceRecord result = grok.apply(record);

        assertNotNull(result);
        final Schema valueSchema = result.valueSchema();
        assertNotNull(valueSchema.field("HOSTNAME"));
        assertNotNull(valueSchema.field("EMAILADDRESS"));
        assertNotNull(valueSchema.field("EMAILLOCALPART"));

        final Struct value = (Struct) result.value();
        Assert.assertEquals("apache.kafka.org", value.get("HOSTNAME"));
        Assert.assertEquals("test", value.get("EMAILLOCALPART"));
        Assert.assertEquals("test@apache.kafka.org", value.get("EMAILADDRESS"));
    }
}