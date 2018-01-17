/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.service.notification;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.github.restdriver.clientdriver.capture.StringBodyCapture;
import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.domain.SubscriptionType;
import com.seyren.core.domain.ThresholdAlert;
import com.seyren.core.domain.ThresholdCheck;
import com.seyren.core.util.config.SeyrenConfig;

public class EmfNotificationServiceTest {
	private NotificationService notificationService;
	private SeyrenConfig mockSeyrenConfig;

	@Rule
	public ClientDriverRule clientDriver = new ClientDriverRule();

	@Before
	public void before() {
		mockSeyrenConfig = mock(SeyrenConfig.class);
		;
		notificationService = new EmfNotificationService(mockSeyrenConfig);
	}

	@Test
	public void notifcationServiceCanOnlyHandleEmfSubscription() {
		assertThat(notificationService.canHandle(SubscriptionType.DC_EMF), is(true));
		assertThat(notificationService.canHandle(SubscriptionType.AWS_EMF), is(true));
		for (SubscriptionType type : SubscriptionType.values()) {
			if (type == SubscriptionType.DC_EMF || type == SubscriptionType.AWS_EMF) {
				continue;
			}
			assertThat(notificationService.canHandle(type), is(false));
		}
	}

	@Test
	public void sendNotificationTest() throws Exception {

		when(mockSeyrenConfig.getBaseUrl()).thenReturn(clientDriver.getBaseUrl() + "/seyren");
		when(mockSeyrenConfig.getEmfUrl()).thenReturn(clientDriver.getBaseUrl() + "/emf/api");

		Check check = new ThresholdCheck().withWarn(BigDecimal.ONE).withError(BigDecimal.TEN).withEnabled(true)
				.withName("check-name").withDescription("Testing Description").withTarget("the.target.name")
				.withState(AlertType.ERROR).withId("testing");

		Subscription subscription = new Subscription().withType(SubscriptionType.DC_EMF).withTarget("testing_app_key");

		DateTime timestamp = new DateTime(1420070400000L);

		Alert alert = new ThresholdAlert().withWarn(BigDecimal.valueOf(5)).withError(BigDecimal.valueOf(10))
				.withTarget("the.target.name").withValue(BigDecimal.valueOf(12)).withFromType(AlertType.WARN)
				.withToType(AlertType.ERROR).withTimestamp(timestamp);

		List<Alert> alerts = Arrays.asList(alert);

		StringBodyCapture bodyCapture = new StringBodyCapture();

		clientDriver.addExpectation(onRequestTo("/emf/api").withMethod(ClientDriverRequest.Method.POST)
				.capturingBodyIn(bodyCapture).withHeader("accept", "application/json"), giveEmptyResponse());

		notificationService.sendNotification(check, subscription, alerts);

		String content = bodyCapture.getContent();
		assertThat(content, Matchers.containsString("Host=testing_app_key"));
		assertThat(content, Matchers.containsString("Source=Seyren"));
		assertThat(content, Matchers.containsString("Summary="));
		assertThat(content, Matchers.containsString("Severity=2"));
		assertThat(content, Matchers.containsString("ExtraDetails="));

	}

}
