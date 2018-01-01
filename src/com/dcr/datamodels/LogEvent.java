package com.dcr.datamodels;

import java.util.Date;

public class LogEvent {
    public String EventId; //unique for each event

    private final String IdOfActivity; //matches an activity
    public String getIdOfActivity() { return IdOfActivity; }
    private final String Name;
    public String getName() { return Name; }

    private Date TimeOfExecution;
    public Date getTimeOfExecution() { return TimeOfExecution; }
    public void setTimeOfExecution(Date value) { TimeOfExecution = value; }

    private String ActorName;
    public String getActorName() { return ActorName; }
    public void setActorName(String value) { ActorName = value; }

    private String RoleName;
    public String getRoleName() { return RoleName; }
    public void setRoleName(String value) { RoleName = value; }

    public LogEvent(String activityId, String name)
    {
        IdOfActivity = activityId;
        Name = name;
    }

    public LogEvent(LogEvent copyFrom)
    {
        IdOfActivity = copyFrom.getIdOfActivity();
        Name = copyFrom.getName();
        TimeOfExecution = copyFrom.getTimeOfExecution();
        ActorName = copyFrom.getActorName();
        RoleName = copyFrom.getRoleName();
    }

    public LogEvent Copy()
    {
        return new LogEvent(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LogEvent && IdOfActivity.equals(((LogEvent) obj).getIdOfActivity());
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        // Suitable nullity checks etc, of course :)
        hash = hash * 23 + IdOfActivity.hashCode();
        //hash = hash * 23 + Name.GetHashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return IdOfActivity;
    }
}
