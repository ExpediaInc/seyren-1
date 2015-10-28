package com.seyren.core.domain;


import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;

public class CheckState {

    private long count;
    private double mean;
    private double posSum;
    private double negSum;
    private final long minCount;
    private final Histogram histogram;


    public CheckState(long minCount) {
        this.minCount = minCount;
        this.histogram = new Histogram(new ExponentiallyDecayingReservoir());
    }

    public Histogram getHistogram() {
        return histogram;
    }

    public CheckState(long minCount, double mean, double posSum, double negSum, long count, Histogram histogram) {
        this.minCount = minCount;
        this.negSum = negSum;
        this.posSum = posSum;
        this.mean = mean;
        this.count = count;
        this.histogram = histogram;
    }

    public long getMinCount() {
        return minCount;
    }

    public long getCount() {
        return count;
    }

    public double getMean() {
        return mean;
    }


    public double getPosSum() {
        return posSum;
    }

    public double getNegSum() {
        return negSum;
    }




}
