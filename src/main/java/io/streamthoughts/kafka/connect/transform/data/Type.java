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
package io.streamthoughts.kafka.connect.transform.data;

import io.streamthoughts.kafka.connect.transform.data.internal.TypeConverter;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Type {

    // This is a special type used to deal with NULL object.
    NULL(null, null) {
        @Override
        public Short convert(final Object o) {
            throw new UnsupportedOperationException("Cannot convert an object to type NULL");
        }
        @Override
        protected boolean isInternal() {
            return true;
        }
    },

    SHORT(Collections.singletonList(Short.class), Schema.Type.INT16) {
        @Override
        public Short convert(final Object o) {
            return TypeConverter.getShort(o);
        }
    },

    INTEGER(Collections.singletonList(Integer.class), Schema.Type.INT32) {

        @Override
        public Integer convert(final Object o) {
            return TypeConverter.getInt(o);
        }
    },

    LONG(Collections.singletonList(Long.class), Schema.Type.INT64) {
        @Override
        public Long convert(final Object o) {
            return TypeConverter.getLong(o);
        }
    },

    FLOAT(Collections.singletonList(Float.class), Schema.Type.FLOAT32) {
        @Override
        public Float convert(final Object o) {
            return TypeConverter.getFloat(o);
        }
    },

    DOUBLE(Collections.singletonList(Double.class), Schema.Type.FLOAT64) {
        @Override
        public Double convert(final Object o) {
            return TypeConverter.getDouble(o);
        }
    },

    BOOLEAN(Collections.singletonList(Boolean.class), Schema.Type.BOOLEAN) {
        @Override
        public Boolean convert(final Object o) {
            return TypeConverter.getBool(o);
        }
    },

    STRING(Collections.singletonList(String.class), Schema.Type.STRING) {
        @Override
        public String convert(final Object o) {
            return TypeConverter.getString(o);
        }
    },

    ARRAY(Collections.singletonList(Collection.class), Schema.Type.ARRAY) {
        @Override
        public Collection convert(final Object o) {
            return TypeConverter.getArray(o);
        }

    },

    BYTES(Collections.emptyList(), Schema.Type.BYTES) {
        @Override
        public byte[] convert(Object o) {
            return TypeConverter.getBytes(o);
        }
    };
    
    private final static Map<Class<?>, Type> JAVA_CLASS_TYPES = new HashMap<>();

    static {
        for (Type type : Type.values()) {
            if (!type.isInternal()) {
                Collection<Class<?>> typeClasses = type.classes;
                for (Class<?> typeClass : typeClasses) {
                    JAVA_CLASS_TYPES.put(typeClass, type);
                }
            }
        }
    }

    private Schema.Type schemaType;

    private Collection<Class<?>> classes;

    /**
     * Creates a new {@link Type} instance.
     *
     * @param schemaType the connect schema type.
     */
    Type(final Collection<Class<?>> classes, final Schema.Type schemaType) {
        this.classes = classes;
        this.schemaType = schemaType;
    }

    /**
     * Returns the {@link Schema.Type} instance for this {@link Type}.
     *
     * @return a {@link Schema.Type} instance.
     */
    public Schema.Type schemaType() {
        return schemaType;
    }

    /**
     * Converts the specified object to this type.
     *
     * @param o the object to be converted.
     * @return  the converted object.
     */
    public abstract Object convert(final Object o) ;

    /**
     * Checks whether this is type is internal.
     * Internal types cannot be resolved from a class or string name.
     *
     * @return {@code false}.
     */
    protected boolean isInternal() {
        return false;
    }

    public boolean isPrimitive() {

        switch (this) {
            case FLOAT:
            case DOUBLE:
            case INTEGER:
            case LONG:
            case BOOLEAN:
            case STRING:
            case BYTES:
                return true;
            default:
        }
        return false;
    }

    public static Type forName(final String name) {
        for (Type type : Type.values()) {
            if (!type.isInternal()
                && (type.name().equals(name) || type.schemaType.name().equals(name))) {
                return type;
            }
        }
        throw new DataException("Cannot find corresponding Type for name : " + name);
    }
}
