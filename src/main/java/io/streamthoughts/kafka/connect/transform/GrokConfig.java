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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrokConfig extends AbstractConfig {

    public static final String GROK_ROW_PATTERN_CONFIG             = "match";
    private static final String GROK_ROW_PATTERN_DOC               = "The Grok pattern to matches.";

    public static final String GROK_ROW_PATTERN_DEFINITIONS_CONFIG = "patternDefinitions";
    private static final String GROK_ROW_PATTERN_DEFINITIONS_DOC   = "Custom pattern definitions";

    public static final String GROK_ROW_PATTERNS_DIR_CONFIG        = "patternsDir";
    private static final String GROK_ROW_PATTERNS_DIR_DOC          = "List of user-defined pattern directories";

    public static final String GROK_ROW_NAMED_CAPTURES_ONLY_CONFIG = "namedCapturesOnly";
    private static final String GROK_ROW_NAMED_CAPTURES_ONLY_DOC   = "If true, only store named captures from grok (default=true).";

    /**
     * Creates a new {@link GrokConfig} instance.
     *
     * @param originals the originals configuration.
     */
    public GrokConfig(final Map<String, ?> originals) {
        super(configDef(), originals);
    }

    public String pattern() {
        return this.getString(GROK_ROW_PATTERN_CONFIG);
    }

    public List<String> patternDefinitions() {
        return this.getList(GROK_ROW_PATTERN_DEFINITIONS_CONFIG);
    }

    public boolean namedCapturesOnly() {
        return this.getBoolean(GROK_ROW_NAMED_CAPTURES_ONLY_CONFIG);
    }

    public Collection<File> patternsDir() {
        return this.getList(GROK_ROW_PATTERNS_DIR_CONFIG)
                .stream()
                .map(File::new)
                .collect(Collectors.toList());
    }

    public static ConfigDef configDef() {
        return new ConfigDef()
        .define(GROK_ROW_PATTERN_CONFIG, ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH, GROK_ROW_PATTERN_DOC)

        .define(GROK_ROW_NAMED_CAPTURES_ONLY_CONFIG, ConfigDef.Type.BOOLEAN, true,
                ConfigDef.Importance.MEDIUM, GROK_ROW_NAMED_CAPTURES_ONLY_DOC)

        .define(GROK_ROW_PATTERNS_DIR_CONFIG, ConfigDef.Type.LIST, Collections.emptyList(),
                ConfigDef.Importance.MEDIUM, GROK_ROW_PATTERNS_DIR_DOC)

        .define(GROK_ROW_PATTERN_DEFINITIONS_CONFIG, ConfigDef.Type.LIST, Collections.emptyList(),
                ConfigDef.Importance.MEDIUM, GROK_ROW_PATTERN_DEFINITIONS_DOC);
    }
}
