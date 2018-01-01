package com.dcr.statistics;

public class Confidence {
    private int _violations;
    private int _invocations;

    public Confidence() {}

    public Confidence(int invocations, int violations) {
        _invocations = invocations;
        _violations = violations;
    }

    public Confidence(Confidence copyFrom) {
        _invocations = copyFrom._invocations;
        _violations = copyFrom._violations;
    }

    public double get() {
        if (_invocations == 0) return 0;
        return (double) _violations / _invocations;
    }

    public Boolean isAboveThreshold() {
        return get() > Threshold.getValue();
    }

    public Boolean incrInvocations() {
        double oldC = get();
        _invocations++;
        double newC = get();

        // Check whether the confidence's update makes it go past the threshold
        return didConfidencesPassThreshold(oldC, newC, Threshold.getValue());
    }

    public Boolean incrViolations()
    {
        double oldC = get();
        _violations++;
        double newC = get();

        // Check whether the confidence's update makes it go past the threshold
        return didConfidencesPassThreshold(oldC, newC, Threshold.getValue());
    }

    /// <summary>
    /// Performs update to the confidence and returns whether the current threshold was passed in either direction
    /// </summary>
    public Boolean increment(Boolean violationOccurred)
    {
        double oldC = get();
        _invocations++;
        if (violationOccurred)
            _violations++;
        double newC = get();

        // Check whether the confidence's update makes it go past the threshold
        return didConfidencesPassThreshold(oldC, newC, Threshold.getValue());
    }

    private static Boolean didConfidencesPassThreshold(double oldConfidence, double newConfidence, double threshold) {
        // Raised above or fell below the threshold:
        return oldConfidence <= threshold && threshold < newConfidence || newConfidence < threshold && threshold < oldConfidence;
    }

    @Override
    public String toString() {
        return String.format("{0} / {1} ({2:N2})", _violations, _invocations, get());
    }
}
