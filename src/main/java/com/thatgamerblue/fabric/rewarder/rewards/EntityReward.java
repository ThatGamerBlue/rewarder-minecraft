/*
 * Copyright (c) 2020 ThatGamerBlue
 * This file is part of Rewarder <https://github.com/thatgamerblue/rewarder>
 *
 * Rewarder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rewarder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rewarder.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.thatgamerblue.fabric.rewarder.rewards;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardDeserializer;
import com.thatgamerblue.fabric.rewarder.api.rewards.TimeableReward;
import com.thatgamerblue.fabric.rewarder.utils.NumberUtils;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import net.minecraft.command.arguments.EntitySummonArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@Log4j2
public class EntityReward extends TimeableReward
{
	private final Identifier entityId;
	private final CompoundTag nbtData;
	private final boolean hasNbt;

	public EntityReward(long timestamp, Identifier entityId, CompoundTag tag, boolean hasNbt)
	{
		super(timestamp);
		this.entityId = entityId;
		this.nbtData = tag;
		this.hasNbt = hasNbt;
	}

	@Override
	public void execute(ServerWorld world, ServerPlayerEntity player)
	{
		// TODO: add support for player position offsets
		String entityIdString = entityId.toString();
		if (entityIdString.startsWith("rewarder:"))
		{
			boolean missing = entityIdString.contains("missing");
			player.sendMessage(new LiteralText("ERROR: Entity ID is " + (missing ? "missing" : "invalid (" + entityIdString + ")") + ", please check your config"), false);
			return;
		}

		CompoundTag finalNbt = nbtData.copy();
		finalNbt.putString("id", entityId.toString());
		Entity entity = EntityType.loadEntityWithPassengers(finalNbt, world, (ent) ->
		{
			ent.refreshPositionAndAngles(player.getBlockPos(), ent.getYaw(1.0f), ent.getPitch(1.0f));
			return world.tryLoadEntity(ent) ? ent : null;
		});

		if (entity == null)
		{
			player.sendMessage(new LiteralText("ERROR: Failed to spawn entity: " + entityIdString), false);
			return;
		}

		if (hasNbt && entity instanceof MobEntity)
		{
			((MobEntity) entity).initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.TRIGGERED, null, null);
		}
	}

	static class EntitySpawnArgumentType
	{
		private static Identifier validate(Identifier identifier) throws CommandSyntaxException
		{
			Registry.ENTITY_TYPE.getOrEmpty(identifier).orElseThrow(() -> EntitySummonArgumentType.NOT_FOUND_EXCEPTION.create(identifier));
			return identifier;
		}

		public static Identifier parse(String string) throws CommandSyntaxException
		{
			return validate(Identifier.fromCommandInput(new StringReader(string)));
		}
	}

	public static class Deserializer implements RewardDeserializer<EntityReward>
	{
		@Override
		public EntityReward deserialize(Map<String, String> data)
		{
			// deserialize time
			long offset = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = offset + System.currentTimeMillis();

			// deserialize entity id
			String entityId = data.getOrDefault("entity", "");
			Identifier identifier;
			try
			{
				identifier = EntitySpawnArgumentType.parse(entityId);
			}
			catch (CommandSyntaxException e)
			{
				log.error("Failed to parse entity ID, will error when executed");
				e.printStackTrace();
				identifier = new Identifier(entityId.isEmpty() ? "rewarder:missing_entity_id" : "rewarder:invalid_entity_id");
			}

			// deserialize nbt
			String serializedNbt = data.getOrDefault("nbt", "{}");
			CompoundTag nbtTag;
			boolean hasNbt = false;
			try
			{
				nbtTag = new StringNbtReader(new StringReader(serializedNbt)).parseCompoundTag();
				hasNbt = true;
			}
			catch (CommandSyntaxException e)
			{
				log.error("Failed to parse NBT Compound tag, resetting to default");
				e.printStackTrace();
				nbtTag = new CompoundTag();
			}
			return new EntityReward(timestamp, identifier, nbtTag, hasNbt);
		}
	}
}
