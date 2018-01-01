package com.dcr.datamodels;

import com.dcr.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LogTrace {
    //public event Action EventAdded; // TODO: Java events...

    private String Id;
    public String getId() { return Id; }
    public void setId(String value) { Id = value; }

    private final List<LogEvent> Events = new ArrayList<>();
    public List<LogEvent> getEvents() { return Events; }

    private boolean IsFinished;
    public boolean getIsFinished() { return IsFinished; }
    public void setIsFinished(boolean value) { IsFinished = value; }

    public LogTrace() {
    }

    public LogTrace(char... ids) {
        this.AddEventsWithChars(ids);
    }

    public LogTrace(LogTrace copyFrom) {
        Id = copyFrom.getId();
        IsFinished = copyFrom.getIsFinished();

        for (LogEvent logEvent : Events)
        {
            Add(logEvent.Copy());
        }
    }

    public void AddEventsWithChars(char... ids) {
        for (char id : ids)
        {
            Add(new LogEvent(id + "", "name_" + id) );
        }
        //EventAdded?.Invoke();
    }

    public void Add(LogEvent e) {
        Events.add(e);
        //EventAdded?.Invoke();
    }

    public LogTrace Copy() {
        return new LogTrace(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof LogTrace))
            return false;
        LogTrace other = (LogTrace)obj;
        if (other.getEvents().size() != Events.size()) // Same size quick check
            return false;

        // We want to compare each underlying event at the same locations (same index)
        IntStream indices = IntStream.range(0, other.getEvents().size());
        return indices.allMatch(idx -> Events.get(idx).equals(other.getEvents().get(idx)));
    }

    @Override
    public int hashCode()
    {
        return 17;
    }

    // "Pretty print"
    @Override
    public String toString() {
        return Events.stream().map(x -> x.getIdOfActivity()).collect(Collectors.joining("; "));
    }
}
