/*
 * Copyright (c) 2018 EditorConfig Gradle Plugin
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ec4j.gradle;

import org.ec4j.lint.api.Logger.LogLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CollectingLoggerTest {

    @Test
    void serialize() throws IOException {
        final CollectingLogger log = new CollectingLogger(LogLevel.TRACE);

        log.trace("trace trace\nstr{}", 1);
        log.debug("debug debug\rstr{}", 2);
        log.info("info info\r\nstr{}", 3);
        log.warn("warn warn str{}", 4);
        log.error("error error\tstr{}", 5);
        log.fail("fail\nfail\n");

        final String prefix = "bla bla\n";
        final StringBuilder out = new StringBuilder();
        out.append(prefix);
        log.serialize(out);

        final String serialized = out.toString();
        assertEquals(prefix //
                + "TRACE trace trace\\nstr1\n" //
                + "DEBUG debug debug\\rstr2\n" //
                + "INFO info info\\r\\nstr3\n" //
                + "WARN warn warn str4\n" //
                + "ERROR error error\tstr5\n" //
                + "FAILURE fail\\nfail\\n", //
                serialized);

        final CollectingLogger.LogMessages data = CollectingLogger.deserialize(prefix.length(), serialized);
        final List<Entry<String, String>> messages = data.getMessages();
        int i = 0;
        assertEquals(5, messages.size());
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("TRACE", "trace trace\nstr1"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("DEBUG", "debug debug\rstr2"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("INFO", "info info\r\nstr3"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("WARN", "warn warn str4"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("ERROR", "error error\tstr5"),
                messages.get(i++));

        final String failureMessage = data.getFailureMessage();
        assertEquals("fail\nfail\n", failureMessage);

    }

    @Test
    void serializeNoFailure() throws IOException {
        final CollectingLogger log = new CollectingLogger(LogLevel.TRACE);

        log.trace("trace trace\nstr{}", 1);
        log.debug("debug debug\rstr{}", 2);
        log.info("info info\r\nstr{}", 3);
        log.warn("warn warn str{}", 4);
        log.error("error error\tstr{}", 5);

        final String prefix = "bla bla\n";
        final StringBuilder out = new StringBuilder();
        out.append(prefix);
        log.serialize(out);

        final String serialized = out.toString();
        assertEquals(prefix //
                + "TRACE trace trace\\nstr1\n" //
                + "DEBUG debug debug\\rstr2\n" //
                + "INFO info info\\r\\nstr3\n" //
                + "WARN warn warn str4\n" //
                + "ERROR error error\tstr5\n", //
                serialized);

        final CollectingLogger.LogMessages data = CollectingLogger.deserialize(prefix.length(), serialized);
        final List<Entry<String, String>> messages = data.getMessages();
        int i = 0;
        assertEquals(5, messages.size());
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("TRACE", "trace trace\nstr1"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("DEBUG", "debug debug\rstr2"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("INFO", "info info\r\nstr3"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("WARN", "warn warn str4"),
                messages.get(i++));
        assertEquals(new AbstractMap.SimpleImmutableEntry<String, String>("ERROR", "error error\tstr5"),
                messages.get(i++));

        final String failureMessage = data.getFailureMessage();
        assertNull(failureMessage);

    }
}
