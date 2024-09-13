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

package org.kie.flyway.springboot;

import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.kie.flyway.KieFlywayException;
import org.kie.flyway.KieFlywayInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.support.JdbcUtils;

public class KieFlywaySpringbootInitializer implements InitializingBean, Ordered {
    private static final Logger log = LoggerFactory.getLogger(KieFlywaySpringbootInitializer.class);

    private int order = 0;

    private final DataSource dataSource;

    public KieFlywaySpringbootInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        if (dataSource != null) {
            String dbType = getDataSourceType();
            KieFlywayInitializer.Builder.get()
                    .withClassLoader(Thread.currentThread().getContextClassLoader())
                    .withDatasource(dataSource)
                    .withDbType(dbType)
                    .build()
                    .migrate();
        }
    }

    private String getDataSourceType() {
        try {
            return JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        } catch (Exception e) {
            log.error("Couldn't extract database product name from datasource: ", e);
            throw new KieFlywayException("Couldn't extract database product name from datasource", e);
        }
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
