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
package org.hawkular.inventory.impl.tinkerpop;

import com.tinkerpop.blueprints.Vertex;
import org.hawkular.inventory.api.RelationAlreadyExistsException;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Relationship;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * @author Lukas Krejci
 * @author Jiri Kremser
 * @since 1.0
 */
final class RelationshipService<E extends Entity> extends AbstractSourcedGraphService<Relationships.Single,
        Relationships.Multiple, E, Relationship.Blueprint> implements Relationships.ReadWrite, Relationships.Read {

    RelationshipService(InventoryContext iContext, PathContext ctx, Class<E> sourceClass, Relationships.Direction
            direction) {
        super(iContext, sourceClass, ctx);
    }

    @Override
    public Relationships.Single get(String id) {
        return createSingleBrowser(id, pathWith(selectCandidates()).get());
    }

    @Override
    public Relationships.Multiple getAll(Filter... filters) {
        Filter[] invalidFilters = getInvalidRelationshipsFilters(filters);
        if (invalidFilters.length != 0) {
            throw new IllegalArgumentException("Cannot use following filters for filtering the relationships: " +
                    Arrays.toString(invalidFilters));
        }
        return createMultiBrowser(null, pathWith(selectCandidates()).andFilter(filters).get());
    }

    @Override
    protected Relationships.Single createSingleBrowser(FilterApplicator... path) {
        return createSingleBrowser(null, path);
    }

    private Relationships.Single createSingleBrowser(String id, FilterApplicator... path) {
        return RelationshipBrowser.single(id, context, entityClass, path);
    }

    @Override
    protected Relationships.Multiple createMultiBrowser(FilterApplicator... path) {
        return createMultiBrowser(null, path);
    }

    private Relationships.Multiple createMultiBrowser(String named, FilterApplicator... path) {
        // TODO foo
        return RelationshipBrowser.multiple(named, context, entityClass, path);
    }

    @Override
    protected String getProposedId(Relationship.Blueprint b) {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    protected Filter[] initNewEntity(Vertex newEntity, Relationship.Blueprint s) {
        return new Filter[0];
    }

    @Override
    public Relationships.Multiple named(String name) {
        return createMultiBrowser(name, path);
    }

    @Override
    public Relationships.Multiple named(Relationships.WellKnown name) {
        return createMultiBrowser(name.name(), path);
    }

    @Override
    public Relationships.Single create(Relationship.Blueprint blueprint) throws RelationAlreadyExistsException {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Relationship relationship) throws RelationNotFoundException {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) throws RelationNotFoundException {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    private Filter[] getInvalidRelationshipsFilters(Filter[] filters) {
        if (null != filters || filters.length == 0) {
            return filters;
        }
        Predicate<Filter> isValidFilterForRelation = f -> f instanceof With.Ids;
        return Arrays.stream(filters).filter(isValidFilterForRelation.negate()).toArray(n -> new Filter[n]);
    }
}
