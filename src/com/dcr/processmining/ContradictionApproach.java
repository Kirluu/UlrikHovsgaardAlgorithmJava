package com.dcr.processmining;

import com.dcr.datamodels.*;
import com.dcr.statistics.Confidence;

import java.util.*;
import java.util.stream.Collectors;

public class ContradictionApproach {
    //public static event Action<DcrGraph> PostProcessingResultEvent; // TODO: Java events...

    //This is the mined graph. NOT THE ACTUAL RUNNING GRAPH.
    private DcrGraph Graph = new DcrGraph();
    public DcrGraph getMinedGraph() { return Graph; }

    private Stack<Activity> _run = new Stack<>();
    private String _runId;
    private final HashMap<String, Stack<Activity>> _allRuns = new HashMap<>();

    private Activity _last;
    private final int MinimumNestedSize = 3;

    private boolean _failSilently = false; // TODO: Set this value to true in "production"

    private void WaitForUserContinue(String message) {
        if (_failSilently) return;
        System.out.println(message);
        new Scanner(System.in).nextLine();
    }

    public ContradictionApproach(HashSet<Activity> activities)
    {
        // Initializing activities
        for (Activity a : activities)
        {
            try {
                Graph.AddActivity(a.getId(), a.getName(), a.getRoles());
                //a is excluded
                Graph.SetIncluded(false, a.getId());
                //a is Pending
                Graph.SetPending(true, a.getId());
            }
            catch(Exception e) { WaitForUserContinue("Failed to add activity to ContradictionApproach!" + e); }
        }
        for (Activity a1 : activities)
        {
            for (Activity a2 : activities)
            {
                try {
                    //add exclude from everything to everything
                    Graph.AddIncludeExclude(false, a1.getId(), a2.getId());
                    Graph.AddResponse(a1.getId(), a2.getId());
                    Graph.AddCondition(a1.getId(), a2.getId());
                }
                catch(Exception e) { WaitForUserContinue("Failed to add relation between activities in ContradictionApproach!" + e); }
            }
        }
    }

    public boolean AddEvent(String id, String instanceId)
    {
        if (!instanceId.equals(_runId))
        { // add the currentRun to dictionary, if not the one we want to work on.
            if(_runId != null)
                _allRuns.put(_runId, _run);

            _run = _allRuns.get(instanceId);
            if (_run != null)
            { //get the one we want to work on.
                _runId = instanceId;
                _last = _run.peek();
            }
            else
            {
                _run = new Stack<Activity>();
                _runId = instanceId;
            }
        }

        Activity currentActivity = Graph.getActivity(id);
        boolean graphAltered = false;

        if (_run.size() == 0) // First event of trace
        {
            // Update Excluded-state invocations and violation for currentActivity
            graphAltered |= currentActivity.IncrementExcludedViolation();
            for (Activity graphActivity : Graph.getActivities())
            {
                graphAltered |= graphActivity.IncrementExcludedInvocation();
            }
        }
        else
        {
            Activity lastActivity = Graph.getActivity(_last.getId());
            boolean firstViolation = true;
            Object[] runObjArr = _run.toArray();
            Activity[] runArr = Arrays.copyOf(runObjArr, runObjArr.length, Activity[].class);

            //traversing run : reverse order, cause stack
            for (int i = runArr.length - 2; i > 0; i--)
            {
                if (runArr[i + 1].equals(lastActivity) && runArr[i].equals(currentActivity))
                    firstViolation = false;
            }

            // Exclude-relation from _last to current has been violated (Counts towards exchanging the exclusion with an inclusion, or if self-excl: removal of excl.)
            if(firstViolation)
                graphAltered |= Graph.getIncludeExcludes().get(lastActivity).get(currentActivity).incrViolations();
        }

        boolean firstOccurrenceInTrace = !_run.contains(currentActivity);
        if (firstOccurrenceInTrace)
        {
            // Update invocation-counter for all outgoing Inclusion/Exclusion relations
            for (Map.Entry<Activity, Confidence> target : Graph.getIncludeExcludes().get(currentActivity).entrySet())
            {
                graphAltered |= target.getValue().incrInvocations();
            }

            List<Activity> otherActivities = Graph.getActivities().stream().filter(x -> !x.equals(currentActivity)).collect(Collectors.toList());
            for (Activity conditionSource : otherActivities)
            {
                // Register ingoing condition-violation for activities that have not been run before : the current trace
                boolean conditionViolated = !_run.contains(conditionSource);
                graphAltered |= Graph.getConditions().get(conditionSource).get(currentActivity).increment(conditionViolated);
            }
        }

        _run.push(currentActivity);
        _last = currentActivity;

        return graphAltered;
    }

    public boolean Stop()
    {
        boolean graphAltered = false;

        // Increment Pending-states invocations (and mayhaps violations of the constraint) for all activities
        HashSet<Activity> notInTrace = Graph.getActivities().stream().filter(a -> !_run.contains(a)).collect(Collectors.toCollection(HashSet::new));
        for (Activity act : Graph.getActivities())
        {
            boolean violationOccurred = notInTrace.contains(act);

            // Invoke Pending + self-condition statistics for all activities : trace
            graphAltered |= act.IncrementPendingInvocation();
            graphAltered |= Graph.getConditions().get(act).get(act).incrInvocations();

            if (violationOccurred)
            {
                // Didn't occur : trace --> Pending-belief is violated:
                graphAltered |= act.IncrementPendingViolation();
            }
            else
            {
                // Did occur : trace --> Self-cindition is violated: (Registered here to get max 1 violations pr. trace)
                graphAltered |= Graph.getConditions().get(act).get(act).incrViolations();
            }
        }

        // Evaluate Responses for all activities
        HashSet<Activity> activitiesConsidered = new HashSet<>();
        while (_run.size() > 0)
        {
            Activity act = _run.pop(); //the next element
            if (activitiesConsidered.contains(act))
                continue;
            activitiesConsidered.add(act);

            List<Activity> otherActivities = Graph.getActivities().stream().filter(x -> !x.equals(act)).collect(Collectors.toList());

            // First time we consider "act": All other activities not already considered
            //     have had their Response-relation from "act" violated, as they did not occur later
            for (Activity otherAct : otherActivities)
            {
                boolean responseViolated = !activitiesConsidered.contains(otherAct);
                graphAltered |= Graph.getResponses().get(act).get(otherAct).increment(responseViolated);
            }
        }

        _allRuns.remove(_runId);
        _runId = null;
        _run = new Stack<Activity>();
        _last = null;

        return graphAltered;
    }

    //if it has not seen the 'id' before it will assume that it is a new trace.
    public boolean Stop(String id)
    {
        if (id != _runId)
        { // add the currentRun to dictionary, if not the one we want to stop.
            if (_runId != null)
                _allRuns.put(_runId, _run);
            _run = _allRuns.get(id);
            if (_run != null)
            { //get the one we want to stop.
                _runId = id;
            }
            else
            { //new empty run.
                _run = new Stack<Activity>();
                _runId = id;
            }
        }
        return Stop();
    }

    //used to add one complete trace.
    public boolean AddTrace(LogTrace trace)
    {
        //maybe run stop first-?
        boolean graphAltered = false;
        for (LogEvent e : trace.getEvents())
        {
            graphAltered |= AddEvent(e.getIdOfActivity(), trace.getId());
        }
        boolean stopAlteredGraph = false;
        if (trace.getEvents().size() > 0)
            stopAlteredGraph = Stop();
        return graphAltered || stopAlteredGraph;
    }

    public boolean addLog(Log log)
    {
        boolean graphAltered = false;
        for (LogTrace trace : log.getTraces())
        {
            graphAltered |= this.AddTrace(trace);
        }
        return graphAltered;
    }

    public static DcrGraph PostProcessing(DcrGraph graph)
    {
        DcrGraph copy = graph.Copy();

        //PostProcessingResultEvent?.Invoke(copy);
        return copy;
    }
}
