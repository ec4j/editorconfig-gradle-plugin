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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ec4j.gradle.runtime.EditorconfigInvoker;
import org.ec4j.lint.api.Logger;

/**
 * A {@link Logger} that collects the log messages in a {@link #messages} {@link List}. This class exists because the
 * SLF4J loggers instantiated by the isolated class loader inside {@link EditorconfigInvoker} do not obey the log level
 * set via Gradle CLI. Thus we collect the messages in a {@link List}, serialize them to {@link String} (see
 * {@link #serialize(Appendable)}), send them via and exception message to the caller context where we
 * {@link #deserialize(int, String)} them to {@link LogMessages}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 0.0.3
 */
public class CollectingLogger extends Logger.AbstractLogger {

    /**
     * An immutable {@link List} of log {@link #messages}, optionally with a {@link #failureMessage}.
     */
    public static class LogMessages {

        private final String failureMessage;
        private final List<Map.Entry<String, String>> messages;

        LogMessages(List<Entry<String, String>> messages, String failureMessage) {
            super();
            this.messages = messages;
            this.failureMessage = failureMessage;
        }

        /**
         * @return the failure message, can be {@code null} if the underlying task did not fail.
         */
        public String getFailureMessage() {
            return failureMessage;
        }

        /**
         * @return an unmodifiable {@link List} of log message {@link Entry}s. Keys are {@link LogLevel}s and values are
         *         the messages.
         */
        public List<Map.Entry<String, String>> getMessages() {
            return messages;
        }
    }

    /**
     * Parses the output of {@link CollectingLogger#serialize(Appendable)} to {@link LogMessages}.
     */
    static class Parser {

        private static int unescape(int offset, String in, StringBuilder out) {
            int i = offset;
            while (i < in.length()) {
                char ch = in.charAt(i++);
                switch (ch) {
                case '\n':
                    return i;
                case '\\':
                    char ch1 = in.charAt(i++);
                    switch (ch1) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    default:
                        throw new IllegalStateException("Unecpxpected escape sequence [\\" + ch1 + "]");
                    }
                    break;
                default:
                    out.append(ch);
                }
            }
            return i;
        }

        private String failureMessage = null;

        private List<Map.Entry<String, String>> messages = new ArrayList<>();

        Parser() {
        }

        private int entry(int offset, String in) {
            final int spaceOffset = in.indexOf(' ', offset);
            if (spaceOffset >= 0) {
                final String key = in.substring(offset, spaceOffset);
                final StringBuilder value = new StringBuilder();
                final int lfOffset = unescape(spaceOffset + 1, in, value);
                if (FAILURE.equals(key)) {
                    if (this.failureMessage != null) {
                        throw new IllegalStateException("Only one " + FAILURE + " expected");
                    }
                    this.failureMessage = value.toString();
                } else {
                    messages.add(new AbstractMap.SimpleImmutableEntry<>(key, value.toString()));
                }
                return lfOffset;
            } else {
                throw new IllegalStateException("Expected a key");
            }
        }

        LogMessages parse(int offset, String in) {
            while ((offset = entry(offset, in)) < in.length()) {
                /* empty */
            }
            List<Entry<String, String>> m = this.messages;
            this.messages = null;
            return new LogMessages(Collections.unmodifiableList(m), failureMessage);
        }
    }

    private static final String FAILURE = "FAILURE";

    /**
     * Parse the {@code input} starting at {@code offset} to {@link LogMessages}.
     *
     * @param offset the zero based index to start at the parsing in {@code input}
     * @param input the string to parse
     * @return new {@link LogMessages}
     */
    public static LogMessages deserialize(int offset, String input) {
        return new Parser().parse(offset, input);
    }

    private static void escape(String string, Appendable out) throws IOException {
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
            case '\n':
                out.append("\\n");
                break;
            case '\r':
                out.append("\\r");
                break;
            case '\\':
                out.append("\\\\");
                break;
            default:
                out.append(ch);
            }
        }
    }

    private volatile String failureMessage;

    private final List<Map.Entry<String, String>> messages = new ArrayList<>();

    public CollectingLogger(LogLevel level) {
        super(level);
    }

    /**
     * Sets the {@link #failureMessage}
     *
     * @param failureMessage the failure message
     */
    public void fail(String failureMessage) {
        if (this.failureMessage != null) {
            throw new IllegalStateException();
        }
        this.failureMessage = failureMessage;
    }

    /** {@inheritDoc} */
    @Override
    public void log(LogLevel level, String string, Object... args) {
        messages.add(new AbstractMap.SimpleImmutableEntry<String, String>(level.name(),
                Slf4jFormatter.format(string, args)));
    }

    /**
     * Append the {@link #messages} and {@link #failureMessage} to the given {@code output}.
     *
     * @param output the {@link Appendable} to append to
     * @throws IOException thrown from {@link Appendable#append(CharSequence)}
     */
    public void serialize(Appendable output) throws IOException {
        for (Map.Entry<String, String> entry : messages) {
            output.append(entry.getKey()).append(' ');
            escape(entry.getValue(), output);
            output.append('\n');
        }
        if (failureMessage != null) {
            output.append(FAILURE).append(' ');
            escape(failureMessage, output);
        }
    }

}
