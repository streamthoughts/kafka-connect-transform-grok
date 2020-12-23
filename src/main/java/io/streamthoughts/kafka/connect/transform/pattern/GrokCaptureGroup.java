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
import org.joni.Region;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class GrokCaptureGroup {

    private final Type type;
    private final String name;
    private final int[] backRefs;

    public GrokCaptureGroup(final String name, final int[] backRefs, final Type type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.backRefs = backRefs;
    }

    /**
     * Gets the type defined for the data field to capture.
     */
    public Type type() {
        return type;
    }

    /**
     * Gets the name defined for the data field to capture.
     */
    public String name() {
        return name;
    }

    /**
     * Gets the {@link GrokCaptureExtractor} to be used for capturing that group.
     *
     * @param consumer  the {@link Consumer} to call when a data field is captured.
     */
    public GrokCaptureExtractor getExtractor(final Consumer<Object> consumer) {
        return new RawValueExtractor(backRefs, (s -> consumer.accept(type.convert(s))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GrokCaptureGroup{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", backRefs=" + Arrays.toString(backRefs) +
                '}';
    }

    private static class RawValueExtractor implements GrokCaptureExtractor {

        final int[] backRefs;
        final Consumer<String> consumer;

        public RawValueExtractor(final int[] backRefs, final Consumer<String> consumer) {
            this.backRefs = backRefs;
            this.consumer = consumer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void extract(byte[] bytes, Region region) {
            for (int capture : backRefs) {
                int offset = region.beg[capture];
                int length = region.end[capture] - region.beg[capture];
                if (offset >= 0) {
                    String value = new String(bytes, offset, length, StandardCharsets.UTF_8);
                    consumer.accept(value);
                    break; // we only need to capture the first value.
                }
            }
        }
    }
}
