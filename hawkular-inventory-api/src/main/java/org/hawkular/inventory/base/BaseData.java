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
package org.hawkular.inventory.base;

import static org.hawkular.inventory.api.Relationships.WellKnown.hasData;
import static org.hawkular.inventory.api.filters.With.id;

import java.util.Collections;
import java.util.Set;

import org.hawkular.inventory.api.Data;
import org.hawkular.inventory.api.EntityNotFoundException;
import org.hawkular.inventory.api.Log;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.DataEntity;
import org.hawkular.inventory.api.model.RelativePath;
import org.hawkular.inventory.api.model.StructuredData;
import org.hawkular.inventory.api.paging.Page;
import org.hawkular.inventory.api.paging.Pager;
import org.hawkular.inventory.base.spi.ShallowStructuredData;

/**
 * Contains acccess interface implementations for accessing data entities.
 *
 * @author Lukas Krejci
 * @since 0.3.0
 */
public final class BaseData {

    private BaseData() {
    }


    public static final class Read<BE> extends Traversal<BE, DataEntity> implements Data.Read {
        public Read(TraversalContext<BE, DataEntity> context) {
            super(context);
        }

        @Override
        public Data.Multiple getAll(Filter[][] filters) {
            return new Multiple<>(context.proceed().whereAll(filters).get());
        }

        @Override
        public Data.Single get(DataEntity.Role role) throws EntityNotFoundException {
            return new Single<>(context.proceed().where(id(role.name())).get());
        }
    }

    public static final class ReadWrite<BE>
            extends Mutator<BE, DataEntity, DataEntity.Blueprint, DataEntity.Update, DataEntity.Role>
            implements Data.ReadWrite {

        public ReadWrite(TraversalContext context) {
            super(context);
        }

        @Override
        protected String getProposedId(DataEntity.Blueprint blueprint) {
            return blueprint.getRole().name();
        }

        @Override
        protected EntityAndPendingNotifications<DataEntity> wireUpNewEntity(BE entity,
                DataEntity.Blueprint blueprint, CanonicalPath parentPath, BE parent) {

            DataEntity data = new DataEntity(parentPath, blueprint.getRole(), blueprint.getValue());

            BE value = context.backend.persist(blueprint.getValue());

            //don't report this relationship, it is implicit
            //also, don't run the RelationshipRules checks - we're in the "privileged code" that is allowed to do
            //this
            context.backend.relate(entity, value, hasData.name(), null);

            return new EntityAndPendingNotifications<>(data, Collections.emptyList());
        }

        @Override
        public Data.Single create(DataEntity.Blueprint data) {
            return new Single<>(context.replacePath(doCreate(data)));
        }

        @Override
        protected void cleanup(DataEntity.Role role, BE entityRepresentation) {
            Set<BE> rels = context.backend.getRelationships(entityRepresentation, Relationships.Direction.outgoing,
                    hasData.name());

            if (rels.isEmpty()) {
                Log.LOGGER.wNoDataAssociatedWithEntity(context.backend.extractCanonicalPath(entityRepresentation));
                return;
            }

            BE dataRel = rels.iterator().next();

            BE structuredData = context.backend.getRelationshipTarget(dataRel);

            context.backend.deleteStructuredData(structuredData);
            context.backend.delete(dataRel);
        }

        @Override
        public Data.Multiple getAll(Filter[][] filters) {
            return new Multiple<>(context.proceed().whereAll(filters).get());
        }

        @Override
        public Data.Single get(DataEntity.Role role) throws EntityNotFoundException {
            return new Single<>(context.proceed().where(id(role.name())).get());
        }
    }

    public static final class Single<BE>
            extends SingleEntityFetcher<BE, DataEntity, DataEntity.Update>
            implements Data.Single {

        public Single(TraversalContext<BE, DataEntity> context) {
            super(context);
        }

        @Override
        public StructuredData data(RelativePath dataPath) {
            //doing this in 2 queries might seem inefficient but this I think needs to be done to be able to
            //do the filtering
            return loadEntity((b, e) -> {
                BE dataEntity = context.backend.descendToData(b, dataPath);
                return dataEntity == null ? null : context.backend.convert(dataEntity, StructuredData.class);
            });
        }

        @Override
        public StructuredData flatData(RelativePath dataPath) {
            return loadEntity((b, e) -> {
                BE dataEntity = context.backend.descendToData(b, dataPath);
                return dataEntity == null ? null : context.backend.convert(dataEntity, ShallowStructuredData.class)
                        .getData();
            });
        }
    }

    public static final class Multiple<BE>
            extends MultipleEntityFetcher<BE, DataEntity, DataEntity.Update>
            implements Data.Multiple {

        public Multiple(TraversalContext<BE, DataEntity> context) {
            super(context);
        }

        @Override
        public Page<StructuredData> data(RelativePath dataPath, Pager pager) {
            return loadEntities(pager, (b, e) -> {
                BE dataEntity = context.backend.descendToData(b, dataPath);
                return context.backend.convert(dataEntity, StructuredData.class);
            });
        }

        @Override
        public Page<StructuredData> flatData(RelativePath dataPath, Pager pager) {
            return loadEntities(pager, (b, e) -> {
                BE dataEntity = context.backend.descendToData(b, dataPath);
                return context.backend.convert(dataEntity, ShallowStructuredData.class).getData();
            });
        }
    }
}