package com.seyren.core.service.schedule;

import com.seyren.core.domain.Check;
import com.seyren.core.service.checker.*;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class ValueCheckerFactory {

    private final DefaultValueChecker defaultValueChecker;
    private final CumulativeSumValueChecker cumulativeSumValueChecker;

    @Inject
    public ValueCheckerFactory(DefaultValueChecker defaultValueChecker, CumulativeSumValueChecker cumulativeSumValueChecker) {
        this.defaultValueChecker = defaultValueChecker;
        this.cumulativeSumValueChecker = cumulativeSumValueChecker;
    }

    public ValueChecker create(Check check) {
        if (check.isVariableThresholdEnabled()) {
            return cumulativeSumValueChecker;
        }
        return defaultValueChecker;
    }
}
