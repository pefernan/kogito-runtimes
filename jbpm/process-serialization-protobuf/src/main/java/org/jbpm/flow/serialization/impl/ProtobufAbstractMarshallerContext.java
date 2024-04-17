/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jbpm.flow.serialization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import org.jbpm.flow.serialization.MarshallerContext;
import org.jbpm.flow.serialization.MarshallerContextName;
import org.jbpm.flow.serialization.ObjectMarshallerStrategy;
import org.jbpm.flow.serialization.ProcessInstanceMarshallerException;

import com.google.protobuf.Any;

public abstract class ProtobufAbstractMarshallerContext implements MarshallerContext {

    private Map<MarshallerContextName, Object> env;

    public ProtobufAbstractMarshallerContext() {
        this.env = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(MarshallerContextName<T> key) {
        return (T) env.get(key);
    }

    @Override
    public <T> void set(MarshallerContextName<T> key, T value) {
        env.put(key, value);
    }

    @Override
    public ObjectMarshallerStrategy findObjectMarshallerStrategyFor(Object value) {
        return findMarshaller(value, (s, v) -> s.acceptForMarshalling(v));
    }

    @Override
    public ObjectMarshallerStrategy findObjectUnmarshallerStrategyFor(Any value) {
        return findMarshaller(value, (s, v) -> s.acceptForUnmarshalling((Any) v));
    }

    private ObjectMarshallerStrategy findMarshaller(Object value, BiPredicate<ObjectMarshallerStrategy, Object> type) {
        ObjectMarshallerStrategy[] objectMarshallerStrategies = get(MarshallerContextName.OBJECT_MARSHALLING_STRATEGIES);

        for (ObjectMarshallerStrategy current : objectMarshallerStrategies) {
            if (type.test(current, value)) {
                return current;
            }
        }

        // we cannot persist the data
        throw new ProcessInstanceMarshallerException("No marshaller found for class " + value.getClass().getName());
    }
}