/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jsecurity.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsecurity.SecurityUtils;
import org.jsecurity.session.Session;
import org.jsecurity.subject.Subject;
import org.jsecurity.util.StringUtils;
import org.jsecurity.util.ThreadContext;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Simple utility class for operations used across multiple class hierarchies in the web framework code.
 *
 * <p>Some methods in this class were copied from the Spring Framework so we didn't have to re-invent the wheel,
 * and in these cases, we have retained all license, copyright and author information.
 *
 * @author Les Hazlewood
 * @author Jeremy Haile
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 0.9
 */
public class WebUtils {

    private static final Log log = LogFactory.getLog(WebUtils.class);


    /**
     * Message displayed when a servlet request or response is not bound to the current thread context when expected.
     */
    private static final String NOT_BOUND_ERROR_MESSAGE =
            "Make sure WebUtils.bind() is being called. (typically called by JSecurityFilter)  " +
                    "This could also happen when running integration tests that don't properly call WebUtils.bind().";

    public static final String SERVLET_REQUEST_KEY = ServletRequest.class.getName() + "_JSECURITY_THREAD_CONTEXT_KEY";
    public static final String SERVLET_RESPONSE_KEY = ServletResponse.class.getName() + "_JSECURITY_THREAD_CONTEXT_KEY";

    /**
     * {@link Session Session} key used to save a request and later restore it, for example when redirecting to a
     * requested page after login, equal to <code>jsecuritySavedRequest</code>.
     */
    public static final String SAVED_REQUEST_KEY = "jsecuritySavedRequest";


    /**
     * Standard Servlet 2.3+ spec request attributes for include URI and paths.
     * <p>If included via a RequestDispatcher, the current resource will see the
     * originating request. Its own URI and paths are exposed as request attributes.
     */
    public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";
    public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";
    public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";
    public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";
    public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";

    /**
     * Standard Servlet 2.4+ spec request attributes for forward URI and paths.
     * <p>If forwarded to via a RequestDispatcher, the current resource will see its
     * own URI and paths. The originating URI and paths are exposed as request attributes.
     */
    public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";
    public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";
    public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";
    public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";
    public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

    /**
     * Default character encoding to use when <code>request.getCharacterEncoding</code>
     * returns <code>null</code>, according to the Servlet spec.
     *
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     */
    public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

    /**
     * Return the path within the web application for the given request.
     * <p>Detects include request URL if called within a RequestDispatcher include.
     * <p/>
     * For example, for a request to URL
     * <p/>
     * <code>http://www.somehost.com/myapp/my/url.jsp</code>,
     * <p/>
     * for an application deployed to <code>/mayapp</code> (the application's context path), this method would return
     * <p/>
     * <code>/my/url.jsp</code>.
     *
     * @param request current HTTP request
     * @return the path within the web application
     */
    public static String getPathWithinApplication(HttpServletRequest request) {
        String contextPath = getContextPath(request);
        String requestUri = getRequestUri(request);
        if (StringUtils.startsWithIgnoreCase(requestUri, contextPath)) {
            // Normal case: URI contains context path.
            String path = requestUri.substring(contextPath.length());
            return (StringUtils.hasText(path) ? path : "/");
        } else {
            // Special case: rather unusual.
            return requestUri;
        }
    }

    /**
     * Return the request URI for the given request, detecting an include request
     * URL if called within a RequestDispatcher include.
     * <p>As the value returned by <code>request.getRequestURI()</code> is <i>not</i>
     * decoded by the servlet container, this method will decode it.
     * <p>The URI that the web container resolves <i>should</i> be correct, but some
     * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
     * in the URI. This method cuts off such incorrect appendices.
     *
     * @param request current HTTP request
     * @return the request URI
     */
    public static String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return decodeAndCleanUriString(request, uri);
    }

    /**
     * Decode the supplied URI string and strips any extraneous portion after a ';'.
     *
     * @param request the incoming HttpServletRequest
     * @param uri     the application's URI string
     * @return the supplied URI string stripped of any extraneous portion after a ';'.
     */
    private static String decodeAndCleanUriString(HttpServletRequest request, String uri) {
        uri = decodeRequestString(request, uri);
        int semicolonIndex = uri.indexOf(';');
        return (semicolonIndex != -1 ? uri.substring(0, semicolonIndex) : uri);
    }

    /**
     * Return the context path for the given request, detecting an include request
     * URL if called within a RequestDispatcher include.
     * <p>As the value returned by <code>request.getContextPath()</code> is <i>not</i>
     * decoded by the servlet container, this method will decode it.
     *
     * @param request current HTTP request
     * @return the context path
     */
    public static String getContextPath(HttpServletRequest request) {
        String contextPath = (String) request.getAttribute(INCLUDE_CONTEXT_PATH_ATTRIBUTE);
        if (contextPath == null) {
            contextPath = request.getContextPath();
        }
        if ("/".equals(contextPath)) {
            // Invalid case, but happens for includes on Jetty: silently adapt it.
            contextPath = "";
        }
        return decodeRequestString(request, contextPath);
    }

    /**
     * Decode the given source string with a URLDecoder. The encoding will be taken
     * from the request, falling back to the default "ISO-8859-1".
     * <p>The default implementation uses <code>URLDecoder.decode(input, enc)</code>.
     *
     * @param request current HTTP request
     * @param source  the String to decode
     * @return the decoded String
     * @see #DEFAULT_CHARACTER_ENCODING
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     * @see java.net.URLDecoder#decode(String, String)
     * @see java.net.URLDecoder#decode(String)
     */
    @SuppressWarnings({"deprecation"})
    public static String decodeRequestString(HttpServletRequest request, String source) {
        String enc = determineEncoding(request);
        try {
            return URLDecoder.decode(source, enc);
        }
        catch (UnsupportedEncodingException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Could not decode request string [" + source + "] with encoding '" + enc +
                        "': falling back to platform default encoding; exception message: " + ex.getMessage());
            }
            return URLDecoder.decode(source);
        }
    }

    /**
     * Determine the encoding for the given request.
     * Can be overridden in subclasses.
     * <p>The default implementation checks the request's
     * {@link ServletRequest#getCharacterEncoding() character encoding}, and if that
     * <code>null</code>, falls back to the {@link #DEFAULT_CHARACTER_ENCODING}.
     *
     * @param request current HTTP request
     * @return the encoding for the request (never <code>null</code>)
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    protected static String determineEncoding(HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        if (enc == null) {
            enc = DEFAULT_CHARACTER_ENCODING;
        }
        return enc;
    }

    /**
     * Returns the <code>InetAddress</code> associated with the current request, or <code>null</code> if the
     * address cannot be resolved/determined.
     * <p/>
     * This implementation returns the InetAddress resolved from the request's
     * {@link javax.servlet.ServletRequest#getRemoteHost() remoteHost} value.  The returned <code>String</code>
     * is resolved to an InetAddress by calling
     * {@link InetAddress#getByName(String) InetAddress.getByName(remoteHost)}. If the remote host is <code>null</code>
     * or <code>getByName(remoteHost)</code> throws an exception, <code>null</code> is returned.
     *
     * @param request the incoming ServletRequest
     * @return the <code>InetAddress</code> associated with the current request, or <code>null</code> if the
     *         address cannot be resolved/determined.
     */
    public static InetAddress getInetAddress(ServletRequest request) {
        InetAddress clientAddress = null;
        //get the Host/IP the client is coming from:
        String addrString = request.getRemoteHost();
        try {
            clientAddress = InetAddress.getByName(addrString);
        } catch (UnknownHostException e) {
            if (log.isInfoEnabled()) {
                log.info("Unable to acquire InetAddress from ServletRequest", e);
            }
        }

        return clientAddress;
    }

    /**
     * A convenience method that merely casts the incoming <code>ServletRequest</code> to an
     * <code>HttpServletRequest</code>:
     * <p/>
     * <code>return (HttpServletRequest)request;</code>
     * <p/>
     * Logic could be changed in the future for logging or throwing an meaningful exception in
     * non HTTP request environments (e.g. Portlet API).
     *
     * @param request the incoming ServletRequest
     * @return the <code>request</code> argument casted to an <code>HttpServletRequest</code>.
     */
    public static HttpServletRequest toHttp(ServletRequest request) {
        return (HttpServletRequest) request;
    }

    /**
     * A convenience method that merely casts the incoming <code>ServletResponse</code> to an
     * <code>HttpServletResponse</code>:
     * <p/>
     * <code>return (HttpServletResponse)response;</code>
     * <p/>
     * Logic could be changed in the future for logging or throwing an meaningful exception in
     * non HTTP request environments (e.g. Portlet API).
     *
     * @param response the outgoing ServletResponse
     * @return the <code>response</code> argument casted to an <code>HttpServletResponse</code>.
     */
    public static HttpServletResponse toHttp(ServletResponse response) {
        return (HttpServletResponse) response;
    }

    public static void bindInetAddressToThread(ServletRequest request) {
        InetAddress ip = getInetAddress(request);
        if (ip != null) {
            ThreadContext.bind(ip);
        }
    }

    public static void unbindInetAddressFromThread() {
        ThreadContext.unbindInetAddress();
    }

    /**
     * Convenience method that simplifies retrieval of a required thread-bound ServletRequest.  If there is no
     * ServletRequest bound to the thread when this method is called, an <code>IllegalStateException</code> is
     * thrown.
     * <p/>
     * This method is basically a convenient wrapper for the following:
     * <p/>
     * <code>(ServletRequest){@link ThreadContext#get ThreadContext.get}( SERVLET_REQUEST_KEY );</code>
     * <p/>
     * But throws an <code>IllegalStateException</code> if the value is not bound to the <code>ThreadContext</code>.
     * <p/>
     * This method only returns the bound value if it exists - it does not remove it
     * from the thread.  To remove it, one must call {@link #unbindServletRequest() unbindServletRequest} instead.
     *
     * @return the ServletRequest bound to the thread.  Never returns null.
     * @throws IllegalStateException if no servlet request is bound in the {@link ThreadContext ThreadContext}.
     */
    public static ServletRequest getRequiredServletRequest() throws IllegalStateException {
        ServletRequest request = (ServletRequest) ThreadContext.get(SERVLET_REQUEST_KEY);
        if (request == null) {
            throw new IllegalStateException("No ServletRequest found in ThreadContext. " + NOT_BOUND_ERROR_MESSAGE);
        }
        return request;
    }

    /**
     * Convenience method that simplifies binding a ServletRequest to the current thread (via the ThreadContext).
     *
     * <p>The method's existence is to help reduce casting in your own code and to simplify remembering of
     * ThreadContext key names.  The implementation is simple in that, if the servletRequest is not <tt>null</tt>,
     * it binds it to the thread, i.e.:
     *
     * <pre>
     * if (servletRequest != null) {
     *     ThreadContext.put( SERVLET_REQUEST_KEY, session );
     * }</pre>
     *
     * @param servletRequest the ServletRequest object to bind to the thread.  If the argument is null, nothing will be done.
     */
    public static void bind(ServletRequest servletRequest) {
        if (servletRequest != null) {
            ThreadContext.put(SERVLET_REQUEST_KEY, servletRequest);
        }
    }

    /**
     * Convenience method that simplifies removal of a thread-local ServletRequest from the thread.
     * <p/>
     * The implementation just helps reduce casting and remembering of the ThreadContext key name, i.e it is
     * merely a conveient wrapper for the following:
     * <p/>
     * <code>return (ServletRequest)ThreadContext.remove( SERVLET_REQUEST_KEY );</code>
     * <p/>
     * If you wish to just retrieve the object from the thread without removing it (so it can be retrieved later during
     * thread execution), you should use the {@link #getRequiredServletRequest() getRequiredServletRequest()} method
     * for that purpose.
     *
     * @return the Session object previously bound to the thread, or <tt>null</tt> if there was none bound.
     */
    public static ServletRequest unbindServletRequest() {
        return (ServletRequest) ThreadContext.remove(SERVLET_REQUEST_KEY);
    }

    /**
     * Convenience method that simplifies retrieval of a required thread-bound ServletResponse.  If there is no
     * ServletResponse bound to the thread when this method is called, an <code>IllegalStateException</code> is
     * thrown.
     * <p/>
     * This method is basically a convenient wrapper for the following:
     * <p/>
     * <code>return (ServletResponse){@link ThreadContext#get ThreadContext.get}( SERVLET_RESPONSE_KEY );</code>
     * <p/>
     * But throws an <code>IllegalStateException</code> if the value is not bound to the <code>ThreadContext</code>.
     * <p/>
     * This method only returns the bound value if it exists - it does not remove it
     * from the thread.  To remove it, one must call {@link #unbindServletResponse() unbindServletResponse} instead.
     *
     * @return the ServletResponse bound to the thread.  Never returns null.
     * @throws IllegalStateException if no <code>ServletResponse> is bound in the {@link ThreadContext ThreadContext}
     */
    public static ServletResponse getRequiredServletResponse() throws IllegalStateException {
        ServletResponse response = (ServletResponse) ThreadContext.get(SERVLET_RESPONSE_KEY);
        if (response == null) {
            throw new IllegalStateException("No ServletResponse found in ThreadContext. " + NOT_BOUND_ERROR_MESSAGE);
        }
        return response;
    }

    /**
     * Convenience method that simplifies binding a ServletResponse to the thread via the ThreadContext.
     *
     * <p>The method's existence is to help reduce casting in your own code and to simplify remembering of
     * ThreadContext key names.  The implementation is simple in that, if the servletResponse is not <tt>null</tt>,
     * it binds it to the thread, i.e.:
     *
     * <pre>
     * if (servletResponse != null) {
     *     ThreadContext.put( SERVLET_RESPONSE_KEY, session );
     * }</pre>
     *
     * @param servletResponse the ServletResponse object to bind to the thread.  If the argument is null, nothing will be done.
     */
    public static void bind(ServletResponse servletResponse) {
        if (servletResponse != null) {
            ThreadContext.put(SERVLET_RESPONSE_KEY, servletResponse);
        }
    }

    /**
     * Convenience method that simplifies removal of a thread-local ServletResponse from the thread.
     * <p/>
     * The implementation just helps reduce casting and remembering of the ThreadContext key name, i.e it is
     * merely a conveient wrapper for the following:
     * <p/>
     * <code>return (ServletResponse)ThreadContext.remove( SERVLET_RESPONSE_KEY );</code>
     * <p/>
     * If you wish to just retrieve the object from the thread without removing it (so it can be retrieved later during
     * thread execution), you should use the {@link #getRequiredServletResponse() getRequiredServletResponse()} method
     * for that purpose.
     *
     * @return the Session object previously bound to the thread, or <tt>null</tt> if there was none bound.
     */
    public static ServletResponse unbindServletResponse() {
        return (ServletResponse) ThreadContext.remove(SERVLET_RESPONSE_KEY);
    }

    /**
     * Redirects the current request to a new URL based on the given parameters.
     *
     * @param request          the servlet request.
     * @param response         the servlet response.
     * @param url              the URL to redirect the user to.
     * @param queryParams      a map of parameters that should be set as request parameters for the new request.
     * @param contextRelative  true if the URL is relative to the servlet context path, or false if the URL is absolute.
     * @param http10Compatible whether to stay compatible with HTTP 1.0 clients.
     * @throws java.io.IOException if thrown by response methods.
     */
    public static void issueRedirect(ServletRequest request, ServletResponse response, String url, Map queryParams, boolean contextRelative, boolean http10Compatible) throws IOException {
        RedirectView view = new RedirectView(url, contextRelative, http10Compatible);
        view.renderMergedOutputModel(queryParams, toHttp(request), toHttp(response));
    }

    /**
     * Redirects the current request to a new URL based on the given parameters and default values
     * for unspecified parameters.
     *
     * @param request  the servlet request.
     * @param response the servlet response.
     * @param url      the URL to redirect the user to.
     * @throws java.io.IOException if thrown by response methods.
     */
    public static void issueRedirect(ServletRequest request, ServletResponse response, String url) throws IOException {
        issueRedirect(request, response, url, null, true, true);
    }

    /**
     * Redirects the current request to a new URL based on the given parameters and default values
     * for unspecified parameters.
     *
     * @param request     the servlet request.
     * @param response    the servlet response.
     * @param url         the URL to redirect the user to.
     * @param queryParams a map of parameters that should be set as request parameters for the new request.
     * @throws java.io.IOException if thrown by response methods.
     */
    public static void issueRedirect(ServletRequest request, ServletResponse response, String url, Map queryParams) throws IOException {
        issueRedirect(request, response, url, queryParams, true, true);
    }

    /**
     * Redirects the current request to a new URL based on the given parameters and default values
     * for unspecified parameters.
     *
     * @param request         the servlet request.
     * @param response        the servlet response.
     * @param url             the URL to redirect the user to.
     * @param queryParams     a map of parameters that should be set as request parameters for the new request.
     * @param contextRelative true if the URL is relative to the servlet context path, or false if the URL is absolute.
     * @throws java.io.IOException if thrown by response methods.
     */
    public static void issueRedirect(ServletRequest request, ServletResponse response, String url, Map queryParams, boolean contextRelative) throws IOException {
        issueRedirect(request, response, url, queryParams, contextRelative, true);
    }

    /**
     * <p>Checks to see if a request param is considered true using a loose matching strategy for
     * general values that indicate that something is true or enabled, etc.</p>
     *
     * <p>Values that are considered "true" include (case-insensitive): true, t, 1, enabled, y, yes, on.</p>
     *
     * @param request   the servlet request
     * @param paramName @return true if the param value is considered true or false if it isn't.
     * @return true if the given parameter is considered "true" - false otherwise.
     */
    public static boolean isTrue(ServletRequest request, String paramName) {
        String value = getCleanParam(request, paramName);
        return value != null &&
                (value.equalsIgnoreCase("true") ||
                        value.equalsIgnoreCase("t") ||
                        value.equalsIgnoreCase("1") ||
                        value.equalsIgnoreCase("enabled") ||
                        value.equalsIgnoreCase("y") ||
                        value.equalsIgnoreCase("yes") ||
                        value.equalsIgnoreCase("on"));
    }

    /**
     * Convenience method that returns a request parameter value, first running it through
     * {@link StringUtils#clean(String)}.
     *
     * @param request   the servlet request.
     * @param paramName the parameter name.
     * @return the clean param value, or null if the param does not exist or is empty.
     */
    public static String getCleanParam(ServletRequest request, String paramName) {
        return StringUtils.clean(request.getParameter(paramName));
    }

    public static void saveRequest(ServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        HttpServletRequest httpRequest = toHttp(request);
        SavedRequest savedRequest = new SavedRequest(httpRequest);
        session.setAttribute(SAVED_REQUEST_KEY, savedRequest);
    }

    public static SavedRequest getAndClearSavedRequest(ServletRequest request) {
        SavedRequest savedRequest = getSavedRequest(request);
        if (savedRequest != null) {
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession();
            session.removeAttribute(SAVED_REQUEST_KEY);
        }
        return savedRequest;
    }

    public static SavedRequest getSavedRequest(ServletRequest request) {
        SavedRequest savedRequest = null;
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession(false);
        if (session != null) {
            savedRequest = (SavedRequest) session.getAttribute(SAVED_REQUEST_KEY);
        }
        return savedRequest;
    }


}
