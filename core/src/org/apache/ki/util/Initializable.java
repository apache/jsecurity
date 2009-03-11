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
package org.apache.ki.util;

import org.apache.ki.KiException;

/**
 * Apache Ki container-agnostic interface that indicates that this object requires initialization.
 *
 * @author Les Hazlewood
 * @author Jeremy Haile
 * @see org.ki.spring.LifecycleBeanPostProcessor
 * @since 0.2
 */
public interface Initializable {

    /**
     * Initializes this object.
     *
     * @throws org.apache.ki.KiException if an exception occurs during initialization.
     */
    void init() throws KiException;

}
