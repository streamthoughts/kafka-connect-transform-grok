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
package io.streamthoughts.kafka.connect.transform.pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GrokMatcherTest {

    private GrokPatternCompiler compiler;

    @Before
    public void setUp() {
        compiler = new GrokPatternCompiler(new GrokPatternResolver(), false);
    }

    @Test
    public void should_parse_given_simple_grok_pattern() {
        final GrokMatcher matcher = compiler.compile("%{EMAILADDRESS}");
        final Map<String, Object> captured = matcher.captures("test@kafka.org".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("kafka.org", captured.get("HOSTNAME"));
        Assert.assertEquals("test@kafka.org", captured.get("EMAILADDRESS"));
        Assert.assertEquals("test", captured.get("EMAILLOCALPART"));
    }

    @Test
    public void should_convert_captured_values_to_type_specified_in_pattern() {
        final GrokMatcher matcher = compiler.compile(
            "%{NUMBER:aShort:SHORT} %{NUMBER:anInteger:INTEGER} %{NUMBER:aLong:LONG} " +
            "%{NUMBER:aFloat:FLOAT} %{NUMBER:aDouble:DOUBLE} %{WORD:aBoolean:BOOLEAN} %{WORD:aString:STRING}"
        );
        final Map<String, Object> captured = matcher.captures(
            "42 42 42 3.14 3.14 true hello".getBytes(StandardCharsets.UTF_8)
        );

        Assert.assertTrue(captured.get("aShort")   instanceof Short);
        Assert.assertTrue(captured.get("anInteger") instanceof Integer);
        Assert.assertTrue(captured.get("aLong")     instanceof Long);
        Assert.assertTrue(captured.get("aFloat")    instanceof Float);
        Assert.assertTrue(captured.get("aDouble")   instanceof Double);
        Assert.assertTrue(captured.get("aBoolean")  instanceof Boolean);
        Assert.assertTrue(captured.get("aString")   instanceof String);

        Assert.assertEquals((short) 42, captured.get("aShort"));
        Assert.assertEquals(42,         captured.get("anInteger"));
        Assert.assertEquals(42L,        captured.get("aLong"));
        Assert.assertEquals(3.14f,      (Float)  captured.get("aFloat"),  0.001f);
        Assert.assertEquals(3.14,       (Double) captured.get("aDouble"), 0.001);
        Assert.assertEquals(true,       captured.get("aBoolean"));
        Assert.assertEquals("hello",    captured.get("aString"));
    }

    @Test
    public void should_parse_given_custom_grok_pattern() {
        final GrokMatcher matcher = compiler.compile("(?<EMAILADDRESS>(?<EMAILLOCALPART>[a-zA-Z][a-zA-Z0-9_.+-=:]+)@(?<HOSTNAME>\\b(?:[0-9A-Za-z][0-9A-Za-z-]{0,62})(?:\\.(?:[0-9A-Za-z][0-9A-Za-z-]{0,62}))*(\\.?|\\b)))");
        final Map<String, Object> captured = matcher.captures("test@kafka.org".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("kafka.org", captured.get("HOSTNAME"));
        Assert.assertEquals("test@kafka.org", captured.get("EMAILADDRESS"));
        Assert.assertEquals("test", captured.get("EMAILLOCALPART"));
    }
}