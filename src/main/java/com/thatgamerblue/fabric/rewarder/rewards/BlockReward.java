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

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardDeserializer;
import com.thatgamerblue.fabric.rewarder.api.rewards.TimeableReward;
import com.thatgamerblue.fabric.rewarder.utils.NumberUtils;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.command.arguments.BlockArgumentParser;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

@Log4j2
public class BlockReward extends TimeableReward
{
	private static final Set<Material> REPLACEABLE_MATERIALS = ImmutableSet.of(
		Material.AIR,
		Material.REPLACEABLE_PLANT,
		Material.REPLACEABLE_UNDERWATER_PLANT,
		Material.WATER
	);

	private final BlockState blockState;

	public BlockReward(long timestamp, BlockState blockState)
	{
		super(timestamp);
		this.blockState = blockState;
	}

	@Override
	public void execute(ServerWorld world)
	{
		world.getPlayers().forEach(player -> {
			if (blockState == null)
			{
				player.sendMessage(new LiteralText("ERROR: Block parsed incorrectly in BlockReward! Did you make a typo?"), false);
				return;
			}

			BlockPos pos = player.getBlockPos();
			if (!world.canSetBlock(pos))
			{
				player.sendMessage(new LiteralText("You missed a " + blockState.toString() + " by being out of the world."), false);
				return;
			}

			Material oldMaterial = world.getBlockState(pos).getMaterial();
			if (REPLACEABLE_MATERIALS.contains(oldMaterial))
			{
				world.setBlockState(pos, blockState);
			}
			else
			{
				player.sendMessage(new LiteralText("Please place this block as soon as possible"), false);
				world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(blockState.getBlock().asItem(), 1)));
			}
		});
	}

	public static class Deserializer implements RewardDeserializer<BlockReward>
	{
		@Override
		public BlockReward deserialize(Map<String, String> data)
		{
			long delay = data.containsKey("delay") ? NumberUtils.parseLongSafe(data.get("delay")) : 0;
			long timestamp = System.currentTimeMillis() + delay;
			String block = data.getOrDefault("block", "MISSING BLOCK ID");
			BlockState state;
			try
			{
				BlockArgumentParser parser = new BlockArgumentParser(new StringReader(block), false).parse(true);
				state = parser.getBlockState();
			}
			catch (CommandSyntaxException e)
			{
				log.error("Failed to parse block state: " + block + ", defaulting to null");
				e.printStackTrace();
				state = null;
			}
			return new BlockReward(timestamp, state);
		}
	}
}
