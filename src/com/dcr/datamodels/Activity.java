package com.dcr.datamodels;

import com.dcr.statistics.Confidence;
import com.dcr.statistics.Threshold;

public class Activity {
    private String _id;
    public String getId() { return _id; }
    private String _name;
    public String getName() { return _name; }

    // INCLUDED
    private Confidence _includedConfidence = new Confidence();
    public Confidence getIncludedConfidence() {
        return _includedConfidence;
    }
    public boolean isIncluded() {
        return _includedConfidence.get() > Threshold.getValue();
    }
    public void setIncluded(boolean value) {
        _includedConfidence = value ? new Confidence(1, 1) : new Confidence();
    }
    public boolean IncrementExcludedInvocation()
    {
        return _includedConfidence.incrInvocations();
    }
    public boolean IncrementExcludedViolation()
    {
        return _includedConfidence.incrViolations();
    }

    // EXECUTED
    private boolean _executed;
    public boolean isExecuted() {
        return _executed;
    }
    public void setExecuted(boolean value) {
        _executed = value;
    }

    // PENDING
    private Confidence _pendingConfidence = new Confidence();
    public Confidence getPendingConfidence() {
        return _pendingConfidence;
    }
    public boolean isPending() {
        return _pendingConfidence.get() <= Threshold.getValue();
    }
    public void setPending(boolean value) {
        _pendingConfidence = value ? new Confidence(1, 1) : new Confidence();
    }
    public boolean IncrementPendingInvocation()
    {
        return _pendingConfidence.incrInvocations();
    }
    public boolean IncrementPendingViolation()
    {
        return _pendingConfidence.incrViolations();
    }

    // ROLES
    private String _roles;
    public String getRoles() {
        return _roles;
    }
    public void setRoles(String value) {
        _roles = value;
    }


    public Activity(String id, String name) {
        _id = id.replaceAll("[^\\w\\s\\-\\+]", "");
        _name = name;
    }

    public Activity(String nameAndId) {
        String cleanedId = nameAndId.replaceAll("[^\\w\\s\\-\\+]", "");
        _id = cleanedId;
        _name = cleanedId;
    }

    public Activity(Activity copyFrom) {
        _roles = copyFrom.getRoles();
        _includedConfidence = new Confidence(copyFrom.getIncludedConfidence());
        _pendingConfidence = new Confidence(copyFrom.getPendingConfidence());
        _executed = copyFrom.isExecuted();
    }

    public byte getHashedActivity(boolean canExecute) {
        byte b = (byte)(canExecute ? 1 << 3 : 0); // 00001000
        b += (byte)(_executed ? 1 << 2 : 0);        // 00000100
        b += (byte)(isIncluded() ? 1 << 1 : 0);   // 00000010
        b += (byte)(isPending() ? 1 : 0);         // 00000001

        return b;
    }

    public boolean isSameNameIdAndStateAsOther(Activity o) {
        return _id.equals(o.getId()) && _name.equals(o.getName()) && isPending() == o.isPending() && isIncluded() == o.isIncluded() && isExecuted() == o.isExecuted();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Activity && _id.equals(((Activity) obj).getId());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        // Suitable nullity checks etc, of course :)
        hash = hash * 23 + _id.hashCode();
        //hash = hash * 23 + Name.getValue()HashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return _id + " : " + _name + " inc=" + isIncluded() + ", pnd=" + isPending() + ", exe=" + isExecuted();
    }
}
