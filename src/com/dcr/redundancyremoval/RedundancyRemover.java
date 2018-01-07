package com.dcr.redundancyremoval;

import com.dcr.datamodels.*;
import com.dcr.traversal.UniqueTraceFinder;
import com.sun.istack.internal.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RedundancyRemover {
    // public event Action<string> ReportProgress; // TODO: Java event listeners...

    private UniqueTraceFinder _uniqueTraceFinder;
    private DcrGraph _originalInputDcrGraph;

    private DcrGraph OutputDcrGraph;
    public DcrGraph getOutputDcrGraph() { return OutputDcrGraph; }

    private int RedundantRelationsFound;
    public int getRedundantRelationsFound() { return RedundantRelationsFound; }

    public List<Relation> IncludesRemoved = new ArrayList<>();
    public List<Relation> ExcludesRemoved = new ArrayList<>();
    public List<Relation> ResponsesRemoved = new ArrayList<>();
    public List<Relation> ConditionsRemoved = new ArrayList<>();

    private int RedundantActivitiesFound;
    public int getRedundantActivitiesFound() { return RedundantActivitiesFound; }

    public DcrGraph RemoveRedundancy(DcrGraph inputGraph) throws Exception {
        DcrGraph graph = RemoveRedundancyInner(inputGraph, null);
        return graph;
    }

    public DcrGraph RemoveRedundancyInner(DcrGraph inputGraph, ByteDcrGraph byteDcrFormat) throws Exception {
        RedundantRelationsFound = 0;
        RedundantActivitiesFound = 0;

        //TODO efficiency: use an algorithm to check if the graph is connected and if not then recursively remove redundancy on the subgraphs.
        DcrGraph copy = inputGraph.Copy();

        ByteDcrGraph byteDcrGraph = new ByteDcrGraph(copy, byteDcrFormat);

        _uniqueTraceFinder = new UniqueTraceFinder(byteDcrGraph);

        _originalInputDcrGraph = copy.Copy();
        OutputDcrGraph = copy;

        // Try to remove entire activities at a time and see if the unique traces acquired are the same as the original:
        for (Activity activity : OutputDcrGraph.getActivities()) {
            ByteDcrGraph graphCopy = byteDcrGraph.Copy();

            graphCopy.RemoveActivity(activity.getId());

            //ReportProgress?.Invoke("Removing Activity " + activity.Id); // Java event todo...

            // Compare unique traces - if equal activity is redundant
            if (_uniqueTraceFinder.CompareTraces(graphCopy))
            {
                // The activity is redundant: Remove it from Output graph (also removing all involved relations (thus also redundant))
                RedundantRelationsFound += OutputDcrGraph.RemoveActivity(activity.getId());

                RedundantActivitiesFound++;
            }
        }

        // Remove relations and see if the unique traces acquired are the same as the original. If so, the relation is clearly redundant and is removed immediately
        // All the following calls can alter the "OutputDcrGraph"
        HashSet<Relation> res = RemoveRedundantRelations(RelationType.Response, byteDcrFormat);

        res.addAll(RemoveRedundantRelations(RelationType.Condition, byteDcrFormat));

        res.addAll(RemoveRedundantRelations(RelationType.Inclusion, byteDcrFormat)); // Handles inclusions + exclusions

        
        return OutputDcrGraph;
    }

    private HashSet<Relation> RemoveRedundantRelations(RelationType relationType, ByteDcrGraph byteDcrFormat)
    {
        HashSet<Relation> relationsNotDiscovered = new HashSet<>();
        // Determine method input
        HashMap<Activity, HashSet<Activity>> relationHashMap = new HashMap<>();
        switch (relationType)
        {
            case Response:
                relationHashMap = new HashMap<>(_originalInputDcrGraph.getResponses().entrySet().stream()
                        .map(a -> new AbstractMap.SimpleEntry<>(a.getKey(), DcrGraph.FilterHashMapByThreshold(a.getValue())))
                        .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())));
            break;
            case Condition:
                relationHashMap = new HashMap<>(_originalInputDcrGraph.getConditions().entrySet().stream()
                        .map(a -> new AbstractMap.SimpleEntry<>(a.getKey(), DcrGraph.FilterHashMapByThreshold(a.getValue())))
                        .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())));
                break;
            case Inclusion:
            case Exclusion:
                // Convert HashMap<Activity, HashMap<Activity, Confidence>> to HashMap<Activity, HashSet<Activity>> without removing anything
                relationHashMap = DcrGraph.ConvertToHashMapActivityHashSetActivity(_originalInputDcrGraph.getIncludeExcludes()); // No thresholding to check - either Exclusion or Inclusion
                break;
        }


        // Remove relations and see if the unique traces acquired are the same as the original. If so, the relation is clearly redundant
        for (Map.Entry<Activity, HashSet<Activity>> relation : relationHashMap.entrySet()) {
            Activity source = relation.getKey();

            for (Activity target : relation.getValue()) {
                //ReportProgress?.Invoke("Removing " + relationType + " from " + source.Id + " to " + target.Id); // Java events todo...

                DcrGraph copy = OutputDcrGraph.Copy(); // "Running copy"
                Activity copyTarget = copy.getActivity(target.getId());

                Boolean isInclude = null;

                // Attempt to remove the relation
                switch (relationType) {
                    case Response:
                        copy.getResponses().get(copy.getActivity(source.getId())).remove(copyTarget);
                        break;
                    case Condition:
                        copy.getConditions().get(copy.getActivity(source.getId())).remove(copyTarget);
                        break;
                    case Inclusion:
                    case Exclusion:
                        isInclude = copy.getIncludeExcludes().get(copy.getActivity(source.getId())).get(copyTarget).isAboveThreshold();
                        copy.getIncludeExcludes().get(copy.getActivity(source.getId())).remove(copyTarget);
                        break;
                }

                // Compare unique traces - if equal (true), relation is redundant
                if (_uniqueTraceFinder.CompareTraces(new ByteDcrGraph(copy, byteDcrFormat)))
                {
                    // The relation is redundant, replace running copy with current copy (with the relation removed)
                    OutputDcrGraph = copy;

                    // Report finding:
                    switch (relationType) {
                        case Response:
                            ResponsesRemoved.add(new Relation(RelationType.Response, source, copyTarget));
                            break;
                        case Condition:
                            ConditionsRemoved.add(new Relation(RelationType.Condition, source, copyTarget));
                            break;
                        case Inclusion:
                        case Exclusion:
                            if (isInclude == null) break;
                            if (isInclude) { IncludesRemoved.add(new Relation(RelationType.Inclusion, source, copyTarget)); }
                            else { ExcludesRemoved.add(new Relation(RelationType.Exclusion, source, copyTarget)); }
                            break;
                    }

                    RedundantRelationsFound++;
                }
            }
        }
        return relationsNotDiscovered;
    }
}
