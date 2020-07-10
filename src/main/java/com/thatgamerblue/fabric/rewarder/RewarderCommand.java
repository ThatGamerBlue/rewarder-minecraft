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

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.command.arguments.GameProfileArgumentType;
import static net.minecraft.server.command.CommandManager.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class RewarderCommand
{
	private static RewarderMod modInst;

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, RewarderMod modInst)
	{
		RewarderCommand.modInst = modInst;
		dispatcher.register(
			literal("rewarder")
			.requires(scs -> scs.hasPermissionLevel(2))
			.then(literal("register")
				.then(argument("player", GameProfileArgumentType.gameProfile())
					.executes(scs -> execute(scs.getSource(), ExecutionMode.REGISTER, GameProfileArgumentType.getProfileArgument(scs, "player")))
				)
			)
		);
	}

	private static int execute(ServerCommandSource scs, ExecutionMode toExec, Collection<GameProfile> targets)
	{
		switch (toExec)
		{
			case REGISTER:
				try
				{
					targets.forEach(p -> modInst.getRewardManager().initializePlayer(p.getId()));
					scs.sendFeedback(new LiteralText("Successfully registered on rewarder, please close your game and configure."), false);
				}
				catch (IllegalArgumentException ex)
				{
					scs.sendFeedback(new LiteralText("Invalid player!"), false);
				}
				break;
			default:
				scs.sendFeedback(new LiteralText("Unknown parameter: " + toExec), false);
				break;
		}
		return 1;
	}

	private enum ExecutionMode
	{
		REGISTER;
	}
}
