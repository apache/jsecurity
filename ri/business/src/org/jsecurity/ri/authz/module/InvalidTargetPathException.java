/*
 * Copyright (C) 2005 Les Hazlewood
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
package org.jsecurity.ri.authz.module;

import org.jsecurity.authz.AuthorizationException;

/**
 * Exception thrown when trying to evaluate an
 * {@link org.jsecurity.authz.annotation.PermissionsRequired PermissionsRequired} annotation's
 * {@link org.jsecurity.authz.annotation.PermissionsRequired#targetPath() targetPath} attribute.
 *
 * @since 0.1
 * @author Les Hazlewood
 */
public class InvalidTargetPathException extends AuthorizationException {

    /**
     * Creates a new InvalidTargetPathException.
     */
    public InvalidTargetPathException() {
        super();
    }

    /**
     * Constructs a new InvalidTargetPathException.
     *
     * @param message the reason for the exception
     */
    public InvalidTargetPathException( String message ) {
        super( message );
    }

    /**
     * Constructs a new InvalidTargetPathException.
     *
     * @param cause the underlying Throwable that caused this exception to be thrown.
     */
    public InvalidTargetPathException( Throwable cause ) {
        super( cause );
    }

    /**
     * Constructs a new InvalidTargetPathException.
     *
     * @param message the reason for the exception
     * @param cause   the underlying Throwable that caused this exception to be thrown.
     */
    public InvalidTargetPathException( String message, Throwable cause ) {
        super( message, cause );
    }
}