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
package com.thatgamerblue.fabric.rewarder.modules.streamloots;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Value;

@Value
public class StreamlootsEvent
{
	String message;
	String type;
	StreamlootsData data;

	public String getCardName()
	{
		return data.getCardName();
	}

	public String getSender()
	{
		for (StreamlootsData.StreamlootsDataField dataField : data.getFields())
		{
			if (dataField.getName().equals("username"))
			{
				return dataField.getValue();
			}
		}
		return "";
	}

	@Value
	public static class StreamlootsData
	{
		String cardName;
		List<StreamlootsDataField> fields;
		String type;

		@Value
		public static class StreamlootsDataField
		{
			String name;
			String value;
		}
	}
}
