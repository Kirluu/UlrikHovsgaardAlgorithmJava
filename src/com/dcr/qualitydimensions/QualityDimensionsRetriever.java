package com.dcr.qualitydimensions;

import com.dcr.datamodels.*;
import com.dcr.statistics.Confidence;

import java.util.*;
import java.util.stream.Collectors;

public class QualityDimensionsRetriever {

    public static QualityDimensions Retrieve(DcrGraph graph, Log log) throws Exception {
        QualityDimensions res = new QualityDimensions();
        res.setFitness(GetFitness(log, graph));
        res.setSimplicity(GetSimplicity(graph));
        res.setPrecision(GetPrecision(log, graph));

        return res;
    }

    /// <summary>
    /// Divides the amount of traces replayable by the _inputGraph with the total amount of traces : the _inputLog, multiplied by 100.
    /// </summary>
    /// <returns>The fitness percentage of the _inputGraph with respects to the _inputLog.</returns>
    public static double GetFitness(Log log, DcrGraph graph) throws Exception {
        if (log.getTraces().isEmpty()) return 100.0;

        double tracesReplayed = 0.0;
        for (LogTrace logTrace : log.getTraces())
        {
            DcrGraph graphCopy = graph.Copy();
            graphCopy.setRunning(true);
            boolean success = true;
            for (LogEvent logEvent : logTrace.getEvents())
            {
                try
                {
                    if (!graphCopy.Execute(graphCopy.getActivity(logEvent.getIdOfActivity())))
                    {
                        success = false;
                        break;
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Error when executing activity from GetFitness method: ");
                    e.printStackTrace();
                    success = false;
                    break;
                }
            }
            if (success && graphCopy.IsFinalState())
            {
                // All executions succeeded
                tracesReplayed++;
            }
        }

        int totalTraces = log.getTraces().size();

        return (tracesReplayed / totalTraces) * 100.0;
    }

    /// <summary>
    /// Divides the amount of relations : the _inputGraph with the total amount of relations that could have been : the graph.
    /// Then divides the amount of relation couples (For instance relation between A and B, regardless of direction) with the
    /// total possible amount of relation couples.
    /// Then divides both of the above by two and adds them together, so that both calculations have an equal say : the
    /// resulting simplicity.
    /// In the end multiplies by 100 for percentage representation.
    /// </summary>
    /// <returns>The simplicity percentage of the _inputGraph.</returns>
    public static double GetSimplicity(DcrGraph graph)
    {
        double totalActivities = (double)graph.getActivities().size();

        double relationsInGraph = graph.getConditions().values().stream().mapToInt(HashMap::size).sum() +
            graph.getIncludeExcludes().values().stream().mapToInt(HashMap::size).sum() +
            graph.getResponses().values().stream().mapToInt(HashMap::size).sum();

        //Also count relations : the possible nested graphs
        //+ _inputGraph.Activities.Where(a -> a.IsNestedGraph).Select(b -> b.NestedGraph).Sum(nestedGraph -> nestedGraph.Conditions.Values.Sum(x -> x.Count) + nestedGraph.IncludeExcludes.Values.Sum(x -> x.Count) + nestedGraph.Responses.Values.Sum(x -> x.Count) + nestedGraph.Milestones.Values.Sum(x -> x.Count));
        double possibleRelations = (totalActivities * totalActivities * 4.0 - totalActivities * 3.0);

        // Possible relation couples = n + n*(n-1) / 2
        double possibleRelationCouples = totalActivities + (totalActivities * (totalActivities - 1) / 2);
        HashSet<RelationCouple> relationCouples = new HashSet<>();
        GatherRelationCouples(graph.getConditions(), relationCouples);
        GatherRelationCouples(graph.getResponses(), relationCouples);
        GatherRelationCouples(graph.getIncludeExcludes(), relationCouples);

        double totalRelationsPart = (1.0 - relationsInGraph / possibleRelations) / 2; // 50 % weight
        double relationCouplesPart = (1.0 - relationCouples.size() / possibleRelationCouples) / 2; // 50 % weight

        double result = (totalRelationsPart + relationCouplesPart) * 100.0;

        return result > 0.0 ? result : 0.0;
    }

    public static void GatherRelationCouples(HashMap<Activity, HashMap<Activity,Confidence>> dictionary, HashSet<RelationCouple> relationCouples) {
        for (Map.Entry<Activity, HashMap<Activity,Confidence>> relation : dictionary.entrySet())
        {
            for (Activity target : DcrGraph.FilterHashMapByThreshold(relation.getValue()))
            {
                relationCouples.add(new RelationCouple(relation.getKey(), target));
            }
        }
    }

    public static double GetPrecision(Log log, DcrGraph graph) {
        HashMap<List<Byte>, Integer> seenStatesWithRunnableActivityCount = new HashMap<>();
        HashMap<List<Byte>, HashSet<String>> legalActivitiesExecutedInStates = new HashMap<>();

        // Expand discovered state-space (Here assuming _inputGraph is in its unmodified start-state)
        StoreRunnableActivityCount(seenStatesWithRunnableActivityCount, DcrGraph.HashDcrGraph(graph, null), graph.GetRunnableActivities().size());

        for (LogTrace logTrace : log.getTraces())
        {
            DcrGraph currentGraph = graph.Copy();
            currentGraph.setRunning(true);

            for (LogEvent logEvent : logTrace.getEvents())
            {
                try
                {
                    List<Byte> hashedGraphBeforeExecution = DcrGraph.HashDcrGraph(currentGraph, null);
                    if (currentGraph.Execute(currentGraph.getActivity(logEvent.getIdOfActivity())))
                    {
                        List<Byte> hashedGraphAfterExecution = DcrGraph.HashDcrGraph(currentGraph, null);
                        // Store successful choice (execution) of path (option)
                        StoreSuccessfulPathChoice(legalActivitiesExecutedInStates, hashedGraphBeforeExecution, logEvent.getIdOfActivity());
                        // Expand discovered state-space
                        StoreRunnableActivityCount(seenStatesWithRunnableActivityCount, hashedGraphAfterExecution, currentGraph.GetRunnableActivities().size());
                    }
                }
                catch (Exception e)
                {
                    // No such activity exists, ignore
                }
            }
        }

        // Sum up resulting values
        int legalActivitiesThatCouldHaveBeExecuted = seenStatesWithRunnableActivityCount.values().stream().mapToInt(x -> x).sum();
        int legalActivitiesExecuted = legalActivitiesExecutedInStates.values().stream().mapToInt(HashSet::size).sum();

        if (legalActivitiesThatCouldHaveBeExecuted == 0)
        {
            //this means that we don't allow any activities to be executed ('everything is illegal' or empty graph)
            //and that we don't execute anything (empty log)
            //we also avoid division by 0
            return 100.0;
        }
        double d1 = legalActivitiesExecuted;
        double d2 = legalActivitiesThatCouldHaveBeExecuted;

        double res = (d1 / d2) * 100.0;
        return res;

        //return ((double) legalActivitiesExecuted / (legalActivitiesThatCanBeExecuted + illegalActivitiesExecuted)) * 100.0;
    }

    private static void StoreSuccessfulPathChoice(HashMap<List<Byte>, HashSet<String>> byteArrToStrings, List<Byte> byteArr, String val)
    {
        if (byteArrToStrings.containsKey(byteArr))
        {
            // Add given value
            byteArrToStrings.get(byteArr).add(val);
        }
        else
        {
            // Initialize set of Strings with given value
            byteArrToStrings.put(byteArr, new HashSet<>(Collections.singletonList(val)));
        }
    }

    private static void StoreRunnableActivityCount(HashMap<List<Byte>, Integer> byteArrToInt, List<Byte> byteArr, Integer val)
    {
        if (!byteArrToInt.containsKey(byteArr))
        {
            byteArrToInt.put(byteArr, val);
        }
        // Otherwise do nothing - the value has been stored previously
    }
}
