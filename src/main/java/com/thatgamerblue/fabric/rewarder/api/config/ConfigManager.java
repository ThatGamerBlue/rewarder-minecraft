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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.thatgamerblue.fabric.rewarder.RewarderMod;
import com.thatgamerblue.fabric.rewarder.events.PlayerConnectCallback;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.api.FabricLoader;

@Log4j2
public class ConfigManager
{
	private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDirectory(), "rewarder.json");

	private final Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeHierarchyAdapter(Config.class, new InterfaceAdapter<Config>())
		.registerTypeAdapter(UUID.class, new UUIDAdapter())
		.disableHtmlEscaping().create();
	private final Map<UUID, Map<String, Config>> playerConfigMap;
	private final RewarderMod modInst;

	public ConfigManager(RewarderMod modInst)
	{
		this.modInst = modInst;

		if (!CONFIG_FILE.exists())
		{
			try
			{
				CONFIG_FILE.createNewFile();
			}
			catch (IOException e)
			{
				log.error("Failed to create config file, loading defaults.");
				e.printStackTrace();
				playerConfigMap = new HashMap<>();
				return;
			}
		}

		Map<UUID, Map<String, Config>> deferredConfigMap;
		try (FileReader fr = new FileReader(CONFIG_FILE))
		{
			//@formatter:off
			deferredConfigMap = gson.fromJson(fr, new TypeToken<Map<UUID, Map<String, Config>>>() {}.getType());
			//@formatter:on
		}
		catch (IOException e)
		{
			log.error("Failed to load config file, loading defaults.");
			e.printStackTrace();
			deferredConfigMap = new HashMap<>();
		}
		if (deferredConfigMap == null)
		{
			log.error("GSON failed to load config file, loading defaults.");
			deferredConfigMap = new HashMap<>();
		}
		playerConfigMap = deferredConfigMap;

		saveConfig();

		PlayerConnectCallback.EVENT.register((player) -> {
			if (playerConfigMap.containsKey(player.getGameProfile().getId()))
			{
				modInst.getRewardManager().initializePlayer(player.getGameProfile().getId());
			}
		});
	}

	// TODO: there should be a better way to get the clazz parameter here rather than needing it passed in
	public <T extends Config> T getObject(UUID player, String key, Class<T> clazz)
	{
		try
		{
			if (!playerConfigMap.containsKey(player))
			{
				playerConfigMap.put(player, new HashMap<>());
			}

			Map<String, Config> configMap = playerConfigMap.get(player);

			if (!configMap.containsKey(key))
			{
				try
				{
					configMap.put(key, clazz.newInstance());
				}
				catch (InstantiationException | IllegalAccessException e)
				{
					log.error("Failed to instantiate config object for with key " + key + " for " + clazz.getSimpleName() + ", expect crashes to occur.");
					e.printStackTrace();
					return null;
				}
			}

			return (T) configMap.get(key);
		}
		finally
		{
			saveConfig();
		}
	}

	public void saveConfig()
	{
		if (CONFIG_FILE.exists())
		{
			CONFIG_FILE.delete();
		}

		try
		{
			Files.write(CONFIG_FILE.toPath(), gson.toJson(playerConfigMap).getBytes(), StandardOpenOption.CREATE_NEW);
		}
		catch (IOException e)
		{
			log.error("Failed to write config file, you may lose data!");
			e.printStackTrace();
		}
	}

	private void initializeSavedPlayers()
	{
		for (Map.Entry<UUID, Map<String, Config>> entry : playerConfigMap.entrySet())
		{
			modInst.getRewardManager().initializePlayer(entry.getKey());
		}
	}

	static class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T>
	{
		private static final Gson internal = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String CLASS_NAME_ID = "clazzName";
		private static final String DATA_ID = "data";

		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			JsonObject object = json.getAsJsonObject();
			JsonPrimitive primitive = (JsonPrimitive) object.get(CLASS_NAME_ID);
			String clazzName = primitive.getAsString();
			Class clazz = getObjectClass(clazzName);

			if (clazz == null)
			{
				return null;
			}

			return internal.fromJson(object.get(DATA_ID), (Type) clazz);
		}

		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonObject object = new JsonObject();
			object.addProperty(CLASS_NAME_ID, src.getClass().getName());
			object.add(DATA_ID, internal.toJsonTree(src));
			return object;
		}

		private Class getObjectClass(String className)
		{
			try
			{
				return Class.forName(className);
			}
			catch (ClassNotFoundException ex)
			{
				log.error("Failed to locate class " + className + ", did you uninstall a module?");
				ex.printStackTrace();
			}
			return null;
		}
	}

	static class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID>
	{

		@Override
		public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			return UUID.fromString(json.getAsJsonPrimitive().getAsString());
		}

		@Override
		public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context)
		{
			return new JsonPrimitive(src.toString());
		}
	}
}
