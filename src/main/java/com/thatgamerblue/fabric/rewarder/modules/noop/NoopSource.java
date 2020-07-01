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

package com.thatgamerblue.fabric.rewarder.modules.noop;

import com.thatgamerblue.fabric.rewarder.api.config.ConfigManager;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardManager;
import com.thatgamerblue.fabric.rewarder.api.sources.RewardSource;
import java.io.IOException;
import java.util.UUID;

// DO NOT REGISTER THIS REWARDSOURCE, THE POINT OF THIS IS TO BE A FALLBACK FOR PROBLEMATIC SOURCES
public class NoopSource extends RewardSource
{
	public NoopSource(RewardManager rewardManager, ConfigManager configManager, UUID player)
	{
		super(rewardManager, configManager, player);
	}

	@Override
	public boolean shouldConnect()
	{
		return false;
	}

	@Override
	public void connect() throws IOException
	{

	}

	@Override
	public void disconnect() throws IOException
	{

	}
}
