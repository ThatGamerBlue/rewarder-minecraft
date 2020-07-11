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

import com.thatgamerblue.fabric.rewarder.api.rewards.RewardDeserializer;
import com.thatgamerblue.fabric.rewarder.api.rewards.TimeableReward;
import com.thatgamerblue.fabric.rewarder.utils.NumberUtils;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

@Log4j2
public class EntityReward extends TimeableReward
{
	private final String entityName;

	public EntityReward(long timestamp, String entityName)
	{
		super(timestamp);
		this.entityName = entityName;
	}

	@Override
	public void execute(ServerWorld world, ServerPlayerEntity player)
	{
		// TODO: add support for nbt and player position offsets
		Optional<EntityType<?>> entityTypeOptional = EntityType.get(entityName);
		if (!entityTypeOptional.isPresent())
		{
			log.error("Entity type " + entityName + " doesn't exist. Maybe you made a typo?");
		}

		if (!entityTypeOptional.isPresent())
		{
			player.sendMessage(new LiteralText("ERROR: Entity type " + entityName + " doesn't exist"), false);
			return;
		}
		Entity entity = entityTypeOptional.get().create(world);
		if (entity == null)
		{
			// something very very bad has happened in the game
			log.error("Failed spawning entity " + entityName + " for player " + player.getName().asString());
			return;
		}
		entity.updatePosition(player.getX(), player.getY(), player.getZ());
		world.spawnEntity(entity);
	}

	public static class Deserializer implements RewardDeserializer<EntityReward>
	{
		@Override
		public EntityReward deserialize(Map<String, String> data)
		{
			// TODO: implement deserializer from summon command or find a better way to do nbt
			long offset = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = offset + System.currentTimeMillis();
			String entityId = data.getOrDefault("entity", "MISSING ENTITY ID");

			return new EntityReward(timestamp, entityId);
		}
	}
}
