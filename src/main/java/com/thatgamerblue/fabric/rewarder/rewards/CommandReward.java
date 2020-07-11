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
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

@Log4j2
public class CommandReward extends TimeableReward
{
	private final String command;

	public CommandReward(long timestamp, String command)
	{
		super(timestamp);
		this.command = command;
	}

	@Override
	public void execute(ServerWorld world, ServerPlayerEntity player)
	{
		String localCommand = command.replace("{{player}}", player.getGameProfile().getName());
		world.getServer().getCommandManager().execute(world.getServer().getCommandSource(), localCommand);
	}

	public static class Deserializer implements RewardDeserializer<CommandReward>
	{
		@Override
		public CommandReward deserialize(Map<String, String> data)
		{
			long delay = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = System.currentTimeMillis() + delay;
			String command = data.getOrDefault("command", "tell {{player}} Command missing in command reward");
			return new CommandReward(timestamp, command);
		}
	}
}
