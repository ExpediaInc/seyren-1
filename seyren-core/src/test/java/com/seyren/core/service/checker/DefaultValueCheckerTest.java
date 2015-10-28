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
package com.seyren.core.service.checker;

import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;


import java.math.BigDecimal;

import com.seyren.core.domain.Check;
import org.junit.Before;
import org.junit.Test;

import com.seyren.core.domain.AlertType;

public class DefaultValueCheckerTest {
    
    private ValueChecker checker;
    AlertType lastState = AlertType.OK;
    Check mockCheck;
    String target = "target";
    BigDecimal warn = BigDecimal.valueOf(.15);
    BigDecimal error = BigDecimal.valueOf(.20);

    @Before
    public void before() {
        checker = new DefaultValueChecker();
        mockCheck = mock(Check.class);
    }




    @Test
    public void alertHasOkStateForBadHighValueWhenValueIsLessThanWarn() {
        when(mockCheck.getWarn()).thenReturn(warn);
        when(mockCheck.getError()).thenReturn(error);
        assertThat(checker.checkValue(bd("0.10"),mockCheck,target,lastState).getToType(), is(AlertType.OK));
    }
    
    @Test
    public void alertHasWarnStateForBadHighValueWhenValueIsEqualToWarn() {
        when(mockCheck.getWarn()).thenReturn(warn);
        when(mockCheck.getError()).thenReturn(error);
        assertThat(checker.checkValue(bd("0.15"),mockCheck, target, lastState).getToType(), is(AlertType.WARN));
    }
    
    @Test
    public void alertHasWarnStateForBadHighValueWhenValueIsGreaterToWarnButLessThanError() {
        when(mockCheck.getWarn()).thenReturn(warn);
        when(mockCheck.getError()).thenReturn(error);
        assertThat(checker.checkValue(bd("0.16"),mockCheck,target,lastState).getToType(), is(AlertType.WARN));
    }
    
    @Test
    public void alertHasErrorStateForBadHighValueWhenValueIsEqualToError() {
        when(mockCheck.getWarn()).thenReturn(warn);
        when(mockCheck.getError()).thenReturn(error);
        assertThat(checker.checkValue(bd("0.20"),mockCheck,target,lastState).getToType(), is(AlertType.ERROR));
    }

    @Test
    public void alertHasErrorStateForBadHighValueWhenValueIsGreaterThanError() {
        when(mockCheck.getWarn()).thenReturn(warn);
        when(mockCheck.getError()).thenReturn(error);
        assertThat(checker.checkValue(bd("0.21"),mockCheck,target,lastState).getToType(), is(AlertType.ERROR));
    }
    
    @Test
    public void alertHasOkStateForBadLowValueWhenValueIsGreaterThanWarn() {
        assertThat(checker.checkValue(bd("0.21"),mockCheck,target,lastState).getToType(), is(AlertType.OK));
    }
    
    @Test
    public void alertHasWarnStateForBadLowValueWhenValueIsEqualToWarn() {
        assertThat(checker.checkValue(bd("0.20"),mockCheck,target,lastState).getToType(), is(AlertType.WARN));
    }
    
    @Test
    public void alertHasWarnStateForBadLowValueWhenValueIsLessThanWarnButGreaterThanError() {
        assertThat(checker.checkValue(bd("0.19"),mockCheck,target,lastState).getToType(), is(AlertType.WARN));
    }
    
    @Test
    public void alertHasErrorStateForBadLowValueWhenValueIsEqualToError() {
        assertThat(checker.checkValue(bd("0.15"),mockCheck,target,lastState).getToType(), is(AlertType.ERROR));
    }
    
    @Test
    public void alertHasErrorStateForBadLowValueWhenValueIsLessThanError() {
        assertThat(checker.checkValue(bd("0.14"), mockCheck, target, lastState).getToType(), is(AlertType.ERROR));
    }
    
    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
    
}
