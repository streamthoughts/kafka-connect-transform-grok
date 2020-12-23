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

import io.streamthoughts.kafka.connect.transform.data.Type;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GrokMatcher {

    private final Map<String, GrokPattern> patternsByName;

    private final List<GrokPattern> patterns;

    private final String expression;

    private final Regex regex;

    private final List<GrokCaptureGroup> grokCaptureGroups;

    private final Schema schema;

    /**
     * Creates a new {@link GrokMatcher} instance.
     *
     * @param patterns    the list of patterns.
     * @param expression  the original expression.
     */
    GrokMatcher(final List<GrokPattern> patterns,
                final String expression) {
        Objects.requireNonNull(patterns, "pattern can't be null");
        Objects.requireNonNull(expression, "expression can't be null");
        this.patterns = patterns;
        this.expression = expression;
        this.patternsByName = patterns
            .stream()
            .collect(Collectors.toMap(GrokPattern::syntax, p -> p,  (p1, p2) -> p1.semantic() != null ? p1 : p2));
        byte[] bytes = expression.getBytes(StandardCharsets.UTF_8);
        regex = new Regex(bytes, 0, bytes.length, Option.NONE, UTF8Encoding.INSTANCE);

        grokCaptureGroups = new ArrayList<>();
        for (Iterator<NameEntry> entry = regex.namedBackrefIterator(); entry.hasNext();) {
            NameEntry nameEntry = entry.next();
            final String field = new String(
                nameEntry.name,
                nameEntry.nameP,
                nameEntry.nameEnd - nameEntry.nameP,
                StandardCharsets.UTF_8);
            final GrokPattern pattern = getGrokPattern(field);
            final Type type = pattern != null ? pattern.type() : Type.STRING;
            grokCaptureGroups.add(new GrokCaptureGroup(field, nameEntry.getBackRefs(), type));
        }

        final SchemaBuilder builder = SchemaBuilder.struct();
        for (GrokCaptureGroup group : grokCaptureGroups) {
            final Schema fieldSchema = new SchemaBuilder(group.type().schemaType()).optional().defaultValue(null).build();
            builder.field(group.name(), fieldSchema);
        }
        schema = builder.build();
    }

    public Schema schema() {
        return schema;
    }

    public GrokPattern getGrokPattern(final int i) {
        return patterns.get(i);
    }

    public GrokPattern getGrokPattern(final String name) {
        return patternsByName.get(name);
    }

    /**
     * Returns the compiled regex expression.
     */
    public Regex regex() {
        return regex;
    }

    /**
     * Returns the raw regex expression.
     */
    public String expression() {
        return expression;
    }

    /**
     *
     * @param bytes the text bytes to match.
     * @return      a {@code Map} that contains all named captured.
     */
    public Map<String, Object> captures(final byte[] bytes) {

        long now = Time.SYSTEM.milliseconds();
        final var extractor = new GrokCaptureExtractor.MapGrokCaptureExtractor(grokCaptureGroups);

        final Matcher matcher = regex.matcher(bytes);
        int result = matcher.search(0, bytes.length, Option.DEFAULT);

        if (result == Matcher.FAILED) {
            return null;
        }
        if (result == Matcher.INTERRUPTED) {
            long interruptedAfterMs = Time.SYSTEM.milliseconds() - now;
            throw new RuntimeException("Grok pattern matching was interrupted before completion (" + interruptedAfterMs + " ms)");
        }
        extractor.extract(bytes, matcher.getEagerRegion());

        return extractor.captured();
    }

    @Override
    public String toString() {
        return "GrokMatcher{" +
                "patterns=" + patterns +
                ", expression='" + expression + '\'' +
                '}';
    }
}
