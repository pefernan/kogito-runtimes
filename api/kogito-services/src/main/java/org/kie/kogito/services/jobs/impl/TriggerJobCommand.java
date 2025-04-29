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
package org.kie.kogito.services.jobs.impl;

import java.util.Objects;
import java.util.Optional;

import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.Signal;
import org.kie.kogito.services.uow.UnitOfWorkExecutor;
import org.kie.kogito.timer.TimerInstance;
import org.kie.kogito.uow.UnitOfWorkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerJobCommand {

    private static Logger LOG = LoggerFactory.getLogger(TriggerJobCommand.class);

    private String processInstanceId;
    private String correlationId;
    private String timerId;
    private Integer limit;
    private Process<?> process;
    private UnitOfWorkManager uom;
    public static final String SIGNAL = "timerTriggered";

    /**
     * @param processInstanceId Identifier of the process instance to execute.
     * @param correlationId The correlation id of the job that is being executed.
     * @param timerId Identifier of the timer to execute.
     * @param limit The job execution limit, a value of 0 indicates the last execution.
     * @param process The Process to which the processInstanceId belongs.
     * @param uom The unit of work manager to produce the execution.
     */
    public TriggerJobCommand(String processInstanceId, String correlationId, String timerId, Integer limit, Process<?> process, UnitOfWorkManager uom) {
        this.processInstanceId = processInstanceId;
        this.correlationId = correlationId;
        this.timerId = timerId;
        this.limit = limit;
        this.process = process;
        this.uom = uom;
    }

    public boolean execute() {
        return UnitOfWorkExecutor.executeInUnitOfWork(uom, () -> {
            logExecute("BEFORE", processInstanceId);
            Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances().findById(processInstanceId);

            boolean result = processInstanceFound.map(processInstance -> {
                processInstance.send(new JobSignal(SIGNAL, TimerInstance.with(correlationId, timerId, limit)));
                return true;
            }).orElse(false);

            logExecute("AFTER", processInstanceId);
            return result;
        });
    }

    private void logExecute(String action, String processInstanceId) {
        LOG.debug("{} JOB COMMAND EXECUTE: ", action, processInstanceId);
    }

    private class JobSignal implements Signal<Object> {
        private String signal;
        private Object payload;

        public JobSignal(String signal, Object payload) {
            this.signal = signal;
            this.payload = payload;
        }

        @Override
        public String channel() {
            return this.signal;
        }

        @Override
        public Object payload() {
            return payload;
        }

        @Override
        public String referenceId() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof JobSignal)) {
                return false;
            }
            JobSignal jobSignal = (JobSignal) o;
            return Objects.equals(signal, jobSignal.signal) &&
                    Objects.equals(payload, jobSignal.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(signal, payload);
        }
    }
}
