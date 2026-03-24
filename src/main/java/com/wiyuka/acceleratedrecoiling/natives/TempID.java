package com.wiyuka.acceleratedrecoiling.natives;

import com.wiyuka.acceleratedrecoiling.api.EntityAccessBridge;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;

public class TempID {
    private static final ThreadLocal<TempIDState> CONTEXT = ThreadLocal.withInitial(TempIDState::new);

    public static void tickStart() {
        CONTEXT.get().tickStart();
    }

    public static void addEntity(Entity e) {
        CONTEXT.get().addEntity(e);
    }

    public static Entity getEntity(int id) {
        return CONTEXT.get().getEntity(id);
    }

    public static int getId(Entity e) {
        return EntityAccessBridge.getNativeId(e);
    }

    public static void remove() {
        CONTEXT.remove();
    }

    private static class TempIDState {
        private Entity[] frameSnapshot = new Entity[10000];
        private int currentIndex = 0;

        public void tickStart() {
            if (currentIndex > 0) Arrays.fill(frameSnapshot, 0, currentIndex, null);
            currentIndex = 0;
        }

        public void addEntity(Entity e) {
            if (currentIndex >= frameSnapshot.length) resize();
            int tempId = currentIndex++;
            frameSnapshot[tempId] = e;
            EntityAccessBridge.setNativeId(e, tempId);
        }

        public Entity getEntity(int id) {
            if (id < 0 || id >= currentIndex) return null;
            return frameSnapshot[id];
        }

        private void resize() {
            int newSize = frameSnapshot.length + (frameSnapshot.length >> 1);
            frameSnapshot = Arrays.copyOf(frameSnapshot, newSize);
        }
    }
}