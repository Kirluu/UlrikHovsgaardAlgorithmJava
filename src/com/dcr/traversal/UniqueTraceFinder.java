package com.dcr.traversal;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.ByteDcrGraph;
import com.dcr.utils.ComparableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UniqueTraceFinder {

    private HashSet<ComparableList<Integer>> _uniqueTraceSet = new HashSet<ComparableList<Integer>>();
    private HashSet<ComparableList<Integer>> _uniqueEarlyTerminationTraceSet = new HashSet<ComparableList<Integer>>();
    private HashSet<List<Byte>> _seenStates;
    private HashSet<List<Byte>> _compareStates;
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

        FindUniqueTraces(graph, new ComparableList<Integer>());
        _compareTraceSet = _uniqueTraceSet;
        _compareEarlyTerminationTraceSet = _uniqueEarlyTerminationTraceSet;

        _compareStates = _seenStates.stream().map(ByteDcrGraph::StateWithExcludedActivitiesEqual)
                .collect(Collectors.toCollection(HashSet::new));

        return _uniqueTraceSet;
    }

    public boolean CompareTraces(ByteDcrGraph graph)
    {
        ResetValues();
        FindUniqueTraces(graph, new ComparableList<Integer>());

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

        _seenStates = new HashSet<List<Byte>>();
    }

    // TODO: Use while loop instead
    private void FindUniqueTraces(ByteDcrGraph inputGraph, ComparableList<Integer> currentTrace)
    {
        //compare trace length with desired depth
        for (Integer activity : inputGraph.GetRunnableIndexes())
        {
            ByteDcrGraph inputGraphCopy = inputGraph.Copy();
            ComparableList<Integer> currentTraceCopy = new ComparableList<>(currentTrace);

            // Execute and add event to trace
            inputGraphCopy.ExecuteActivity(activity);
            currentTraceCopy.add(activity);

            if (ByteDcrGraph.IsFinalState(inputGraphCopy.getState()))
            {
                _uniqueTraceSet.add(currentTraceCopy);

                if(_compareTraceSet != null &&
                        (!_compareTraceSet.contains(currentTraceCopy)))
                {
                    _comparisonResult = false;
                    ComparisonFailureTrace = currentTraceCopy.stream().map(x -> inputGraphCopy.getIndexToActivityId().get(x)).collect(Collectors.toList());
                    return;
                }
            }

            // If we have not seen the state before (successfully ADD it to the state)
            if (_seenStates.add(Arrays.asList(inputGraphCopy.getState()))) // Doc: "returns false if already present"
            {
                // Chase DFS:
                FindUniqueTraces(inputGraphCopy, currentTraceCopy);
            }
            else // "this set already contains the element" (State already seen):
            {
                // Add to collection of traces that reached previously seen states through different, alternate paths
                _uniqueEarlyTerminationTraceSet.add(currentTraceCopy);

                // If we found an alternate path that the original graph semantics trace-finding could not, we have observed a change
                if (_compareEarlyTerminationTraceSet != null &&
                        !_compareEarlyTerminationTraceSet.contains(currentTraceCopy))
                {
                    // TODO: Terminate early allowed here? We reached a seen state in a way that the original trace-finding did not
                }
            }
        }
    }
}
