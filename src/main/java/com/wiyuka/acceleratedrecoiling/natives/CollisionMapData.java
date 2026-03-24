package com.wiyuka.acceleratedrecoiling.natives;

import com.wiyuka.acceleratedrecoiling.config.FoldConfig;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollisionMapData {
    private static final ThreadLocal<CollisionState> CONTEXT = ThreadLocal.withInitial(CollisionState::new);

    public static void putCollision(int idA, int idB) {
        CONTEXT.get().putCollision(idA, idB);
    }

    public static void clear() {
        CONTEXT.get().clear();
    }

    public static List<Entity> getCollisionList(Entity source, Level level) {
        return CONTEXT.get().getCollisionList(source, level);
    }

    public static void remove() {
        CONTEXT.remove();
    }

    private static class CollisionState {
        private IntArrayList[] collisionMap = new IntArrayList[10000];

        public void putCollision(int idA, int idB) {
            addSingle(idA, idB);
            addSingle(idB, idA);
        }

        private void addSingle(int source, int target) {
            if (source >= collisionMap.length) {
                resize(Math.max(source + 1, (int) (collisionMap.length * 1.5)));
            }

            IntArrayList list = collisionMap[source];
            if (list == null) {
                list = new IntArrayList(FoldConfig.maxCollision * 2);
                collisionMap[source] = list;
            }
            list.add(target);
        }

        private void resize(int newSize) {
            collisionMap = Arrays.copyOf(collisionMap, newSize);
        }

        public void clear() {
            for (int i = 0; i < collisionMap.length; i++) {
                if (collisionMap[i] != null && !collisionMap[i].isEmpty()) {
                    collisionMap[i].clear();
                }
            }
        }

        public List<Entity> getCollisionList(Entity source, Level level) {
            int id = TempID.getId(source);
            if (id == -1 || id >= collisionMap.length) {
                return Collections.emptyList();
            }
            IntArrayList ids = collisionMap[id];
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            return new EntityListView(ids, level, source);
        }
    }

    private static class EntityListView extends AbstractList<Entity> {
        private final IntArrayList ids;
        private final Level level;
        private final Entity source;

        public EntityListView(IntArrayList ids, Level level, Entity source) {
            this.ids = ids;
            this.level = level;
            this.source = source;
        }

        @Override
        public Entity get(int index) {
            int entityId = ids.getInt(index);
            Entity target = TempID.getEntity(entityId);
            if (target == null) return source;
            return target;
        }

        @Override
        public int size() {
            return ids.size();
        }

        @Override
        public boolean isEmpty() {
            return ids.isEmpty();
        }
    }
}