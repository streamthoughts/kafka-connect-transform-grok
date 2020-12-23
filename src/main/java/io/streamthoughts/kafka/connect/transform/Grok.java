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

import io.streamthoughts.kafka.connect.transform.pattern.GrokException;
import io.streamthoughts.kafka.connect.transform.pattern.GrokMatcher;
import io.streamthoughts.kafka.connect.transform.pattern.GrokPatternCompiler;
import io.streamthoughts.kafka.connect.transform.pattern.GrokPatternResolver;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Grok<R extends ConnectRecord<R>> implements Transformation<R> {

    private GrokPatternCompiler compiler;
    private List<GrokMatcher> matchPatterns;
    private GrokConfig config;

    protected SchemaAndValue process(final Schema inputSchema, final Object inputValue) {
        if (inputSchema == null && inputValue == null) {
            return new SchemaAndValue(null, null);
        }

        if (Schema.Type.STRING != inputSchema.type()) {
            throw new UnsupportedOperationException(inputSchema.type() + " is not a supported type.");
        }

        final byte[] bytes = ((String)inputValue).getBytes(StandardCharsets.UTF_8);

        List<SchemaAndNamedCaptured> allNamedCaptured = new ArrayList<>(matchPatterns.size());
        for (GrokMatcher matcher : matchPatterns) {
            final Map<String, Object> captured = matcher.captures(bytes);
            if (captured != null) {
                allNamedCaptured.add(new SchemaAndNamedCaptured(matcher.schema(), captured));
                if (config.breakOnFirstPattern()) break;
            }
        }

        if (allNamedCaptured.isEmpty()) {
            throw new GrokException("Supplied Grok patterns does not match input data: " + inputSchema);
        }

        final Schema schema = mergeToSchema(allNamedCaptured);
        return new SchemaAndValue(schema, mergeToStruct(allNamedCaptured, schema));
    }

    @SuppressWarnings("unchecked")
    private Struct mergeToStruct(final List<SchemaAndNamedCaptured> allNamedCaptured, final Schema schema) {
        final Map<String, Object> fields = new HashMap<>();
        for (SchemaAndNamedCaptured schemaAndNamedCaptured : allNamedCaptured) {
            schemaAndNamedCaptured.namedCaptured().forEach((name, value) -> {
                final Field field = schema.field(name);
                if (field.schema().type() == Schema.Type.ARRAY)
                    ((List<Object>)fields.computeIfAbsent(name, k -> new ArrayList<>())).add(value);
                else
                    fields.put(name, value);
            });
        }
        final Struct struct = new Struct(schema);
        fields.forEach(struct::put);
        return struct;
    }

    private Schema mergeToSchema(final List<SchemaAndNamedCaptured> allNamedCaptured) {
        if (allNamedCaptured.size() == 1) return allNamedCaptured.get(0).schema();

        final Map<String, Schema> fields = new HashMap<>();
        for (SchemaAndNamedCaptured namedCaptured : allNamedCaptured) {
            final Schema schema = namedCaptured.schema();
            schema.fields().forEach(f -> {
                final Schema fieldSchema = fields.containsKey(f.name()) ? SchemaBuilder.array(f.schema()) : f.schema();
                fields.put(f.name(), fieldSchema);
            });
        }
        SchemaBuilder schema = SchemaBuilder.struct();
        fields.forEach(schema::field);
        return schema.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigDef config() {
        return GrokConfig.configDef();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> props) {
        config = new GrokConfig(props);
        compiler = new GrokPatternCompiler(
            new GrokPatternResolver(
                config.patternDefinitions(),
                config.patternsDir()),
                config.namedCapturesOnly()
        );
        matchPatterns = config.patterns()
            .stream()
            .map(pattern -> compiler.compile(pattern))
            .collect(Collectors.toList());
    }

    public static class Key<R extends ConnectRecord<R>> extends Grok<R> {

        /**
         * {@inheritDoc}
         */
        @Override
        public R apply(R r) {
            final SchemaAndValue transformed = process(r.keySchema(), r.key());
            return r.newRecord(
                r.topic(),
                r.kafkaPartition(),
                transformed.schema(),
                transformed.value(),
                r.valueSchema(),
                r.value(),
                r.timestamp()
            );
        }
    }

    public static class Value<R extends ConnectRecord<R>> extends Grok<R> {

        /**
         * {@inheritDoc}
         */
        @Override
        public R apply(R r) {
            final SchemaAndValue transformed = process(r.valueSchema(), r.value());
            return r.newRecord(
                r.topic(),
                r.kafkaPartition(),
                r.keySchema(),
                r.key(),
                transformed.schema(),
                transformed.value(),
                r.timestamp()
            );
        }
    }

    private static class SchemaAndNamedCaptured {
        private final Schema schema;
        private final Map<String, Object> namedCaptured;

        public SchemaAndNamedCaptured(final Schema schema,
                                      final Map<String, Object> namedCaptured) {
            this.schema = schema;
            this.namedCaptured = namedCaptured;
        }

        public Schema schema() {
            return schema;
        }

        public Map<String, Object> namedCaptured() {
            return namedCaptured;
        }
    }
}
