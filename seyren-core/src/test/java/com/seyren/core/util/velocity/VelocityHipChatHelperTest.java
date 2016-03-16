package com.seyren.core.util.velocity;

import com.seyren.core.domain.*;
import com.seyren.core.util.config.SeyrenConfig;
import com.seyren.core.util.hipchat.HipChatHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VelocityHipChatHelperTest {

    private HipChatHelper hipChatHelper;

    @Before
    public void before() {
        this.hipChatHelper = new VelocityHipChatHelper(new SeyrenConfig());
    }

    @Test
    public void bodyContainsRightSortsOfThings() {

        Check check = new Check()
                .withId("123")
                .withEnabled(true)
                .withName("test-check")
                .withWarn(new BigDecimal("2.0"))
                .withError(new BigDecimal("3.0"))
                .withState(AlertType.ERROR);
        Subscription subscription = new Subscription()
                .withEnabled(true)
                .withType(SubscriptionType.HIPCHAT)
                .withTarget("target");
        Alert alert = new Alert()
                .withTarget("some.value")
                .withValue(new BigDecimal("4.0"))
                .withTimestamp(new DateTime())
                .withFromType(AlertType.OK)
                .withToType(AlertType.ERROR);
        List<Alert> alerts = Arrays.asList(alert);

        String message = hipChatHelper.createHipChatContent(check, subscription, alerts);

        assertThat(message, containsString("test-check"));
        assertThat(message, containsString("some.value"));
        assertThat(message, containsString("4.0"));

    }

    @Test
    public void templateLocationShouldBeConfigurable() {
        Check check = new Check()
                .withId("123")
                .withEnabled(true)
                .withName("test-check")
                .withWarn(new BigDecimal("2.0"))
                .withError(new BigDecimal("3.0"))
                .withState(AlertType.ERROR);

        SeyrenConfig mockConfiguration = mock(SeyrenConfig.class);
        when(mockConfiguration.getHipChatTemplateFileName()).thenReturn("test-hipchat-template.vm");
        when(mockConfiguration.getGraphiteUrl()).thenReturn("http://localhost:8081/graphite");
        HipChatHelper hipChatHelper = new VelocityHipChatHelper(mockConfiguration);
        String body = hipChatHelper.createHipChatContent(check, null, null);
        assertThat(body, containsString("Test content."));
    }

}