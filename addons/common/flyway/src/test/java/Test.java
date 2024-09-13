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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.stream.Stream;

import org.kie.flyway.KieFlywayConstants;
import org.kie.flyway.model.KieFlywayModuleConfig;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

public class Test {
    public static void main(String[] args) {

        try {
            Stream<URL> resources = Test.class.getClassLoader().resources(KieFlywayConstants.KIE_FLYWAY_DESCRIPTOR_FILE_NAME);
            JavaPropsMapper mapper = new JavaPropsMapper();

            resources.forEach(url -> {
                Properties properties = new Properties();
                try {
                    properties.load(new FileInputStream(url.getFile()));

                    KieFlywayModuleConfig module = mapper.readPropertiesAs(properties, KieFlywayModuleConfig.class);
                    System.out.println(module);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(url);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
