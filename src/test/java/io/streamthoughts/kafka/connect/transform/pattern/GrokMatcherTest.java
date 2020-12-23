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
    public void should_parse_given_custom_grok_pattern() {
        final GrokMatcher matcher = compiler.compile("(?<EMAILADDRESS>(?<EMAILLOCALPART>[a-zA-Z][a-zA-Z0-9_.+-=:]+)@(?<HOSTNAME>\\b(?:[0-9A-Za-z][0-9A-Za-z-]{0,62})(?:\\.(?:[0-9A-Za-z][0-9A-Za-z-]{0,62}))*(\\.?|\\b)))");
        final Map<String, Object> captured = matcher.captures("test@kafka.org".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("kafka.org", captured.get("HOSTNAME"));
        Assert.assertEquals("test@kafka.org", captured.get("EMAILADDRESS"));
        Assert.assertEquals("test", captured.get("EMAILLOCALPART"));
    }
}