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
package com.thatgamerblue.fabric.rewarder.api.config;

import com.google.common.collect.Lists;
import com.thatgamerblue.fabric.rewarder.api.rewards.SerializedReward;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public abstract class Config
{
	@Getter
	@Setter
	private String _id;

	@Getter
	private Map<String, List<SerializedReward>> rewards = new HashMap<String, List<SerializedReward>>()
	{{
		put("F in chat", Lists.newArrayList(
			new SerializedReward("message", new HashMap<String, String>()
			{{
				put("message", "Press F to pay respects.");
			}}),
			new SerializedReward("entity", new HashMap<String, String>()
			{{
				put("delay", "5000");
				put("entity", "minecraft:creeper");
			}}),
			new SerializedReward("block", new HashMap<String, String>()
			{{
				put("block", "minecraft:anvil[facing=south]");
			}}),
			new SerializedReward("item", new HashMap<String, String>()
			{{
				put("item", "minecraft:iron_pickaxe{display:{Name:\"[{\\\"text\\\":\\\"Pick of Destiny\\\",\\\"italic\\\":false}]\"}}");
			}})
		));
	}};
}
