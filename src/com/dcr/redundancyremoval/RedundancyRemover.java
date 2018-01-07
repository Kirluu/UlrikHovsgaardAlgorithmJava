package com.dcr.redundancyremoval;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.ByteDcrGraph;
import com.dcr.datamodels.DcrGraph;
import com.dcr.traversal.UniqueTraceFinder;

public class RedundancyRemover {
    public event Action<string> ReportProgress;


    private UniqueTraceFinder _uniqueTraceFinder;
    private DcrGraph _originalInputDcrGraph;
    private BackgroundWorker _worker;

    public DcrGraph OutputDcrGraph { get; private set; }

    public int RedundantRelationsFound { get; set; }

        public List<(Activity, Activity)> IncludesRemoved { get; set; }
        public List<(Activity, Activity)> ExcludesRemoved { get; set; }
        public List<(Activity, Activity)> ResponsesRemoved { get; set; }
        public List<(Activity, Activity)> ConditionsRemoved { get; set; }

    public int RedundantActivitiesFound { get; set; }


    public DcrGraph RemoveRedundancy(DcrGraph inputGraph, BackgroundWorker worker = null)
    {
        var (graph, _) = RemoveRedundancyInner(inputGraph, null, worker);
        return graph;
    }

    public (DcrGraph, HashSet<Relation>) RemoveRedundancyInner(DcrGraph inputGraph, ByteDcrGraph byteDcrFormat = null, BackgroundWorker worker = null, DcrGraphSimple comparisonGraph = null)
    {
        RedundantRelationsFound = 0;
        RedundantActivitiesFound = 0;

        _worker = worker;

        //TODO: use an algorithm to check if the graph is connected and if not then recursively remove redundancy on the subgraphs.
        var copy = inputGraph.Copy();

        // Temporarily remove flower activities.
        //var flowerActivities =
        //    copy.GetActivities().Where(x => x.Included && !copy.ActivityHasRelations(x)).ToList();

        //foreach (var a in flowerActivities)
        //{
        //    copy.RemoveActivity(a.Id);
        //}

        var byteDcrGraph = new ByteDcrGraph(copy, byteDcrFormat);

        _uniqueTraceFinder = new UniqueTraceFinder(byteDcrGraph);

        _originalInputDcrGraph = copy.Copy();
        OutputDcrGraph = copy;


        // Remove relations and see if the unique traces acquired are the same as the original. If so, the relation is clearly redundant and is removed immediately
        // All the following calls potentially alter the OutputDcrGraph

        var res = RemoveRedundantRelations(RelationType.Response, byteDcrFormat, comparisonGraph);

        res.UnionWith(RemoveRedundantRelations(RelationType.Condition, byteDcrFormat, comparisonGraph));

        // Handles inclusions + exclusions
        res.UnionWith(RemoveRedundantRelations(RelationType.Inclusion, byteDcrFormat, comparisonGraph));

        res.UnionWith(RemoveRedundantRelations(RelationType.Milestone, byteDcrFormat, comparisonGraph));



        foreach (var activity in OutputDcrGraph.GetActivities())
        {
            var graphCopy = byteDcrGraph.Copy();

            graphCopy.RemoveActivity(activity.Id);

            ReportProgress?.Invoke("Removing Activity " + activity.Id);

            // Compare unique traces - if equal activity is redundant
            if (_uniqueTraceFinder.CompareTraces(graphCopy))
            {
                // The activity is redundant: Remove it from Output graph (also removing all involved relations (thus also redundant))
                RedundantRelationsFound += OutputDcrGraph.RemoveActivity(activity.Id);

                RedundantActivitiesFound++;
            }
        }


        //foreach (var a in flowerActivities)
        //{
        //    OutputDcrGraph.AddActivity(a.Id, a.Name);
        //    OutputDcrGraph.SetIncluded(a.Included, a.Id);
        //    OutputDcrGraph.SetPending(a.Pending, a.Id);
        //}
        //var nested = DcrGraphExporter.ExportToXml(OutputDcrGraph);


        return (OutputDcrGraph, res);
    }

    private HashSet<Relation> RemoveRedundantRelations(RelationType relationType, ByteDcrGraph byteDcrFormat, DcrGraphSimple comparisonGraph = null)
    {
        var relationsNotDiscovered = new HashSet<Relation>();
        // Determine method input
        Dictionary<Activity, HashSet<Activity>> relationDictionary = new Dictionary<Activity, HashSet<Activity>>();
        switch (relationType)
        {
            case RelationType.Response:
                relationDictionary = _originalInputDcrGraph.Responses.ToDictionary(a => a.Key, b => DcrGraph.FilterDictionaryByThreshold(b.Value));
                break;
            case RelationType.Condition:
                relationDictionary = _originalInputDcrGraph.Conditions.ToDictionary(a => a.Key, b => DcrGraph.FilterDictionaryByThreshold(b.Value));
                break;
            case RelationType.Milestone:
                relationDictionary = _originalInputDcrGraph.Milestones.ToDictionary(a => a.Key, b => DcrGraph.FilterDictionaryByThreshold(b.Value));
                break;
            case RelationType.Inclusion:
            case RelationType.Exclusion:
                // Convert Dictionary<Activity, Dictionary<Activity, bool>> to Dictionary<Activity, HashSet<Activity>>
                relationDictionary = DcrGraph.ConvertToDictionaryActivityHashSetActivity(_originalInputDcrGraph.IncludeExcludes); // No thresholding to check - either Exclusion or Inclusion
                break;
        }


        // Remove relations and see if the unique traces acquired are the same as the original. If so, the relation is clearly redundant
        foreach (var relation in relationDictionary)
        {
            var source = relation.Key;

            foreach (var target in relation.Value)
            {
#if DEBUG
                //Console.WriteLine("Removing " + relationType + " from " + source.Id + " to " + target.Id + ":");
#endif
                ReportProgress?.Invoke("Removing " + relationType + " from " + source.Id + " to " + target.Id);

                var copy = OutputDcrGraph.Copy(); // "Running copy"
                var copyTarget = copy.GetActivity(target.Id);

                bool? isInclude = null;

                // Attempt to remove the relation
                switch (relationType)
                {
                    case RelationType.Response:
                        copy.Responses[copy.GetActivity(source.Id)].Remove(copyTarget);
                        break;
                    case RelationType.Condition:
                        copy.Conditions[copy.GetActivity(source.Id)].Remove(copyTarget);
                        break;
                    case RelationType.Milestone:
                        copy.Milestones[copy.GetActivity(source.Id)].Remove(copyTarget);
                        break;
                    case RelationType.Inclusion:
                    case RelationType.Exclusion:
                        // TODO: This check and continue gave an incorrect count of redundant relations found
                        //if (source.Id == target.Id) // Assume self-exclude @ equal IDs (Assumption that relation-addition METHODS in DcrGraph have been used to add relations)
                        //{
                        //    continue; // ASSUMPTION: A self-exclude on an activity that is included at some point is never redundant
                        //    // Recall: All never-included activities have already been removed from graph
                        //}

                        isInclude = copy.IncludeExcludes[copy.GetActivity(source.Id)][copyTarget].IsAboveThreshold();
                        if (source.Id == "Appraisal audit" && target.Id == "Make appraisal appointment")
                        {
                            int i = 0;
                        }
                        copy.IncludeExcludes[copy.GetActivity(source.Id)].Remove(copyTarget);
                        break;
                }

                // Compare unique traces - if equal (true), relation is redundant
                if (_uniqueTraceFinder.CompareTraces(new ByteDcrGraph(copy, byteDcrFormat)))
                {
                    // The relation is redundant, replace running copy with current copy (with the relation removed)
                    OutputDcrGraph = copy;

                    // Print about detected relation if not in 'comparisonGraph'
                    if (comparisonGraph != null &&
                            RelationInSimpleDcrGraph(relationType, source, target, comparisonGraph))
                    {
                        var relationString = isInclude != null ? (isInclude == true ? "Include" : "Exclude") : relationType.ToString();

                        relationsNotDiscovered.Add(new Relation(relationString, source, target));
                        //Console.WriteLine(
                        //  $"{relationString} from {source.ToDcrFormatString(false)} " +
                        //$"to {target.ToDcrFormatString(false)} is redundant, but not in comparison-graph!");
                    }

                    RedundantRelationsFound++;
                }
            }
        }
        return relationsNotDiscovered;
    }

    private bool RelationInSimpleDcrGraph(RelationType type, Activity source, Activity target, DcrGraphSimple comparisonGraph)
    {
        switch (type)
        {
            case RelationType.Response:
                return comparisonGraph.Responses.Any(x => x.Key.Id == source.Id && x.Value.Any(y => y.Id == target.Id));

            case RelationType.Condition:
                return comparisonGraph.Conditions.Any(x => x.Key.Id == source.Id && x.Value.Any(y => y.Id == target.Id));

            //case RelationType.Milestone:
            //    return comparisonGraph.Milestones.Any(x => x.Key.Id == source.Id && x.Value.Any(y => y.Id == target.Id));

            case RelationType.Inclusion:
            case RelationType.Exclusion:
                return comparisonGraph.Includes.Any(x => x.Key.Id == source.Id && x.Value.Any(y => y.Id == target.Id))
                        || comparisonGraph.Excludes.Any(x => x.Key.Id == source.Id && x.Value.Any(y => y.Id == target.Id));

            default:
                return true;
        }
    }

    private bool CompareTraceSet(HashSet<List<int>> me, HashSet<List<int>> you)
    {
        if (me.Count != you.Count)
        {
            return false;
        }

        foreach (var list in me)
        {
            you.RemoveWhere(l => CompareTraces(l, list));
        }

        return !you.Any();
    }

    private bool CompareTraces(List<int> me, List<int> you)
    {
        if (me.Count != you.Count)
        {
            return false;
        }

        return !me.Where((t, i) => t != you[i]).Any();
    }
}
