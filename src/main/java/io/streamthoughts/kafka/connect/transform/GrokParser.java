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

import io.streamthoughts.kafka.connect.transform.data.Type;
import io.streamthoughts.kafka.connect.transform.pattern.GrokMatcher;
import io.streamthoughts.kafka.connect.transform.pattern.GrokPattern;
import io.streamthoughts.kafka.connect.transform.pattern.GrokSchemaBuilder;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrokParser {

    public static boolean parse(final byte[] input,
                                final GrokMatcher grok,
                                final GrokPatternListener listener) {
        final Regex regex = grok.regex();
        final Matcher matcher = regex.matcher(input);
        int result = matcher.search(0, input.length, Option.DEFAULT);

        if (result == -1) return false;

        final Region region = matcher.getEagerRegion();
        for (Iterator<NameEntry> entry = regex.namedBackrefIterator(); entry.hasNext(); ) {
            NameEntry e = entry.next();
            final String field = GrokSchemaBuilder.getStringFieldName(e);
            final GrokPattern pattern = grok.getGrokPattern(field);
            final Type type = pattern != null ? pattern.type() : Type.STRING;
            final List<Object> values = extractValuesForEntry(region, e, input, type);
            listener.onEachMatches(field, values, type);
        }
        return true;
    }

    private static List<Object> extractValuesForEntry(final Region region,
                                                      final NameEntry e,
                                                      final byte[] bytes,
                                                      final Type target) {
        final List<Object> values = new ArrayList<>(e.getBackRefs().length);
        for (int i = 0; i < e.getBackRefs().length; i++) {
            int capture = e.getBackRefs()[i];
            int begin = region.beg[capture];
            int end = region.end[capture];

            if (begin > -1 && end > -1) {
                Object value = new String(bytes, begin, end - begin, StandardCharsets.UTF_8);
                if (target != null) {
                    value = target.convert(value);
                }
                values.add(value);
            }
        }
        return values;
    }
}
