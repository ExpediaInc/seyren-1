package com.seyren.core.service.notification;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.domain.SubscriptionType;
import com.seyren.core.exception.NotificationFailedException;
import com.seyren.core.util.config.SeyrenConfig;

//ONLY difference in AWS and Date center EMF, apart from having different end points is that an authentication token is required for the former.

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
		return isDCSub(subscriptionType) || isAWSSub(subscriptionType);
	}

	@Override
	public void sendNotification(Check check, Subscription subscription, List<Alert> alerts)
			throws NotificationFailedException {

		HttpClient client = HttpClientBuilder.create().useSystemProperties().build();
		HttpPost post = new HttpPost();
		String emfUrl = seyrenConfig.getEmfUrl();
		if (StringUtils.isNotBlank(emfUrl)) {
			post = new HttpPost(emfUrl);
		} else {
			LOGGER.warn("EMF API URL in Seyren Config needs to be set before sending notifications to EMF");
			return;
		}

		post = setHeaders(post, subscription.getType());
		JSONObject parameters = getParameters(check, subscription);
		LOGGER.info("> emfUrl: {}", emfUrl);
		LOGGER.info("> parameters: {}", parameters);
		try {
			post.setEntity(new StringEntity(parameters.toString()));
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

	private HttpPost setHeaders(HttpPost post, SubscriptionType subscriptionType) {
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-type", "application/json");

		if (isAWSSub(subscriptionType)) {
			post.setHeader("Ocp-Apim-Subscription-Key", seyrenConfig.getEmfSubKey());
		}
		return post;
	}

	private boolean isAWSSub(SubscriptionType subscriptionType) {
		return subscriptionType == SubscriptionType.AWS_EMF;
	}

	private boolean isDCSub(SubscriptionType subscriptionType) {
		return subscriptionType == SubscriptionType.DC_EMF;
	}

	private JSONObject getParameters(Check check, Subscription sub) {
		String description = getDescription(check);
		String url = String.format("%s/#/checks/%s", seyrenConfig.getBaseUrl(), check.getId());
		int severity = getSeverity(check);
		return buildParameters(sub, description, url, severity);
	}

	private JSONObject buildParameters(Subscription sub, String description, String url, int severity) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Host", sub.getTarget());
		jsonObj.put("Source", "Seyren");
		jsonObj.put("NetworkDevice", "");
		jsonObj.put("EventType", "AQ-Seyren");
		jsonObj.put("Summary", description);
		jsonObj.put("Severity", Integer.toString(severity));
		jsonObj.put("ExtraDetails", url);
		return jsonObj;
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
		AlertType state = check.getState();
		if (state == AlertType.ERROR) {
			severity = 2;
		} else if (state == AlertType.WARN) {
			severity = 1;
		}
		return severity;
	}

}
