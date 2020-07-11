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
package com.thatgamerblue.fabric.rewarder;

import com.thatgamerblue.fabric.rewarder.api.config.ConfigManager;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardManager;
import com.thatgamerblue.fabric.rewarder.modules.streamloots.StreamlootsSource;
import com.thatgamerblue.fabric.rewarder.rewards.BlockReward;
import com.thatgamerblue.fabric.rewarder.rewards.CommandReward;
import com.thatgamerblue.fabric.rewarder.rewards.EntityReward;
import com.thatgamerblue.fabric.rewarder.rewards.ItemReward;
import com.thatgamerblue.fabric.rewarder.rewards.MessageReward;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

@Log4j2
public class RewarderMod implements ModInitializer
{
	@Getter
	private RewardManager rewardManager;

	@Getter
	private ConfigManager configManager;

	@Override
	public void onInitialize()
	{
		rewardManager = new RewardManager(this);

		configManager = new ConfigManager(this);

		rewardManager.registerRewardSource(StreamlootsSource.class);

		rewardManager.registerRewardDeserializer("command", new CommandReward.Deserializer());
		rewardManager.registerRewardDeserializer("message", new MessageReward.Deserializer());
		rewardManager.registerRewardDeserializer("entity", new EntityReward.Deserializer());
		rewardManager.registerRewardDeserializer("block", new BlockReward.Deserializer());
		rewardManager.registerRewardDeserializer("item", new ItemReward.Deserializer());

		CommandRegistrationCallback.EVENT.register((dispatcher, _ignored) -> {
			RewarderCommand.register(dispatcher, this);
		});
	}
}
