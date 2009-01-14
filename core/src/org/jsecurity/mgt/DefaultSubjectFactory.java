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
package org.jsecurity.mgt;

import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.InetAuthenticationToken;
import org.jsecurity.session.Session;
import org.jsecurity.subject.DelegatingSubject;
import org.jsecurity.subject.PrincipalCollection;
import org.jsecurity.subject.Subject;
import org.jsecurity.util.ThreadContext;

import java.net.InetAddress;

/**
 * TODO - Class JavaDoc
 *
 * @author Les Hazlewood
 * @since 1.0
 */
public class DefaultSubjectFactory implements SubjectFactory {

    private SecurityManager securityManager;

    public DefaultSubjectFactory() {
    }

    public DefaultSubjectFactory(SecurityManager securityManager) {
        setSecurityManager(securityManager);
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Subject createSubject(PrincipalCollection principals, Session existing,
                                 boolean authenticated, InetAddress inetAddress) {
        return new DelegatingSubject(principals, authenticated, inetAddress, existing, getSecurityManager());
    }

    private void assertPrincipals(AuthenticationInfo info) {
        PrincipalCollection principals = info.getPrincipals();
        if (principals == null || principals.isEmpty()) {
            String msg = "AuthenticationInfo must have non null and non empty principals.";
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Creates a <tt>Subject</tt> instance for the user represented by the given method arguments.
     *
     * @param token the <tt>AuthenticationToken</tt> submitted for the successful authentication.
     * @param info  the <tt>AuthenticationInfo</tt> of a newly authenticated user.
     * @return the <tt>Subject</tt> instance that represents the user and session data for the newly
     *         authenticated user.
     */
    public Subject createSubject(AuthenticationToken token, AuthenticationInfo info, Subject existing) {
        assertPrincipals(info);

        //get any existing session that may exist - we don't want to lose it:
        Session session = null;
        if (existing != null) {
            session = existing.getSession(false);
        }

        InetAddress authcSourceIP = null;
        if (token instanceof InetAuthenticationToken) {
            authcSourceIP = ((InetAuthenticationToken) token).getInetAddress();
        }
        if (authcSourceIP == null) {
            //try the thread local:
            authcSourceIP = ThreadContext.getInetAddress();
        }

        return createSubject(info.getPrincipals(), session, true, authcSourceIP);
    }
}
