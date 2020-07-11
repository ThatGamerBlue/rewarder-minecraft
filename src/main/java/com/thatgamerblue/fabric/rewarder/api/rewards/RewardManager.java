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
package com.thatgamerblue.fabric.rewarder.api.rewards;

import com.google.common.annotations.VisibleForTesting;
import com.thatgamerblue.fabric.rewarder.RewarderMod;
import com.thatgamerblue.fabric.rewarder.api.config.ConfigManager;
import com.thatgamerblue.fabric.rewarder.api.sources.RewardSource;
import com.thatgamerblue.fabric.rewarder.modules.noop.NoopSource;
import com.thatgamerblue.fabric.rewarder.rewards.MessageReward;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

@Log4j2
public class RewardManager
{
	private final RewarderMod modInst;

	private final HashMap<String, RewardDeserializer<? extends Reward>> rewardDeserializers = new HashMap<>();

	private final Set<Class<? extends RewardSource>> rewardSourceRegistry = new HashSet<>();
	private final Map<UUID, List<RewardSource>> activeRewardSources = new HashMap<>();

	private final Map<UUID, PriorityBlockingQueue<Reward>> playerRewardQueues = new HashMap<>();

	private int tickCount = 0;

	public RewardManager(RewarderMod modInst)
	{
		this.modInst = modInst;

		ServerTickCallback.EVENT.register(this::tickEvents);
	}

	//region Reward Source Registry
	public void registerRewardSource(Class<? extends RewardSource> rewardSource)
	{
		synchronized (rewardSourceRegistry)
		{
			rewardSourceRegistry.add(rewardSource);
		}
	}

	public void deregisterRewardSource(Class<? extends RewardSource> rewardSource)
	{
		synchronized (activeRewardSources)
		{
			for (Iterator<Map.Entry<UUID, List<RewardSource>>> iterator = activeRewardSources.entrySet().iterator(); iterator.hasNext(); )
			{
				Map.Entry<UUID, List<RewardSource>> entry = iterator.next();
				List<RewardSource> v = entry.getValue();
				for (RewardSource rs : v)
				{
					if (rs.getClass().equals(rewardSource))
					{
						try
						{
							rs.disconnect();
						}
						catch (IOException e)
						{
							log.error("Failed to disconnect from reward source " + v.getClass().getSimpleName() + "!");
							e.printStackTrace();
						}
						finally
						{
							iterator.remove();
						}
					}
				}
			}
		}
	}

	private RewardSource instantiateRewardSource(Class<? extends RewardSource> clazz, UUID uuid)
	{
		try
		{
			return clazz.getConstructor(RewardManager.class, ConfigManager.class, UUID.class).newInstance(this, modInst.getConfigManager(), uuid);
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
		{
			log.error("Failed to instantiate reward source " + clazz.getSimpleName() + ", falling back to noop");
			e.printStackTrace();
		}
		return new NoopSource(this, modInst.getConfigManager(), uuid);
	}

	@VisibleForTesting
	public <T extends RewardSource> T getRewardSource(Class<T> clazz, UUID player)
	{
		synchronized (activeRewardSources)
		{
			List<RewardSource> sources = activeRewardSources.get(player);

			if (sources == null)
			{
				return null;
			}

			return (T) sources.stream().filter(o -> o.getClass() == clazz).findFirst().orElseGet(null);
		}
	}
	//endregion

	//region Reward Type Registry
	public void registerRewardDeserializer(String rewardId, RewardDeserializer<?> rewardDeserializer)
	{
		rewardId = rewardId.toLowerCase();
		if (rewardDeserializers.containsKey(rewardId))
		{
			throw new IllegalStateException("Already have a deserializer mapped for id " + rewardId + "!");
		}

		rewardDeserializers.put(rewardId, rewardDeserializer);
	}

	public Reward deserializeReward(SerializedReward serializedReward)
	{
		return deserializeReward(serializedReward.getId(), serializedReward.getData());
	}

	public Reward deserializeReward(String rewardId, Map<String, String> data)
	{
		rewardId = rewardId.toLowerCase();

		if (!rewardDeserializers.containsKey(rewardId))
		{
			log.error("Reward ID " + rewardId + " is missing a deserializer, perhaps you made a typo?");
			return new MessageReward(0, "ERROR: " + rewardId + " is not a recognised reward type.", false);
		}

		return rewardDeserializers.get(rewardId).deserialize(data);
	}
	//endregion

	public void queueReward(UUID uuid, Reward reward)
	{
		if (!playerRewardQueues.containsKey(uuid))
		{
			// if this happens something has gone completely fucking wrong somewhere
			log.error("Received reward for player " + uuid.toString() + " without having them registered! What the fuck?!?!");
			return;
		}

		playerRewardQueues.get(uuid).add(reward);
	}

	public void initializePlayer(UUID uuid)
	{
		synchronized (activeRewardSources)
		{
			if (activeRewardSources.containsKey(uuid))
			{
				// player is already initialized
				return;
			}
		}

		PriorityBlockingQueue<Reward> rewardQueue = createRewardQueue();
		List<RewardSource> rewardSources = new ArrayList<>();

		synchronized (rewardSourceRegistry)
		{
			rewardSourceRegistry.forEach(r -> rewardSources.add(instantiateRewardSource(r, uuid)));
		}

		activeRewardSources.put(uuid, rewardSources);

		synchronized (activeRewardSources)
		{
			playerRewardQueues.put(uuid, rewardQueue);
		}
	}

	private PriorityBlockingQueue<Reward> createRewardQueue()
	{
		return new PriorityBlockingQueue<>(16, (o1, o2) -> Boolean.compare(o1.canExecute(), o2.canExecute()));
	}

	private void tickEvents(MinecraftServer server)
	{
		if (tickCount++ % 20 == 0)
		{
			synchronized (activeRewardSources)
			{
				for (Map.Entry<UUID, List<RewardSource>> entry : activeRewardSources.entrySet())
				{
					List<RewardSource> rewardSourceList = entry.getValue();
					for (RewardSource rs : rewardSourceList)
					{
						if (rs.shouldConnect())
						{
							connect(rs);
						}
					}
				}
			}
		}

		for (Map.Entry<UUID, PriorityBlockingQueue<Reward>> entry : playerRewardQueues.entrySet())
		{
			UUID k = entry.getKey();
			PriorityBlockingQueue<Reward> v = entry.getValue();
			ServerPlayerEntity playerEntity;
			if ((playerEntity = server.getPlayerManager().getPlayer(k)) == null)
			{
				// player is offline
				continue;
			}

			for (Iterator<Reward> iterator = v.iterator(); iterator.hasNext(); )
			{
				Reward reward = iterator.next();

				if (reward.canExecute())
				{
					reward.execute(playerEntity.getServerWorld(), playerEntity);
					iterator.remove();
				}
			}
		}
	}

	private void connect(RewardSource rewardSource)
	{
		try
		{
			rewardSource.connect();
		}
		catch (IOException e)
		{
			log.error("Failed to connect to reward source " + rewardSource.getClass().getCanonicalName() + ", funky things may occur");
			e.printStackTrace();
		}
	}

	@VisibleForTesting
	public int getQueueLength()
	{
		synchronized (activeRewardSources)
		{
			int i = 0;
			for (Map.Entry<UUID, List<RewardSource>> entry : activeRewardSources.entrySet())
			{
				List<RewardSource> v = entry.getValue();
				i += v.size();
			}
			return 1;
		}
	}
}
