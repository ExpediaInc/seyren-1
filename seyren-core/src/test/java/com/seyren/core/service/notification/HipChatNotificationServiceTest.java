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

import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.github.restdriver.clientdriver.capture.StringBodyCapture;
import com.seyren.core.domain.*;
import com.seyren.core.util.config.SeyrenConfig;
import com.seyren.core.util.hipchat.HipChatHelper;
import com.seyren.core.util.velocity.VelocityHipChatHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HipChatNotificationServiceTest {

    private SeyrenConfig mockSeyrenConfig;
    private HipChatHelper mockHipChatHelper;
    private NotificationService notificationService;

    @Rule
    public ClientDriverRule clientDriver = new ClientDriverRule();

    @Before
    public void before() {
        mockSeyrenConfig = mock(SeyrenConfig.class);
        when(mockSeyrenConfig.getBaseUrl()).thenReturn(clientDriver.getBaseUrl() + "/hipchat");
        when(mockSeyrenConfig.getHipChatUsername()).thenReturn("LPMS");
        when(mockSeyrenConfig.getHipChatTemplateFileName()).thenReturn("test2-hipchat-template.vm");

        mockHipChatHelper = new VelocityHipChatHelper(mockSeyrenConfig);
        notificationService = new HipChatNotificationService(mockSeyrenConfig, mockHipChatHelper, clientDriver.getBaseUrl());
    }

    @Test
    public void notificationServiceCanHandleHipChatSubscription() {
        assertThat(notificationService.canHandle(SubscriptionType.HIPCHAT), is(true));
    }

    @Test
    public void checkingOutTheHappyPath() {

        Check check = new Check()
                .withEnabled(true)
                .withName("test-check")
                .withState(AlertType.ERROR)
                .withWarn(BigDecimal.ONE)
                .withError(BigDecimal.TEN);

        Subscription subscription = new Subscription()
                .withEnabled(true)
                .withType(SubscriptionType.HIPCHAT)
                .withTarget("target");

        Alert alert = new Alert()
                .withTarget("the.target.name")
                .withValue(BigDecimal.valueOf(12))
                .withWarn(BigDecimal.valueOf(5))
                .withError(BigDecimal.valueOf(10))
                .withFromType(AlertType.OK)
                .withToType(AlertType.ERROR)
                .withTimestamp(new DateTime());

        List<Alert> alerts = Arrays.asList(alert);

        String seyrenUrl = clientDriver.getBaseUrl() + "/seyren";

        when(mockSeyrenConfig.getGraphiteUrl()).thenReturn(clientDriver.getBaseUrl() + "/graphite");
        when(mockSeyrenConfig.getBaseUrl()).thenReturn(seyrenUrl);


        clientDriver.addExpectation(
                onRequestTo("/v2/room/target/notification")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withAnyParams(),
                giveEmptyResponse()
        );

        notificationService.sendNotification(check, subscription, alerts);

    }

}