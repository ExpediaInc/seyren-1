package com.seyren.core.service.schedule;

import com.seyren.core.domain.Check;
import com.seyren.core.service.checker.GraphiteTargetChecker;
import com.seyren.core.service.checker.TargetChecker;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class TargetCheckerFactory {

    private final GraphiteTargetChecker graphiteTargetChecker;

    @Inject
    public TargetCheckerFactory(GraphiteTargetChecker graphiteTargetChecker) {
        this.graphiteTargetChecker = graphiteTargetChecker;
    }

    public TargetChecker create(Check check) {
        return graphiteTargetChecker;
    }
}
