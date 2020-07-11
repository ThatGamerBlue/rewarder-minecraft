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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public class MessageReward extends TimeableReward
{
	private final String message;
	private final boolean actionBar;

	public MessageReward(long timestamp, String message, boolean actionBar)
	{
		super(timestamp);
		this.message = message;
		this.actionBar = actionBar;
	}

	@Override
	public void execute(ServerWorld world, ServerPlayerEntity player)
	{
		player.sendMessage(new LiteralText(message), actionBar);
	}

	public static class Deserializer implements RewardDeserializer<MessageReward>
	{
		@Override
		public MessageReward deserialize(Map<String, String> data)
		{
			long offset = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = offset + System.currentTimeMillis();
			String message = data.getOrDefault("message", "MESSAGE ATTRIBUTE MISSING IN MessageReward");
			boolean actionBar = data.containsKey("actionbar");

			return new MessageReward(timestamp, message, actionBar);
		}
	}
}