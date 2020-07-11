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
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

@Log4j2
public class ItemReward extends TimeableReward
{
	private final ItemStack stack;

	public ItemReward(long timestamp, ItemStack stack)
	{
		super(timestamp);
		this.stack = stack;
	}

	@Override
	public void execute(ServerWorld world, ServerPlayerEntity player)
	{
		if (stack == null)
		{
			player.sendMessage(new LiteralText("ERROR: Item parsed incorrectly in ItemReward! Did you make a typo?"), false);
			return;
		}

		world.spawnEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack));
	}

	public static class Deserializer implements RewardDeserializer<ItemReward>
	{
		@Override
		public ItemReward deserialize(Map<String, String> data)
		{
			long delay = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = System.currentTimeMillis() + delay;
			String item = data.getOrDefault("item", "MISSING ITEM ID");
			ItemStack itemStack;
			int count = data.containsKey("count") ? NumberUtils.parseIntSafe(data.get("count"), 1) : 1;
			try
			{
				ItemStackArgumentType parser = ItemStackArgumentType.itemStack();
				itemStack = parser.parse(new StringReader(item)).createStack(count, false);
			}
			catch (CommandSyntaxException e)
			{
				log.error("Failed to parse item id: " + item + ", defaulting to null");
				e.printStackTrace();
				itemStack = null;
			}
			return new ItemReward(timestamp, itemStack);
		}
	}
}
