package com.gontry.gestorage;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;

/**
 * Codec shared by the per-player persisted states that store item stacks.
 *
 * <p>Every state decodes each stored item independently: an unreadable item (for
 * example an id from a removed mod) is skipped with a warning instead of failing
 * the whole state. {@link ItemStack#CODEC} is strict, so this wrapper keeps that
 * behaviour on top of it. It is a {@link MapCodec} so an item's own keys
 * ({@code id}, {@code count}, {@code components}) merge into the surrounding
 * record instead of nesting under an extra field.
 */
public final class StackCodecs {
	private StackCodecs() {}

	/** Encodes exactly like {@link ItemStack#CODEC}; unreadable input decodes as {@link ItemStack#EMPTY}. */
	public static final MapCodec<ItemStack> TOLERANT = MapCodec.assumeMapUnsafe(new Codec<ItemStack>() {
		@Override
		public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
			return ItemStack.CODEC.decode(ops, input)
					.resultOrPartial(error -> Gestorage.LOGGER.warn("[Gestorage] Skipped unreadable stored item: {}", error))
					.map(DataResult::success)
					.orElseGet(() -> DataResult.success(Pair.of(ItemStack.EMPTY, input)));
		}

		@Override
		public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
			return ItemStack.CODEC.encode(stack, ops, prefix);
		}
	});
}
