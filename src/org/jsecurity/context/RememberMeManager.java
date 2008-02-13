package org.jsecurity.context;

import org.jsecurity.authc.Account;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationToken;

/**
 * @author Les Hazlewood
 * @since 1.0
 */
public interface RememberMeManager {

    Object getRememberedIdentity();

    void onSuccessfulLogin( AuthenticationToken token, Account account );

    void onFailedLogin( AuthenticationToken token, AuthenticationException ae );

    void onLogout( Object subjectPrincipals );
}