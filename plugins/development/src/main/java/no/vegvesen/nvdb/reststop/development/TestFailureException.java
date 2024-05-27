/*
 * Copyright 2018 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.vegvesen.nvdb.reststop.development;

import org.junit.runner.notification.Failure;

import java.util.List;

/**
 *
 */
public class TestFailureException extends RuntimeException {
    private final List<Failure> failures;

    public TestFailureException(List<Failure> failures) {
        this.failures = failures;
    }

    public List<Failure> getFailures() {
        return failures;
    }
}
