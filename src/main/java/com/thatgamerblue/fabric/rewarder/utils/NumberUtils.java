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

package com.thatgamerblue.fabric.rewarder.utils;

public class NumberUtils
{
	public static long parseLongSafe(String s)
	{
		return parseLongSafe(s, 0);
	}

	public static long parseLongSafe(String s, long failure)
	{
		try
		{
			return Long.parseLong(s);
		}
		catch (NumberFormatException ex)
		{
			return failure;
		}
	}

	public static int parseIntSafe(String s)
	{
		return parseIntSafe(s, 0);
	}

	public static int parseIntSafe(String s, int failure)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (NumberFormatException ex)
		{
			return failure;
		}
	}
}
