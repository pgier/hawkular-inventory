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

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import org.hawkular.inventory.api.Environments;
import org.hawkular.inventory.api.Feeds;
import org.hawkular.inventory.api.MetricTypes;
import org.hawkular.inventory.api.Metrics;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.ResourceTypes;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Related;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Feed;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Jirka Kremser
 * @since 1.0
 */
final class RelationshipBrowser<E extends Entity> extends AbstractBrowser<E> {

    private RelationshipBrowser(InventoryContext iContext, Class<E> sourceClass, FilterApplicator... path) {
        super(iContext, sourceClass, path);
    }

    public static Relationships.Single single(String relationshipId, InventoryContext iContext, Class<? extends Entity>
            sourceClass, FilterApplicator... path) {
        if (null == relationshipId) {
            throw new IllegalArgumentException("unable to create Relationships.Single without the edge id.");
        }
        RelationshipBrowser b = new RelationshipBrowser(iContext, sourceClass, path);

        return new Relationships.Single() {

            @Override
            public Relationship entity() {
                HawkularPipeline<?, Edge> edges = b.source().outE().has("id", relationshipId).cast(Edge.class);
                if (!edges.hasNext()) {
                    throw new RelationNotFoundException(sourceClass, relationshipId, FilterApplicator.filters(path));
                }
                Edge edge = edges.next();
                return new Relationship(edge.getId().toString(), edge.getLabel(), convert(edge.getVertex(Direction
                        .OUT)), convert(edge.getVertex(Direction.IN)));
            }

            @Override
            public Tenants.ReadRelate tenants() {
                return b.tenants(EdgeFilter.ID, relationshipId);
            }

            @Override
            public Environments.ReadRelate environments() {
                return (Environments.ReadRelate) b.<EnvironmentsService>getService(EdgeFilter.ID, relationshipId,
                        Environment.class, EnvironmentsService.class);
            }

            @Override
            public Feeds.ReadRelate feeds() {
                return (Feeds.ReadRelate)b.<FeedsService>getService(EdgeFilter.ID, relationshipId, Feed.class,
                        FeedsService.class);
            }

            @Override
            public MetricTypes.ReadRelate metricTypes() {
                return (MetricTypes.ReadRelate)b.<MetricTypesService>getService(EdgeFilter.ID, relationshipId,
                        MetricType.class, MetricTypesService.class);
            }

            @Override
            public Metrics.ReadRelate metrics() {
                return (Metrics.ReadRelate)b.<MetricsService>getService(EdgeFilter.ID, relationshipId, Metrics.class,
                        MetricsService.class);
            }

            @Override
            public Resources.ReadRelate resources() {
                return (Resources.ReadRelate)b.<ResourcesService>getService(EdgeFilter.ID, relationshipId, Resource
                                .class, ResourcesService.class);
            }

            @Override
            public ResourceTypes.ReadRelate resourceTypes() {
                return (ResourceTypes.ReadRelate)b.<ResourceTypesService>getService(EdgeFilter.ID, relationshipId,
                        ResourceType.class,
                        ResourceTypesService.class);
            }
        };
    }

    public static Relationships.Multiple multiple(String named, InventoryContext iContext, Class<? extends Entity>
            sourceClass, FilterApplicator... path) {

        RelationshipBrowser b = new RelationshipBrowser(iContext, sourceClass, path);

        return new Relationships.Multiple() {
            @Override
            public Set<Relationship> entities() {
                // TODO process filters

                HawkularPipeline<?, Edge> edges = null == named ? b.source().outE() : b.source().outE(named);
                Stream<Relationship> relationshipStream = StreamSupport
                        .stream(edges.spliterator(), false)
                        .map(edge -> new Relationship(edge.getId().toString(), edge.getLabel(),
                                convert(edge.getVertex(Direction.OUT)), convert(edge.getVertex(Direction.IN))));
                return relationshipStream.collect(Collectors.toSet());
            }

            @Override
            public Tenants.Read tenants() {
                // TODO implement
                return null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Environments.Read environments() {
                return (Environments.Read) b.<EnvironmentsService>getService(EdgeFilter.NAMED, named, Environment.class,
                        EnvironmentsService.class);
            }

            @Override
            public Feeds.Read feeds() {
                return (Feeds.Read) b.<FeedsService>getService(EdgeFilter.NAMED, named, Feed.class, FeedsService.class);
            }

            @Override
            public MetricTypes.Read metricTypes() {
                return (MetricTypes.Read) b.<MetricTypesService>getService(EdgeFilter.NAMED, named, MetricType.class,
                        MetricTypesService.class);
            }

            @Override
            public Metrics.Read metrics() {
                return (Metrics.Read) b.<MetricsService>getService(EdgeFilter.NAMED, named, Metrics.class,
                        MetricsService.class);
            }

            @Override
            public Resources.Read resources() {
                return (Resources.Read) b.<ResourcesService>getService(EdgeFilter.NAMED, named, Resource.class,
                        ResourcesService.class);
            }

            @Override
            public ResourceTypes.Read resourceTypes() {
                return (ResourceTypes.Read)b.<ResourceTypesService>getService(EdgeFilter.NAMED, named, ResourceType
                                .class, ResourceTypesService.class);
            }
        };
    }


    private <S extends AbstractSourcedGraphService> S getService(EdgeFilter filter, String value, Class<? extends
            Entity> clazz1, Class<S> clazz2) {
        Filter.Accumulator acc = Filter.by(EdgeFilter.NAMED == filter ? Related.by(value) : Related.byRelationshipWithId
                (value), With.type(clazz1));
        try {
            Constructor<S> constructor = clazz2.getDeclaredConstructor(InventoryContext.class, PathContext.class);
            return constructor.newInstance(context, pathToHereWithSelect(acc));
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException
                e) {
            throw new IllegalStateException("Unable to create new instance of " + clazz2.getCanonicalName(), e);
        }
    }

    public TenantsService tenants(EdgeFilter filter, String value) {
        //return new TenantsService(graph, pathToHereWithSelect(Filter.by(Related.by(id),
        //        With.type(Tenant.class))));
        // TODO implement
        return null;
    }

    private enum EdgeFilter {
        ID, NAMED
    }
}