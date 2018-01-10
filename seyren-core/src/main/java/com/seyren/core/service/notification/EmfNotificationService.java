package com.seyren.core.service.notification;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;
import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.domain.SubscriptionType;
import com.seyren.core.exception.NotificationFailedException;
import com.seyren.core.util.config.SeyrenConfig;

//ONLY difference in AWS and CH EMF apart from having different end points is that an authentication token is required for the former.

@Named
public class EmfNotificationService implements NotificationService {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EmfNotificationService.class);
	private final SeyrenConfig seyrenConfig;

	@Inject
	public EmfNotificationService(SeyrenConfig seyrenConfig) {
		this.seyrenConfig = seyrenConfig;
	}

	@Override
	public boolean canHandle(SubscriptionType subscriptionType) {
		return (subscriptionType == SubscriptionType.DC_EMF || subscriptionType == SubscriptionType.AWS_EMF);
	}

	@Override
	public void sendNotification(Check check, Subscription subscription, List<Alert> alerts)
			throws NotificationFailedException {
		HttpClient client = HttpClientBuilder.create().useSystemProperties().build();
		HttpPost post;

		String emfUrl = seyrenConfig.getEmfUrl();
		if (StringUtils.isNotBlank(emfUrl)) {
			post = new HttpPost(emfUrl);
		} else {
			LOGGER.warn("EMF API URL in Seyren Config needs to be set before sending notifications to EMF");
			return;
		}

		post.setHeader("Accept", "application/json");
		post.setHeader("Content-type", "application/json");

		if (subscription.getType() == SubscriptionType.AWS_EMF) {
			post.setHeader("Ocp-Apim-Subscription-Key", seyrenConfig.getEmfSubKey());
		}

		List<BasicNameValuePair> parameters = getParameters(check);

		try {
			post.setEntity(new UrlEncodedFormEntity(parameters));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.info("> parameters: {}", parameters);
			}
			HttpResponse response = client.execute(post);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.info("> parameters: {}", parameters);
				LOGGER.debug("Status: {}, Body: {}", response.getStatusLine(),
						new BasicResponseHandler().handleResponse(response));
			}
		} catch (Exception e) {
			LOGGER.warn("Error posting to EMF", e);
		} finally {
			post.releaseConnection();
			HttpClientUtils.closeQuietly(client);
		}
	}

	private List<BasicNameValuePair> getParameters(Check check) {
		String description = getDescription(check);
		String url = String.format("%s/#/checks/%s", seyrenConfig.getBaseUrl(), check.getId());
		int severity = getSeverity(check);
		return buildParameters(check, description, url, severity);
	}

	private String getDescription(Check check) {
		String description = "";
		if (StringUtils.isNotBlank(check.getDescription())) {
			description = String.format("\n> %s", check.getDescription());
		}
		return description;
	}

	private int getSeverity(Check check) {
		int severity = 4;
		if (check.getState() == AlertType.ERROR) {
			severity = 2;
		} else if (check.getState() == AlertType.WARN) {
			severity = 1;
		}
		return severity;
	}

	private List<BasicNameValuePair> buildParameters(Check check, String description, String url, int severity) {
		List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair("Host", check.getTarget()));
		parameters.add(new BasicNameValuePair("Source", "Seyren"));
		parameters.add(new BasicNameValuePair("NetworkDevice", null));
		parameters.add(new BasicNameValuePair("EventType", "EventType"));
		parameters.add(new BasicNameValuePair("Summary", description));
		parameters.add(new BasicNameValuePair("Severity", Integer.toString(severity)));
		parameters.add(new BasicNameValuePair("ExtraDetails", url));
		return parameters;
	}
}
