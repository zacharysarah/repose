/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.servlet.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA. User: joshualockwood Date: May 19, 2011 Time: 10:38:16 AM
 */
@RunWith(Enclosed.class)
public class MutableHttpServletRequestTest {

    private static Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length);

        namesCollection.addAll(Arrays.asList(names));

        return namesCollection.elements();
    }

    public static class WhenCreatingNewInstances {

        private HttpServletRequest originalRequest;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {
            headerNames = createStringEnumeration("accept", "ACCEPT-ENCODING");
            headerValues1 = createStringEnumeration("val1.1", "val1.2");
            headerValues2 = createStringEnumeration("val2.1");

            originalRequest = mock(HttpServletRequest.class);

            when(originalRequest.getHeaderNames()).thenReturn(headerNames);
            when(originalRequest.getHeaders(argThat(equalToIgnoringCase("accept")))).thenReturn(headerValues1);
            when(originalRequest.getHeaders(argThat(equalToIgnoringCase("accept-encoding")))).thenReturn(headerValues2);

            wrappedRequest = MutableHttpServletRequest.wrap(originalRequest);
        }

        @Test
        public void shouldMapExpectedNumberOfHeaders() {
            Integer expected, actual = 0;
            expected = 2;

            Enumeration<String> headerNames = wrappedRequest.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(expected, actual);
        }

        @Test
        public void shouldMapHeaderNamesAsLowerCase() {
            Integer expected, actual = 0;
            expected = 1;

            Enumeration<String> headerNames = wrappedRequest.getHeaders("accept-encoding");

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(expected, actual);
        }

        @Test
        public void shouldMapHeaderNamesAndValues() {
            assertEquals("val1.1", wrappedRequest.getHeader("accept"));
        }

        @Test
        public void shouldCreateNewInstanceIfIsNotWrapperInstance() {
            HttpServletRequest original = originalRequest;
            MutableHttpServletRequest actual;

            actual = MutableHttpServletRequest.wrap(original);

            assertNotSame(original, actual);
        }

        @Test
        public void shouldRemoveMapHeaderNamesAndValues() {
            wrappedRequest.removeHeader("accept");
            assertThat(wrappedRequest.getHeader("accept"), equalTo(null));
        }

        @Test
        public void shouldClearMapHeaderNamesAndValues() {
            wrappedRequest.clearHeaders();
            assertThat(wrappedRequest.getHeader("accept"), equalTo(null));
            assertThat(wrappedRequest.getHeader("accept-encoding"), equalTo(null));
        }

        @Test
        public void shouldReplaceMapHeaderNamesAndValues() {
            String expected = "val3.1";
            wrappedRequest.replaceHeader("accept", expected);
            assertThat(wrappedRequest.getHeader("accept"), equalTo(expected));
        }

        @Test
        public void shouldAddMapHeaderNamesAndValues() {
            String expected = "val3.1";
            wrappedRequest.addHeader("header3", expected);
            assertThat(wrappedRequest.getHeader("header3"), equalTo(expected));
        }
    }

    public static class WhenGettingHeaderValuesFromMap {

        private List<String> headerValues;
        private Map<HeaderName, List<String>> headers;

        @Before
        public void setup() {
            headerValues = new ArrayList<String>();
            headerValues.add("val1");
            headerValues.add("val2");
            headerValues.add("val3");

            headers = new HashMap<HeaderName, List<String>>();
            headers.put(HeaderName.wrap("accept"), headerValues);
            headers.put(HeaderName.wrap("ACCEPT-ENCODING"), new ArrayList<String>());
        }

        @Test
        public void shouldReturnFirstElementInMatchingHeader() {
            String expected, actual;

            expected = headerValues.get(0);
            actual = HeaderValuesImpl.fromMap(headers, "accept");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnNullIfNotFound() {
            assertNull(HeaderValuesImpl.fromMap(headers, "headerZ"));
        }

        @Test
        public void shouldReturnNullHeadersCollectionIsEmpty() {
            assertNull(HeaderValuesImpl.fromMap(headers, "ACCEPT-ENCODING"));
        }
    }

    public static class WhenGettingEntityLength {

        private HttpServletRequest request;
        private MutableHttpServletRequest wrappedRequest;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private String msg = "This is my test entity";

        @Before
        public void setup() throws IOException {
            request = mock(HttpServletRequest.class);

            headerNames = createStringEnumeration("content-length");
            headerValues1 = createStringEnumeration("2");

            when(request.getHeaders("content-length")).thenReturn(headerValues1);
            when(request.getHeader("content-length")).thenReturn("2");
        }

        @Test
        public void shouldReturnActualLengthOfEntity() throws IOException {

            when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(msg.getBytes())));
            wrappedRequest = MutableHttpServletRequest.wrap(request);

            final int realEntitySize = wrappedRequest.getRealBodyLength();

            assertEquals("Real entity length should reflect what is in the inputstream", realEntitySize, msg.length());
            assertFalse("Real entity length should not match content-length", String.valueOf(realEntitySize).equals(request.getHeader("content-length")));
        }

        @Test
        public void shouldNotAlterMessageWhenRetrievingActualEntityLength() throws IOException {

            when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(msg.getBytes())));
            wrappedRequest = MutableHttpServletRequest.wrap(request);
            final int realEntitySize = wrappedRequest.getRealBodyLength();
            ServletInputStream is = wrappedRequest.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            final byte[] internalBuffer = new byte[1024];

            long total = 0;
            int read;

            while ((read = is.read(internalBuffer)) != -1) {
                os.write(internalBuffer, 0, read);
                total += read;
            }

            String newMsg = new String(os.toByteArray());

            assertEquals("Retrieving size of message should not alter message", msg, newMsg);

        }

        @Test
        public void shouldReturn0OnEmptyBody() throws IOException {

            String empty = "";
            ServletInputStream in = new ServletInputStreamWrapper(new ByteArrayInputStream(empty.getBytes()));

            when(request.getInputStream()).thenReturn(in);
            wrappedRequest = MutableHttpServletRequest.wrap(request);

            final int realEntitySize = wrappedRequest.getRealBodyLength();
            assertTrue("Should return 0 on empty body", realEntitySize == 0);

        }
    }

    public static class WhenDealingWithNonSplittableHeaders {

        private HttpServletRequest request;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private Enumeration<String> headerValues3;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {

            headerNames = createStringEnumeration("header1", "header2", "header3");

            headerValues1 = createStringEnumeration("val1", "val2", "val3");
            headerValues2 = createStringEnumeration("val4");
            headerValues3 = createStringEnumeration("val5,val6,val7");


            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("header1")).thenReturn(headerValues1);
            when(request.getHeaders("header2")).thenReturn(headerValues2);
            when(request.getHeaders("header3")).thenReturn(headerValues3);

            wrappedRequest = MutableHttpServletRequest.wrap(request);
        }

        @Test
        public void shouldNotSplitHeaders() {

            Integer expected, actual = 0;
            expected = 1;

            Enumeration<String> headerNames = wrappedRequest.getHeaders("header3");

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(actual, expected);

        }

    }

    public static class WhenDealingWithContentType {

        private MutableHttpServletRequest mutableHttpServletRequest;
        private HttpServletRequest request;

        @Before
        public void setup() throws IOException {
            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(createStringEnumeration(HttpHeaders.CONTENT_TYPE));
            when(request.getHeaders(HttpHeaders.CONTENT_TYPE)).thenReturn(createStringEnumeration("text/plain"));

            mutableHttpServletRequest = MutableHttpServletRequest.wrap(request);
        }

        @Test
        public void shouldAccuratelyReflectChanges() {
            mutableHttpServletRequest.replaceHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            assertThat(mutableHttpServletRequest.getContentType(), equalTo("application/json"));
        }
    }
}
