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
import org.jsecurity.mgt.DefaultSecurityManager;
import org.jsecurity.realm.Realm;
import org.jsecurity.session.Session;
import org.jsecurity.session.mgt.SessionManager;
import org.jsecurity.subject.PrincipalCollection;
import org.jsecurity.subject.Subject;
import org.jsecurity.util.LifecycleUtils;
import org.jsecurity.web.session.DefaultWebSessionManager;
import org.jsecurity.web.session.ServletContainerSessionManager;
import org.jsecurity.web.session.WebSessionManager;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.net.InetAddress;
import java.util.Collection;

/**
 * SecurityManager implementation that should be used in web-based applications or any application that requires
 * HTTP connectivity (SOAP, http remoting, etc).
 *
 * @author Les Hazlewood
 * @since 0.2
 */
public class DefaultWebSecurityManager extends DefaultSecurityManager {

    private static final Log log = LogFactory.getLog(DefaultWebSecurityManager.class);

    public static final String HTTP_SESSION_MODE = "http";
    public static final String JSECURITY_SESSION_MODE = "jsecurity";

    /**
     * The key that is used to store subject principals in the session.
     */
    public static final String PRINCIPALS_SESSION_KEY = DefaultWebSecurityManager.class.getName() + "_PRINCIPALS_SESSION_KEY";

    /**
     * The key that is used to store whether or not the user is authenticated in the session.
     */
    public static final String AUTHENTICATED_SESSION_KEY = DefaultWebSecurityManager.class.getName() + "_AUTHENTICATED_SESSION_KEY";

    private String sessionMode = HTTP_SESSION_MODE; //default

    public DefaultWebSecurityManager() {
        setRememberMeManager(new WebRememberMeManager());
    }

    public DefaultWebSecurityManager(Realm singleRealm) {
        setRealm(singleRealm);
    }

    public DefaultWebSecurityManager(Collection<Realm> realms) {
        setRealms(realms);
    }

    /**
     * Sets the path used to store the remember me cookie.  This determines which paths
     * are able to view the remember me cookie.
     *
     * @param rememberMeCookiePath the path to use for the remember me cookie.
     */
    public void setRememberMeCookiePath(String rememberMeCookiePath) {
        ((WebRememberMeManager) getRememberMeManager()).setCookiePath(rememberMeCookiePath);
    }

    /**
     * Sets the maximum age allowed for the remember me cookie.  This basically sets how long
     * a user will be remembered by the "remember me" feature.  Used when calling
     * {@link javax.servlet.http.Cookie#setMaxAge(int) maxAge}.  Please see that JavaDoc for the semantics on the
     * repercussions of negative, zero, and positive values for the maxAge.
     *
     * @param rememberMeMaxAge the maximum age for the remember me cookie.
     */
    public void setRememberMeCookieMaxAge(Integer rememberMeMaxAge) {
        ((WebRememberMeManager) getRememberMeManager()).setCookieMaxAge(rememberMeMaxAge);
    }

    private DefaultWebSessionManager getSessionManagerForCookieAttributes() {
        SessionManager sessionManager = getSessionManager();
        if (!(sessionManager instanceof DefaultWebSessionManager)) {
            String msg = "The convenience passthrough methods for setting session id cookie attributes " +
                    "are only available when the underlying SessionManager implementation is " +
                    DefaultWebSessionManager.class.getName() + ", which is enabled by default when the " +
                    "sessionMode is 'jsecurity'.";
            throw new IllegalStateException(msg);
        }
        return (DefaultWebSessionManager) sessionManager;
    }

    public void setSessionIdCookieName(String name) {
        getSessionManagerForCookieAttributes().setSessionIdCookieName(name);
    }

    public void setSessionIdCookiePath(String path) {
        getSessionManagerForCookieAttributes().setSessionIdCookiePath(path);
    }

    public void setSessionIdCookieMaxAge(int maxAge) {
        getSessionManagerForCookieAttributes().setSessionIdCookieMaxAge(maxAge);
    }

    public void setSessionIdCookieSecure(boolean secure) {
        getSessionManagerForCookieAttributes().setSessionIdCookieSecure(secure);
    }

    public String getSessionMode() {
        return sessionMode;
    }

    public void setSessionMode(String sessionMode) {
        if (sessionMode == null ||
                (!sessionMode.equals(HTTP_SESSION_MODE) && !sessionMode.equals(JSECURITY_SESSION_MODE))) {
            String msg = "Invalid sessionMode [" + sessionMode + "].  Allowed values are " +
                    "public static final String constants in the " + getClass().getName() + " class: '"
                    + HTTP_SESSION_MODE + "' or '" + JSECURITY_SESSION_MODE + "', with '" +
                    HTTP_SESSION_MODE + "' being the default.";
            throw new IllegalArgumentException(msg);
        }
        boolean recreate = this.sessionMode == null || !this.sessionMode.equals(sessionMode);
        this.sessionMode = sessionMode;
        if (recreate) {
            LifecycleUtils.destroy(getSessionManager());
            SessionManager sm = createSessionManager();
            setSessionManager(sm);
        }
    }

    public boolean isHttpSessionMode() {
        return this.sessionMode == null || this.sessionMode.equals(HTTP_SESSION_MODE);
    }

    protected SessionManager newSessionManagerInstance() {
        if (isHttpSessionMode()) {
            if (log.isInfoEnabled()) {
                log.info(HTTP_SESSION_MODE + " mode - enabling ServletContainerSessionManager (Http Sessions)");
            }
            return new ServletContainerSessionManager();
        } else {
            if (log.isInfoEnabled()) {
                log.info(JSECURITY_SESSION_MODE + " mode - enabling WebSessionManager (JSecurity heterogenous sessions)");
            }
            return new DefaultWebSessionManager();
        }
    }

    protected PrincipalCollection getPrincipals(Session session) {
        PrincipalCollection principals = null;
        if (session != null) {
            principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
        }
        return principals;
    }

    protected PrincipalCollection getPrincipals(Session existing, ServletRequest servletRequest, ServletResponse servletResponse) {
        PrincipalCollection principals = getPrincipals(existing);
        if (principals == null) {
            //check remember me:
            principals = getRememberedIdentity();
            if (principals != null && existing != null) {
                existing.setAttribute(PRINCIPALS_SESSION_KEY, principals);
            }
        }
        return principals;
    }

    protected boolean isAuthenticated(Session session) {
        Boolean value = null;
        if (session != null) {
            value = (Boolean) session.getAttribute(AUTHENTICATED_SESSION_KEY);
        }
        return value != null && value;
    }

    protected boolean isAuthenticated(Session existing, ServletRequest servletRequest, ServletResponse servletResponse) {
        return isAuthenticated(existing);
    }

    public Subject createSubject() {
        ServletRequest request = WebUtils.getRequiredServletRequest();
        ServletResponse response = WebUtils.getRequiredServletResponse();
        return createSubject(request, response);
    }

    public Subject createSubject(ServletRequest request, ServletResponse response) {
        Session session = ((WebSessionManager) getSessionManager()).getSession(request, response);
        if (session == null) {
            if (log.isTraceEnabled()) {
                log.trace("No session found for the incoming request.  The Subject instance created for " +
                        "the incoming request will not have an associated Session.");
            }
        }
        return createSubject(session, request, response);
    }

    public Subject createSubject(Session existing, ServletRequest request, ServletResponse response) {
        PrincipalCollection principals = getPrincipals(existing, request, response);
        boolean authenticated = isAuthenticated(existing, request, response);
        return createSubject(principals, authenticated, existing, request, response);
    }

    protected Subject createSubject(PrincipalCollection principals,
                                    boolean authenticated,
                                    Session existing,
                                    ServletRequest request,
                                    ServletResponse response) {
        InetAddress inetAddress = WebUtils.getInetAddress(request);
        return createSubject(principals, existing, authenticated, inetAddress);
    }

    protected void bind(Subject subject) {
        super.bind(subject);
        ServletRequest request = WebUtils.getRequiredServletRequest();
        ServletResponse response = WebUtils.getRequiredServletResponse();
        bind(subject, request, response);
    }

    protected void bind(Subject subject, ServletRequest request, ServletResponse response) {

        PrincipalCollection principals = subject.getPrincipals();
        if (principals != null && !principals.isEmpty()) {
            Session session = subject.getSession();
            session.setAttribute(PRINCIPALS_SESSION_KEY, principals);
        } else {
            Session session = subject.getSession(false);
            if (session != null) {
                session.removeAttribute(PRINCIPALS_SESSION_KEY);
            }
        }

        if (subject.isAuthenticated()) {
            Session session = subject.getSession();
            session.setAttribute(AUTHENTICATED_SESSION_KEY, subject.isAuthenticated());
        } else {
            Session session = subject.getSession(false);
            if (session != null) {
                session.removeAttribute(AUTHENTICATED_SESSION_KEY);
            }
        }
    }
}
