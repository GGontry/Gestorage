package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polls the inspector key and requests the crosshair target's contents.
 *
 * A retry reuses the same request id, so a reply still in flight is never
 * invalidated by the next retry. Replies are cached by world and target so
 * looking back at a container is instant.
 */
public class InspectorTickHandler {
	/** Refresh interval for live targets such as hoppers. */
	private static final int REFRESH_TICKS = 5;
	/** Resend sooner when the previous request never got an answer. */
	private static final int RETRY_TICKS = 3;
	/** Stop waiting after this many ticks so the panel cannot stick on Loading. */
	private static final int TIMEOUT_TICKS = 20;
	private static final int CACHE_SIZE = 32;

	private static boolean prevPressed = false;
	private static boolean wasActive = false;
	private static String targetKey = "";
	private static int ticksSinceRequest = RETRY_TICKS;
	private static int ticksAwaiting = 0;
	private static boolean awaitingReply = false;
	private static String pendingKey = "";
	private static int requestId = 0;
	private static final Map<String, CachedInspection> cache = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedInspection> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	private InspectorTickHandler() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(InspectorTickHandler::tick);
	}

	private static void tick(MinecraftClient client) {
		if (!ModuleConfig.inspector().enabled()) {
			deactivate();
			return;
		}
		if (client.player == null || client.world == null || client.currentScreen != null) {
			deactivate();
			return;
		}
		long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
		if (handle == 0) return;

		String key = ModuleConfig.inspector().inspectKey();
		boolean pressed = key != null && !key.isEmpty() && KeybindHelper.isPressed(key, handle);
		if (ModuleConfig.inspector().toggleMode()) {
			if (pressed && !prevPressed) {
				ClientInspectorState.active = !ClientInspectorState.active;
			}
		} else {
			ClientInspectorState.active = pressed;
		}
		prevPressed = pressed;

		if (!ClientInspectorState.active) {
			deactivate();
			return;
		}
		boolean justActivated = !wasActive;
		wasActive = true;
		ticksSinceRequest++;
		if (awaitingReply) ticksAwaiting++;

		Target target = currentTarget(client);
		if (target == null) {
			targetKey = "";
			clearPending();
			ClientInspectorState.clear();
			return;
		}
		boolean changed = !target.key().equals(targetKey);
		if (changed) {
			targetKey = target.key();
			clearPending();
			ClientInspectorState.clear();
			CachedInspection cached = cache.get(targetKey);
			if (cached != null) {
				ClientInspectorState.apply(cached.title(), cached.stacks(), cached.info(), cached.icon());
			} else {
				ClientInspectorState.loading = true;
			}
		}

		boolean expired = awaitingReply && ticksAwaiting >= TIMEOUT_TICKS;
		if (expired) {
			clearPending();
			ClientInspectorState.loading = false;
		}
		int dueTicks = awaitingReply ? RETRY_TICKS : REFRESH_TICKS;
		if (justActivated || changed || expired || ticksSinceRequest >= dueTicks) {
			sendRequest(target, justActivated || changed || expired || !awaitingReply);
		}
	}

	/** Sends a request; retries of the same target keep the current request id. */
	private static void sendRequest(Target target, boolean fresh) {
		if (fresh) requestId++;
		int id = requestId;
		pendingKey = target.key();
		ClientInspectorState.expectedRequestId = id;
		awaitingReply = true;
		ticksSinceRequest = 0;
		if (fresh) ticksAwaiting = 0;

		boolean sent = ModNetworkingClient.sendInspectRequest(new ModNetworking.InspectRequestC2S(
				id, target.entity(), target.pos(), target.entityId()));
		if (!sent) clearPending();
	}

	/** Applies a reply, remembers it for this target and stops the fast retry. */
	public static void onReply(ModNetworking.InspectDataS2C payload) {
		if (!ClientInspectorState.active || pendingKey.isEmpty() || !pendingKey.equals(targetKey)) return;
		ClientInspectorState.apply(payload.title(), payload.stacks(), payload.infoLines(), payload.icon());
		cache.put(pendingKey, new CachedInspection(
				payload.title(), payload.stacks(), payload.infoLines(), payload.icon()));
		awaitingReply = false;
		ticksSinceRequest = 0;
		ticksAwaiting = 0;
		ClientInspectorState.expectedRequestId = -1;
	}

	private static void clearPending() {
		pendingKey = "";
		awaitingReply = false;
		ticksSinceRequest = RETRY_TICKS;
		ticksAwaiting = 0;
		ClientInspectorState.expectedRequestId = -1;
	}

	private static void deactivate() {
		ClientInspectorState.reset();
		prevPressed = false;
		wasActive = false;
		targetKey = "";
		clearPending();
	}

	/** World state belongs to a session, so old containers must not reappear on join. */
	public static void clearCache() {
		cache.clear();
		deactivate();
	}

	private static Target currentTarget(MinecraftClient client) {
		HitResult hit = effectiveCrosshair(client);
		String world = client.world.getRegistryKey().getValue().toString();
		if (hit instanceof EntityHitResult entityHit) {
			int id = entityHit.getEntity().getId();
			return new Target(true, BlockPos.ORIGIN, id, world + "|e:" + id);
		}
		if (hit instanceof BlockHitResult blockHit) {
			BlockPos pos = blockHit.getBlockPos();
			return new Target(false, pos, -1,
					world + "|b:" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
		}
		return null;
	}

	/**
	 * Tweakeroo's freecam has a "player inputs" option that forces vanilla's
	 * crosshair raycast back to the player, even while the camera is somewhere
	 * else. Whenever another camera entity is active, repeat the vanilla pick
	 * from that camera so the Inspector follows what the camera sees. No
	 * Tweakeroo classes are referenced: this only uses the vanilla camera entity.
	 */
	private static HitResult effectiveCrosshair(MinecraftClient client) {
		Entity camera = client.getCameraEntity();
		if (camera == null || camera == client.player) return client.crosshairTarget;
		return raycastFromCamera(client, camera);
	}

	/** Mirrors ClientPlayerEntity.getCrosshairTarget(), but from the camera entity. */
	private static HitResult raycastFromCamera(MinecraftClient client, Entity camera) {
		double reach = Math.max(client.player.getBlockInteractionRange(),
				client.player.getEntityInteractionRange());
		Vec3d start = camera.getCameraPosVec(1.0F);
		Vec3d direction = camera.getRotationVec(1.0F);
		HitResult blockHit = camera.raycast(reach, 1.0F, false);
		double blockDistanceSq = blockHit.getPos().squaredDistanceTo(start);
		double entityReachSq = Math.min(reach * reach, blockDistanceSq);
		Vec3d end = start.add(direction.multiply(reach));
		Box box = camera.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0, 1.0, 1.0);
		EntityHitResult entityHit = ProjectileUtil.raycast(camera, start, end, box,
				EntityPredicates.CAN_HIT, entityReachSq);
		if (entityHit != null && entityHit.getPos().squaredDistanceTo(start) < blockDistanceSq) {
			return entityHit;
		}
		return blockHit;
	}

	private record Target(boolean entity, BlockPos pos, int entityId, String key) {}

	private record CachedInspection(String title, List<ItemStack> stacks, List<String> info, ItemStack icon) {}
}
