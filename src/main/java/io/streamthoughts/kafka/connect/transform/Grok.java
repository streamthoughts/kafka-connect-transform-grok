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
import io.streamthoughts.kafka.connect.transform.pattern.GrokSchemaBuilder;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class Grok<R extends ConnectRecord<R>> implements Transformation<R> {

    private GrokConfig config;
    private GrokPatternCompiler compiler;
    private List<GrokMatcher> patterns;
    private Schema schema;


    protected SchemaAndValue process(final Schema inputSchema, final Object inputValue) {
        if (inputSchema == null && inputValue == null) {
            return new SchemaAndValue(null, null);
        }

        if (Schema.Type.STRING == inputSchema.type()) {
            final byte[] bytes = ((String)inputValue).getBytes(StandardCharsets.UTF_8);
            final Struct struct = new Struct(schema);

            for (GrokMatcher matcher : patterns) {
                final boolean matched = GrokParser.parse(bytes, matcher, (field, values, type) -> {
                    final Schema.Type fieldType = schema.field(field).schema().type();
                    if (fieldType == Schema.Type.ARRAY)
                        struct.put(field, values);
                    else
                        struct.put(field, values.get(0));
                });

                if (matched) {
                    return new SchemaAndValue(schema, struct);
                }
            }
            throw new GrokException("Can not matches grok pattern on value : " + inputValue);
        }

        throw new UnsupportedOperationException(inputSchema.type() + " is not a supported type.");
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
        patterns = Collections.singletonList(compiler.compile(config.pattern()));
        schema = GrokSchemaBuilder.buildSchemaForGrok(patterns);
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
}
