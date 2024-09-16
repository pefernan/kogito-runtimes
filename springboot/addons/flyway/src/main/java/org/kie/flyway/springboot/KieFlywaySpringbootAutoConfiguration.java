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

import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.kie.flyway.KieFlywayException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = { DataSourceAutoConfiguration.class })
@ConditionalOnProperty(prefix = "kie.flyway", name = "enabled", havingValue = "true")
@ConditionalOnClass(Flyway.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@EnableConfigurationProperties(KieFlywayProperties.class)
public class KieFlywaySpringbootAutoConfiguration {

    @Bean
    public KieFlywaySpringbootInitializer kieFlyway(KieFlywayProperties properties, ObjectProvider<DataSource> dataSource, Map<String, DataSource> datasources) {

        if (!properties.isEnabled()) {
            throw new KieFlywayException("Kie Flyway migrations are disabled, we shouldn't be here");
        }

        DataSource ds = datasources.get(properties.getDataSource());

        if (Objects.isNull(ds)) {
            throw new KieFlywayException("Couldn't find datasource `" + properties.getDataSource() + "`");
        }

        return new KieFlywaySpringbootInitializer(ds);
    }

}
