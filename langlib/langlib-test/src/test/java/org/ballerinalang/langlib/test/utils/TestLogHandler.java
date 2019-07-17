/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.test.utils;

import java.util.logging.ConsoleHandler;

/**
 * A Console Log Handler to log developer level logs during unit tests.
 *
 * @since 1.0
 */
public class TestLogHandler extends ConsoleHandler {
    // Log handler class to use in tests.
    // This does not need a body as the super class has all the functions we need.
    // Purpose of this is to create a new class as we need two log handlers, but cannot use the same log handler twice.
}
