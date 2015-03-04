/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.inventory.api;

/**
 * Base interface of all browser interfaces over multiple entities that can have relations.
 *
 * @param <Entity> the type of the entity being browsed
 *
 * @author Lukas Krejci
 * @author Jirka Kremser
 * @since 1.0
 */
interface MultipleRelatableEntityBrowser<Entity> extends ResolvableToMany<Entity> {

    /**
     * @return the (read) access interface to all (outgoing) relationships of the entities on the current position in
     * the inventory traversal.
     */
    Relationships.Read relationships();

    /**
     * @param direction the direction of the relation (aka edge) This is needed because direction are not bidirectional.
     * @return the (read) access interface to all relationships of the entities on the current position in
     * the inventory traversal.
     */
    Relationships.Read relationships(Relationships.Direction direction);
}