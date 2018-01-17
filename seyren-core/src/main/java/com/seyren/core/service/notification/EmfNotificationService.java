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
		HttpPost post;

		String emfUrl = seyrenConfig.getEmfUrl();
		if (StringUtils.isNotBlank(emfUrl)) {
			post = new HttpPost(emfUrl);
		} else {
			LOGGER.warn("EMF API URL in Seyren Config needs to be set before sending notifications to EMF");
			return;
		}

		post = setHeaders(post, subscription.getType());
		List<BasicNameValuePair> parameters = getParameters(check, subscription);

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

	private List<BasicNameValuePair> getParameters(Check check, Subscription sub) {
		String description = getDescription(check);
		String url = String.format("%s/#/checks/%s", seyrenConfig.getBaseUrl(), check.getId());
		int severity = getSeverity(check);
		return buildParameters(sub, description, url, severity);
	}

	private List<BasicNameValuePair> buildParameters(Subscription sub, String description, String url, int severity) {
		List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair("Host", sub.getTarget()));
		parameters.add(new BasicNameValuePair("Source", "Seyren"));
		parameters.add(new BasicNameValuePair("NetworkDevice", null));
		parameters.add(new BasicNameValuePair("EventType", "AQ-Seyren"));
		parameters.add(new BasicNameValuePair("Summary", description));
		parameters.add(new BasicNameValuePair("Severity", Integer.toString(severity)));
		parameters.add(new BasicNameValuePair("ExtraDetails", url));
		return parameters;
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
