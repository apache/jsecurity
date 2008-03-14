/*
 * Copyright (C) 2005-2008 Jeremy Haile, Les Hazlewood
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the
 *
 * Free Software Foundation, Inc.
 * 59 Temple Place, Suite 330
 * Boston, MA 02111-1307
 * USA
 *
 * Or, you may view it online at
 * http://www.opensource.org/licenses/lgpl-license.php
 */
package org.jsecurity.authc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsecurity.authc.event.AuthenticationEventListener;
import org.jsecurity.authc.event.mgt.AuthenticationEventListenerRegistrar;
import org.jsecurity.authc.event.mgt.AuthenticationEventManager;
import org.jsecurity.authc.event.mgt.DefaultAuthenticationEventManager;

import java.util.Collection;

/**
 * Superclass for almost all {@link Authenticator} implementations that performs the common work around authentication
 * attempts.
 *
 * <p>This class delegates the actual authentication attempt to subclasses but supports event propagation for
 * successful and failed logins and logouts.
 *
 * <p>In most cases, the only thing a subclass needs to do (via its {@link #doAuthenticate} implementation)
 * is perform the actual principal/credential verification process for the submitted <tt>AuthenticationToken</tt>.
 *
 * <p>This implementation employs an event-based architecture so other components may react to both failed and
 * successful authentication attempts.  Failure or success events are triggered based on the
 * subclass's {@link #doAuthenticate} implementation throwing an exception or not, respectively.  That is, a failure
 * event will be created if <tt>doAuthenticate</tt> throws an exception a success event will be created and
 * sent if it does not.  The actual events
 * themselves are constructed and sent via an {@link AuthenticationEventManager} to interested
 * {@link AuthenticationEventListener}s.  If no <tt>AuthenticationEventListener</tt>s are configured, no events will
 * be created or sent.
 *
 * <p>Both the event manager and the event listeners may be set as properties of this class, but a default event
 * manager will be provided.
 *
 * @author Jeremy Haile
 * @author Les Hazlewood
 * @since 0.1
 */
public abstract class AbstractAuthenticator
        implements Authenticator, LogoutAware, AuthenticationEventListenerRegistrar {

    /*--------------------------------------------
    |             C O N S T A N T S             |
    ============================================*/
    /**
     * Commons-logging logger
     */
    protected final transient Log log = LogFactory.getLog(getClass());

    /*--------------------------------------------
    |    I N S T A N C E   V A R I A B L E S    |
    ============================================*/
    private AuthenticationEventManager authcEventManager = new DefaultAuthenticationEventManager();

    /*--------------------------------------------
    |         C O N S T R U C T O R S           |
    ============================================*/
    public AbstractAuthenticator() {
    }

    /*--------------------------------------------
    |  A C C E S S O R S / M O D I F I E R S    |
    ============================================*/

    public AuthenticationEventManager getAuthenticationEventManager() {
        return authcEventManager;
    }

    public void setAuthenticationEventManager(AuthenticationEventManager authcEventManager) {
        this.authcEventManager = authcEventManager;
    }

    public void setAuthenticationEventListeners(Collection<AuthenticationEventListener> listeners) {
        this.authcEventManager.setAuthenticationEventListeners(listeners);
    }

    public void add(AuthenticationEventListener listener) {
        this.authcEventManager.add(listener);
    }

    public boolean remove(AuthenticationEventListener listener) {
        return this.authcEventManager.remove(listener);
    }

    /*-------------------------------------------
    |               M E T H O D S               |
    ============================================*/
    protected void sendFailureEvent(AuthenticationToken token, AuthenticationException ae) {
        this.authcEventManager.sendFailureEvent(token, ae);
    }

    protected void sendSuccessEvent(AuthenticationToken token, Account account) {
        this.authcEventManager.sendSuccessEvent(token, account);
    }

    protected void sendLogoutEvent(Object subjectPrincipal) {
        this.authcEventManager.sendLogoutEvent(subjectPrincipal);
    }

    public void onLogout(Object accountPrincipal) {
        sendLogoutEvent(accountPrincipal);
    }


    /**
     * Implementation of the {@link Authenticator} interface that functions in the following manner:
     *
     * <ol>
     * <li>Calls template {@link #doAuthenticate doAuthenticate} method for subclass execution of the actual
     * authentication behavior.</li>
     * <li>If an <tt>AuthenticationException</tt> is thrown during <tt>doAuthenticate</tt>, create and send a
     * failure <tt>AuthenticationEvent</tt> that represents this failure, and then propogate this exception
     * for the caller to handle.</li>
     * <li>If no exception is thrown (indicating a successful login), send a success <tt>AuthenticationEvent</tt>
     * noting the successful authentication.</li>
     * <li>Return the <tt>Account</tt></li>
     * </ol>
     *
     * @param token the submitted token representing the subject's (user's) login principals and credentials.
     * @return the Account referencing the authenticated user's account data.
     * @throws AuthenticationException if there is any problem during the authentication process - see the
     *                                 interface's JavaDoc for a more detailed explanation.
     */
    public final Account authenticate(AuthenticationToken token)
            throws AuthenticationException {

        if (token == null) {
            throw new IllegalArgumentException("Method argumet (authentication token) cannot be null.");
        }

        if (log.isTraceEnabled()) {
            log.trace("Authentication attempt received for token [" + token + "]");
        }

        Account account;
        try {
            account = doAuthenticate(token);
            if (account == null) {
                String msg = "No account information found for authentication token [" + token + "] by this " +
                        "Authenticator instance.  Please check that it is configured correctly.";
                throw new AuthenticationException(msg);
            }
        } catch (Throwable t) {
            AuthenticationException ae = null;
            if (t instanceof AuthenticationException) {
                ae = (AuthenticationException) t;
            }
            if (ae == null) {
                //Exception thrown was not an expected AuthenticationException.  Therefore it is probably a little more
                //severe or unexpected.  So, wrap in an AuthenticationException, log to warn, and propagate:
                String msg = "Authentication failed for token submission [" + token + "].  Possible unexpected " +
                        "error? (Typical or expected login exceptions should extend from AuthenticationException).";
                ae = new AuthenticationException(msg, t);
                if (log.isWarnEnabled()) {
                    log.warn(msg, t);
                }
            }
            try {
                sendFailureEvent(token, ae);
            } catch (Throwable t2) {
                String msg = "Unable to send event for failed authentication attempt - listener error?.  Please check " +
                        "your AuthenticationEventListener implementation(s).  Logging sending exception and " +
                        "propagating original AuthenticationException instead...";
                if (log.isWarnEnabled()) {
                    log.warn(msg, t2);
                }
            }


            throw ae;
        }

        if (log.isInfoEnabled()) {
            log.info("Authentication successful for token [" + token + "].  " +
                    "Returned account: [" + account + "]");
        }

        sendSuccessEvent(token, account);

        return account;
    }

    /**
     * Template design pattern hook for subclasses to implement specific authentication behavior.
     *
     * <p>Common behavior for most authentication attempts is encapsulated in the
     * {@link #authenticate} method and that method invokes this one for custom behavior.
     *
     * <p><b>N.B.</b> Subclasses <em>should</em> throw some kind of
     * <tt>AuthenticationException</tt> if there is a problem during
     * authentication instead of returning <tt>null</tt>.  A <tt>null</tt> return value indicates
     * a configuration or programming error, since <tt>AuthenticationException</tt>s should
     * indicate any expected problem (such as an unknown account or username, or invalid password, etc).
     *
     * @param token the authentication token encapsulating the user's login information.
     * @return an <tt>Account</tt> object encapsulating the user's account information
     *         important to JSecurity.
     * @throws AuthenticationException if there is a problem logging in the user.
     */
    protected abstract Account doAuthenticate(AuthenticationToken token)
            throws AuthenticationException;



}