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
package com.thatgamerblue.fabric.rewarder.api.sources;

import com.thatgamerblue.fabric.rewarder.api.config.ConfigManager;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardManager;
import java.io.IOException;
import java.util.UUID;

public abstract class RewardSource
{
	protected final RewardManager rewardManager;
	protected final ConfigManager configManager;
	protected final UUID player;

	public RewardSource(RewardManager rewardManager, ConfigManager configManager, UUID player)
	{
		this.rewardManager = rewardManager;
		this.configManager = configManager;
		this.player = player;
	}

	public abstract boolean shouldConnect();

	public abstract void connect() throws IOException;

	public abstract void disconnect() throws IOException;
}
