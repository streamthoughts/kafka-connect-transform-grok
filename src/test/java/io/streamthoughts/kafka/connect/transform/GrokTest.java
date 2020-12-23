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

import static org.junit.Assert.assertNotNull;

public class GrokTest<R extends ConnectRecord<R>>  {

    @Test
    public void should_extract_named_captured_given_config_with_single_pattern() {
        final Grok.Value<SourceRecord> grok = new Grok.Value<>();
        grok.configure(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERN_CONFIG, "%{EMAILADDRESS}");
            put(GrokConfig.GROK_NAMED_CAPTURES_ONLY_CONFIG, false);
        }});
        final SourceRecord record = newSourceRecord("test@apache.kafka.org");
        final SourceRecord result = grok.apply(record);

        assertNotNull(result);
        assertResultSchemaAndValue(result);
    }

    @Test
    public void should_extract_named_captured_given_config_with_multiple_patterns_and_break_false() {

        final Grok.Value<SourceRecord> grok = new Grok.Value<>();
        grok.configure(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG+"1", "%{NUMBER}");
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG+"2", "%{EMAILADDRESS}");
            put(GrokConfig.GROK_NAMED_CAPTURES_ONLY_CONFIG, false);
            put(GrokConfig.GROK_PATTERN_BREAK_ON_FIRST_PATTERN, false);
        }});

        final SourceRecord record = newSourceRecord("1 test@apache.kafka.org");
        final SourceRecord result = grok.apply(record);

        assertNotNull(result);
        assertResultSchemaAndValue(result);

        final Schema valueSchema = result.valueSchema();
        assertNotNull(valueSchema.field("NUMBER"));
        final Struct value = (Struct) result.value();
        Assert.assertEquals("1", value.get("NUMBER"));
    }

    @Test
    public void should_extract_named_captured_given_config_with_multiple_patterns_and_break_true() {

        final Grok.Value<SourceRecord> grok = new Grok.Value<>();
        grok.configure(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG+"1", "%{NUMBER}");
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG+"2", "%{EMAILADDRESS}");
            put(GrokConfig.GROK_NAMED_CAPTURES_ONLY_CONFIG, false);
            put(GrokConfig.GROK_PATTERN_BREAK_ON_FIRST_PATTERN, true);
        }});

        final SourceRecord record = newSourceRecord("test@apache.kafka.org");
        final SourceRecord result = grok.apply(record);

        assertNotNull(result);
        assertResultSchemaAndValue(result);
    }

    private void assertResultSchemaAndValue(SourceRecord result) {
        final Schema valueSchema = result.valueSchema();
        assertNotNull(valueSchema.field("HOSTNAME"));
        assertNotNull(valueSchema.field("EMAILADDRESS"));
        assertNotNull(valueSchema.field("EMAILLOCALPART"));

        final Struct value = (Struct) result.value();
        Assert.assertEquals("apache.kafka.org", value.get("HOSTNAME"));
        Assert.assertEquals("test", value.get("EMAILLOCALPART"));
        Assert.assertEquals("test@apache.kafka.org", value.get("EMAILADDRESS"));
    }

    private static SourceRecord newSourceRecord(final String value) {
        return new SourceRecord(
            null,
            null,
            "topic",
            Schema.STRING_SCHEMA,
            null,
            Schema.STRING_SCHEMA,
            value
        );
    }
}