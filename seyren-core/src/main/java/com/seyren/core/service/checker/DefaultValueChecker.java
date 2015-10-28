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

import java.math.BigDecimal;

import javax.inject.Named;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import org.joda.time.DateTime;

@Named
public class DefaultValueChecker implements ValueChecker {
    
    @Override
    public Alert checkValue(BigDecimal value, Check check, String target, AlertType lastState) {

        BigDecimal warn = check.getWarn();
        BigDecimal error = check.getError();
        
        boolean isHighValueWorse = isTheValueBeingHighWorse(warn, error);
        
        if (isBeyondThreshold(value, error, isHighValueWorse)) {
            return createAlert(target, value, check.getWarn(), check.getError(), lastState, AlertType.ERROR, DateTime.now());
        } else if (isBeyondThreshold(value, warn, isHighValueWorse)) {
            return  createAlert(target, value, check.getWarn(), check.getError(), lastState, AlertType.WARN, DateTime.now());
        }
        
        return createAlert(target, value, check.getWarn(), check.getError(), lastState, AlertType.OK, DateTime.now());
        
    }
    
    private boolean isBeyondThreshold(BigDecimal value, BigDecimal threshold, boolean isHighValueWorse) {
        if (isHighValueWorse) {
            return value.compareTo(threshold) >= 0;
        }
        return value.compareTo(threshold) <= 0;
    }

    private Alert createAlert(String target, BigDecimal value, BigDecimal warn, BigDecimal error, AlertType from, AlertType to, DateTime now) {
        return new Alert()
                .withTarget(target)
                .withValue(value)
                .withWarn(warn)
                .withError(error)
                .withFromType(from)
                .withToType(to)
                .withTimestamp(now);
    }
    
    private boolean isTheValueBeingHighWorse(BigDecimal warn, BigDecimal error) {
        return warn.compareTo(error) <= 0;
    }
    
}
