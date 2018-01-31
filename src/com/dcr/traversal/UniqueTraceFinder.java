package com.dcr.traversal;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.ByteDcrGraph;
import com.dcr.utils.ComparableList;

import java.util.*;
import java.util.stream.Collectors;

public class UniqueTraceFinder {

    private HashSet<ComparableList<Integer>> _uniqueTraceSet = new HashSet<ComparableList<Integer>>();
    private HashSet<ComparableList<Integer>> _uniqueEarlyTerminationTraceSet = new HashSet<ComparableList<Integer>>();
    private HashMap<List<Byte>, Boolean> _seenStates; // Stores each seen state along with whether or not it has lead to an accepting trace (accepting state)
    private HashSet<ComparableList<Integer>> _compareTraceSet;
    private HashSet<ComparableList<Integer>> _compareEarlyTerminationTraceSet;
    private ByteDcrGraph _compareByteGraph;

    private boolean _comparisonResult = true;

    private List<String> ComparisonFailureTrace; // Failure-trace as list of Strings
    public List<String> getComparisonFailureTrace() {
        return ComparisonFailureTrace;
    }
    

    public UniqueTraceFinder(ByteDcrGraph graph)
    {
        _compareByteGraph = graph.Copy();
        SetUniqueTraces(graph);
    }

    public boolean hasNoAcceptingTrace()
    {
        return _compareTraceSet.isEmpty();
    }

    public List<List<String>> getLanguageAsListOfTracesWithIds()
    {
        return _compareTraceSet.stream().map(traceInts ->
                traceInts.stream().map(index ->
                    _compareByteGraph.getIndexToActivityId().get(index)) // Look up Ids from indexes
                .collect(Collectors.toList())).collect(Collectors.toList());
    }

    private HashSet<ComparableList<Integer>> SetUniqueTraces(ByteDcrGraph graph)
    {
        ResetValues();

        List<List<Byte>> initiallySeenStates = new ArrayList<>();
        initiallySeenStates.add(graph.getState());
        FindUniqueTraces(graph, new ComparableList<Integer>(), initiallySeenStates);
        _compareTraceSet = _uniqueTraceSet;
        _compareEarlyTerminationTraceSet = _uniqueEarlyTerminationTraceSet;

        return _uniqueTraceSet;
    }

    public boolean CompareTraces(ByteDcrGraph graph)
    {
        ResetValues();

        List<List<Byte>> initiallySeenStates = new ArrayList<>();
        initiallySeenStates.add(graph.getState());
        FindUniqueTraces(graph, new ComparableList<Integer>(), initiallySeenStates);

            /* NOTE: State-space comparison is NOT a valid comparison-factor, since **different** graphs 
               may represent the same graph language. Therefore, the permitted language serves as the
               valid comparison factor in its stead. */
        return _comparisonResult
                && _uniqueTraceSet.size() == _compareTraceSet.size()
                && _compareEarlyTerminationTraceSet.size() == _uniqueEarlyTerminationTraceSet.size()
                && _compareEarlyTerminationTraceSet.equals(_uniqueEarlyTerminationTraceSet); // Set comparison, aka "containsAll" both ways
    }

    private void ResetValues()
    {
        _comparisonResult = true;
        ComparisonFailureTrace = null;
        _uniqueTraceSet = new HashSet<ComparableList<Integer>>();
        _uniqueEarlyTerminationTraceSet = new HashSet<ComparableList<Integer>>();

        _seenStates = new HashMap<List<Byte>, Boolean>();
    }

    /// <summary>
    /// Private function used to discover the full language of a DCR graph in the shape of its memory-optimized ByteDcrGraph format.
    /// </summary>
    /// <param name="inputGraph">The ByteDcrGraph for which we wish to discover the language.</param>
    /// <param name="currentTrace">A comparable list of integers, signifying the activity-IDs given in
    /// the 'inputGraph' in an order signifying a trace of activity-executions.</param>
    /// <param name="statesSeenInTrace">A list of states that are meant to be updated as 'leading to an accepting trace/state later on'
    /// if the state is not a final state itself. When a state is learned to have lead to such an accepting state later on, we stop passing
    /// it around (removing them from the list to save memory).</param>
    private void FindUniqueTraces(ByteDcrGraph inputGraph, ComparableList<Integer> currentTrace, List<List<Byte>> statesSeenInTrace)
    {
        //compare trace length with desired depth
        for (Integer activity : inputGraph.GetRunnableIndexes())
        {
            ByteDcrGraph inputGraphCopy = inputGraph.Copy();
            ComparableList<Integer> currentTraceCopy = new ComparableList<>(currentTrace);

            // Execute and add event to trace
            inputGraphCopy.ExecuteActivity(activity);
            currentTraceCopy.add(activity);

            boolean isFinalState = ByteDcrGraph.IsFinalState(inputGraphCopy.getState());
            if (isFinalState)
            {
                // Store this trace as unique, accepting trace
                _uniqueTraceSet.add(currentTraceCopy);

                // Store the fact that all the states seen until here, lead to some accepting trace:
                // NOTE: **Intended**: Only set true for the states PRIOR to the change we've just seen (by activity execution)
                // NOTE: ^--> We handle the (non-)occurrence of whether the new state reached was seen before below
                for (List<Byte> state : statesSeenInTrace)
                {
                    _seenStates.put(state, true); // The states seen prior in trace all lead to a final state via the language
                }
                // Optimization: We can disregard holding all of these states for this trace now, because we already said they all lead to acceptance
                statesSeenInTrace.clear(); // Clears across the remaining DFS branches too - if one path leads to acceptance, we don't need to re-update for every other path to acceptance too

                if(_compareTraceSet != null &&
                        (!_compareTraceSet.contains(currentTraceCopy)))
                {
                    _comparisonResult = false;
                    ComparisonFailureTrace = currentTraceCopy.stream().map(x -> inputGraphCopy.getIndexToActivityId().get(x)).collect(Collectors.toList());
                    return;
                }
            }

            // If we have not seen the state before (successfully ADD it to the state)
            Boolean leadsToAcceptingTrace = _seenStates.get(inputGraphCopy.getState());
            if (leadsToAcceptingTrace != null) { // the map already contains the given state (seen before):

                /* ASSUMPTION: When checking for previously having seen our newly reached state,
                 * the "leadsToAcceptingState" value is fully updated and dependable on due to D-F-S. */
                if (leadsToAcceptingTrace) {
                    // Add to collection of traces that reached previously seen states through different, alternate paths
                    _uniqueEarlyTerminationTraceSet.add(currentTraceCopy);

                    /* If we found an alternate path to a previously seen state (which leads to an accepting trace),
                     * that the original graph semantics trace-finding could not: We have observed a language change */
                    if (_compareEarlyTerminationTraceSet != null &&
                            !_compareEarlyTerminationTraceSet.contains(currentTraceCopy))
                    {
                        // TODO: Terminate early allowed here? We reached a seen state in a way that the original trace-finding did not
                    }
                }
            }
            else { // this is the first time we see this state
                /* Perform the first observation of this newly reached state along with the local knowledge of whether it leads to a final state,
                 * determined by whether it itself is one such final state */
                _seenStates.put(inputGraphCopy.getState(), isFinalState);

                // Add newly reached state, because it's not a final state itself, so we will have to update later if we reach a final state later
                List<List<Byte>> statesSeenInTraceCopy = new ArrayList<>(statesSeenInTrace);
                statesSeenInTraceCopy.add(inputGraphCopy.getState());

                // RECURSION:
                /* Optimization-note: We pass on the original, mutable list of states seen prior in trace to
                 * allow future accepting states found to optimize the list through updates even further backwards in the DFS flow. */
                FindUniqueTraces(inputGraphCopy, currentTraceCopy, isFinalState ? statesSeenInTrace : statesSeenInTraceCopy);
            }
        }
    }
}
