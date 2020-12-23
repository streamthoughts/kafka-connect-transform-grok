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

import org.apache.kafka.common.config.ConfigException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

public class GrokConfigTest {

    @Test(expected = ConfigException.class)
    public void should_throw_exception_given_no_pattern() {
        final GrokConfig config = new GrokConfig(Collections.emptyMap());
        config.patterns();
    }

    @Test
    public void should_return_given_single_pattern() {
        final GrokConfig config = new GrokConfig(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERN_CONFIG, "pattern");
        }});
        Assert.assertEquals(1, config.patterns().size());
        Assert.assertEquals("pattern", config.patterns().get(0));
    }

    @Test
    public void should_return_given_multiple_ordered_patterns() {
        final GrokConfig config = new GrokConfig(new HashMap<>(){{
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG + "3", "pattern3");
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG + "1", "pattern1");
            put(GrokConfig.GROK_PATTERNS_PREFIX_CONFIG + "2", "pattern2");
        }});
        Assert.assertEquals(3, config.patterns().size());
        Assert.assertEquals("pattern1", config.patterns().get(0));
        Assert.assertEquals("pattern2", config.patterns().get(1));
        Assert.assertEquals("pattern3", config.patterns().get(2));
    }
}