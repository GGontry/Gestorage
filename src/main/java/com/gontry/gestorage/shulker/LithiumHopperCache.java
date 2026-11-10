package com.gontry.gestorage.shulker;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks Lithium's hopper stack lists so their cached {@code fullSlots} count can
 * be recomputed when a shulker rule changes.
 *
 * <p>Lithium only recomputes that count when an inventory changes. Changing
 * {@code gestorage:stackableShulkers} or {@code gestorage:hopperStackableShulkers}
 * changes the effective max count without touching any inventory, so a cache
 * filled under the old rule can make a hopper believe the destination is still
 * full (or not full) until something else mutates it.
 *
 * <p>Lithium is optional, so the list type is never referenced directly: the
 * compatibility mixin registers instances as plain objects and this class calls
 * the public {@code changedALot()} method reflectively. Identity comparisons and
 * weak references are used because the lists compare by content, which changes
 * as items move.
 */
public final class LithiumHopperCache {
	private static final List<WeakReference<Object>> LISTS = new ArrayList<>();

	private LithiumHopperCache() {}

	public static synchronized void register(Object stackList) {
		for (Iterator<WeakReference<Object>> iterator = LISTS.iterator(); iterator.hasNext(); ) {
			Object existing = iterator.next().get();
			if (existing == null) {
				iterator.remove();
			} else if (existing == stackList) {
				return;
			}
		}
		LISTS.add(new WeakReference<>(stackList));
	}

	public static void invalidateAll() {
		List<Object> snapshot = new ArrayList<>();
		synchronized (LithiumHopperCache.class) {
			for (Iterator<WeakReference<Object>> iterator = LISTS.iterator(); iterator.hasNext(); ) {
				Object stackList = iterator.next().get();
				if (stackList == null) {
					iterator.remove();
				} else {
					snapshot.add(stackList);
				}
			}
		}

		for (Object stackList : snapshot) {
			try {
				Method changedALot = stackList.getClass().getMethod("changedALot");
				changedALot.invoke(stackList);
			} catch (ReflectiveOperationException | SecurityException e) {
				// Not a Lithium list (or an incompatible version): stop tracking it.
				synchronized (LithiumHopperCache.class) {
					LISTS.removeIf(reference -> reference.get() == stackList);
				}
			}
		}
	}
}
