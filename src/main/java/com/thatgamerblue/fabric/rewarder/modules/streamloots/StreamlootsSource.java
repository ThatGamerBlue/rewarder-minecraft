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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.thatgamerblue.fabric.rewarder.api.config.ConfigManager;
import com.thatgamerblue.fabric.rewarder.api.rewards.RewardManager;
import com.thatgamerblue.fabric.rewarder.api.rewards.SerializedReward;
import com.thatgamerblue.fabric.rewarder.api.sources.RewardSource;
import java.io.IOException;
import java.nio.CharBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

@Log4j2
public class StreamlootsSource extends RewardSource
{
	private final Gson gson = new Gson();
	private final StreamlootsConfig config;

	private CloseableHttpAsyncClient httpClient;
	private String apiKey;
	private long connectAt;
	private boolean disableSsl = false;
	private SSLContext noopSslContext;
	private boolean disabled = false;

	public StreamlootsSource(RewardManager rewardManager, ConfigManager configManager, UUID player)
	{
		super(rewardManager, configManager, player);
		config = configManager.getObject(player, "streamloots", StreamlootsConfig.class);
		apiKey = null;
		try
		{
			noopSslContext = SSLContext.getInstance("SSL");
			noopSslContext.init(null, new TrustManager[]{new X509TrustManager()
			{
				public X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType)
				{
				}

				public void checkServerTrusted(X509Certificate[] certs,
					String authType)
				{
				}
			}}, new SecureRandom());
		}
		catch (NoSuchAlgorithmException | KeyManagementException e)
		{
			log.error("Failed to initialize noop ssl context");
			noopSslContext = null;
		}
	}

	@Override
	public boolean shouldConnect()
	{
		return (config.getApiKey() != null && apiKey == null) || (apiKey != null && !apiKey.equals(config.getApiKey())) && !disabled;
	}

	@Override
	public void connect() throws IOException
	{
		if (connectAt > System.currentTimeMillis())
		{
			apiKey = null;
			return;
		}

		apiKey = config.getApiKey();

		try
		{
			actualConnect();
		}
		catch (IOException ex)
		{
			log.error("Failed to connect to streamloots api, retrying in 30 seconds");
			connectAt = System.currentTimeMillis() + (1000 * 30);
			apiKey = null;
			throw ex;
		}
	}

	@Override
	public void disconnect() throws IOException
	{
		if (httpClient != null)
		{
			httpClient.close();
			httpClient = null;
		}
	}

	// TODO: write a test for this
	@VisibleForTesting
	public void handleReceivedEvent(String recvBuffer)
	{
		String process = recvBuffer.trim();
		log.debug("received useful data: " + process);
		process = process.substring(process.indexOf("{")).trim();
		StreamlootsEvent event = gson.fromJson(process, StreamlootsEvent.class);

		if (event.getData().getType().equals("redemption"))
		{
			if (config.getRewards().containsKey(event.getCardName()))
			{
				List<SerializedReward> rewards = config.getRewards().get(event.getCardName());
				rewards.stream().map(rewardManager::deserializeReward).forEach(r -> rewardManager.queueReward(player, r));
			}
		}
	}

	private void actualConnect() throws IOException
	{
		disconnect();

		if (apiKey == null || apiKey.trim().isEmpty())
		{
			log.error("Provided api key is empty, skipping connect");
			return;
		}

		if (disableSsl)
		{
			if (noopSslContext == null)
			{
				log.error("noopSslContext is null, disabling streamloots integration.");
				disabled = true;
				return;
			}

			httpClient = HttpAsyncClients
				.custom()
				.setSSLContext(noopSslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		}
		else
		{
			httpClient = HttpAsyncClients.createDefault();
		}
		httpClient.start();

		HttpGet request = new HttpGet("https://widgets.streamloots.com/alerts/" + apiKey + "/media-stream");
		request.addHeader("accept", "text/event-stream");

		HttpAsyncRequestProducer producer = HttpAsyncMethods.create(request);

		AsyncCharConsumer<HttpResponse> consumer3 = new AsyncCharConsumer<HttpResponse>()
		{
			private HttpResponse response;
			private String recvBuffer;

			@Override
			protected void onResponseReceived(final HttpResponse response)
			{
				this.response = response;
			}

			@Override
			protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl)
			{
				while (buf.hasRemaining())
				{
					recvBuffer += buf.get();
				}

				if (recvBuffer.endsWith("\n\n"))
				{
					handleReceivedEvent(recvBuffer);

					recvBuffer = "";
				}
				else if (!recvBuffer.startsWith("data"))
				{
					log.debug("received garbage or heartbeat: " + recvBuffer);
					recvBuffer = "";
				}
			}

			@Override
			protected void releaseResources()
			{
				recvBuffer = null;
			}

			@Override
			protected HttpResponse buildResult(final HttpContext context)
			{
				return this.response;
			}

		};

		httpClient.execute(producer, consumer3, new FutureCallback<HttpResponse>()
		{
			public void completed(final HttpResponse response3)
			{
				log.info("Connection to streamloots closed remotely, attempting reconnection in 30 seconds");
				apiKey = null;
				connectAt = System.currentTimeMillis() + (1000 * 30);
			}

			public void failed(final Exception ex)
			{
				log.error("Failed to connect to streamloots, see exception for details.");
				ex.printStackTrace();
				if (ex instanceof SSLHandshakeException)
				{
					log.error("Outdated java version, disabling SSL encryption.");
					disableSsl = true;
					apiKey = null;
				}
			}

			public void cancelled()
			{
				// noop
			}
		});

		producer.close();
	}
}
