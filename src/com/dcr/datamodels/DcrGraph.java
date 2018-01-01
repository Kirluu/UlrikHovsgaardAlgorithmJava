package com.dcr.datamodels;

import com.dcr.statistics.Confidence;
import com.dcr.statistics.Threshold;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dcr.utils.Utility.CloneByteArray;

// NOTE: This DCR graph representation is biased towards statistical storage for the Contradiction-approach process
// mining algorithm, in that there is always either an Include or an Exclude - the non-existence of such is only represented
// if certain activities do not exist in the mapping at all. In other words, statistical knowledge of whether an
// include should exist or whether an exclude should exist does not exist individually, but in unison.
public class DcrGraph {

    private String _title;
    public String getTitle() { return _title; }
    public void setTitle(String value) { _title = value; }

    private HashSet<Activity> Activities = new HashSet<>();
    public HashSet<Activity>  getActivities() { return Activities; }
    public ArrayList<Activity> getActivitiesSortedById() {
        ArrayList<Activity> sortedActivities = new ArrayList<>(getActivities());
        Collections.sort(sortedActivities, Comparator.comparing(Activity::getId));
        return sortedActivities;
    }

    public void setActivities(HashSet<Activity> value) { Activities = value; }

    private HashMap<Activity, HashMap<Activity, Confidence>> Responses = new HashMap<>();
    public HashMap<Activity, HashMap<Activity, Confidence>>  getResponses() { return Responses; }
    public void setResponses(HashMap<Activity, HashMap<Activity, Confidence>> value) { Responses = value; }

    private HashMap<Activity, HashMap<Activity, Confidence>> IncludeExcludes = new HashMap<>(); // Confidence > threshold is include
    public HashMap<Activity, HashMap<Activity, Confidence>>  getIncludeExcludes() { return IncludeExcludes; }
    public void setIncludeExcludes(HashMap<Activity, HashMap<Activity, Confidence>> value) { IncludeExcludes = value; }

    private HashMap<Activity, HashMap<Activity, Confidence>> Conditions = new HashMap<>();
    public HashMap<Activity, HashMap<Activity, Confidence>>  getConditions() { return Conditions; }
    public void setConditions(HashMap<Activity, HashMap<Activity, Confidence>> value) { Conditions = value; }


    private HashMap<Activity, Confidence> PendingStates = new HashMap<>();
    public HashMap<Activity, Confidence> getPendingStates() { return PendingStates; }
    public void setPendingStates(HashMap<Activity, Confidence> value) { PendingStates = value; }

    public HashMap<Activity, Confidence> ExcludedStates = new HashMap<>();
    public HashMap<Activity, Confidence> getExcludedStates() { return ExcludedStates; }
    public void setExcludedStates(HashMap<Activity, Confidence> value) { ExcludedStates = value; }


    public long getConditionsCount()
    {
        return Conditions.values().stream().flatMap(x -> x.values().stream().filter(y -> !y.isAboveThreshold())).count();
    }
    public long getResponsesCount()
    {
        return Responses.values().stream().flatMap(x -> x.values().stream().filter(y -> !y.isAboveThreshold())).count();
    }
    public long getIncludesCount()
    {
        return IncludeExcludes.values().stream().flatMap(x -> x.values().stream().filter(Confidence::isAboveThreshold)).count();
    }
    public long getExcludesCount()
    {
        return IncludeExcludes.values().stream().flatMap(x -> x.values().stream().filter(y -> !y.isAboveThreshold())).count();
    }


    private boolean Running;
    public boolean isRunning() { return Running; }
    public void setRunning(boolean value) { Running = value; }


    public Activity getActivity(String id)
    {
        for(Activity act : Activities)
        {
            if (act.getId() == id)
                return act;
        }
        return null;
    }

    public long getRelationsCount() {
        return getIncludesCount() + getExcludesCount() + getConditionsCount() + getResponsesCount();
    }

    public Activity AddActivity(String id, String name) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity activity = new Activity(id, name);
        Activities.add(activity);

        return activity;
    }

    public Activity AddActivity(String id, String name, String actor) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity activity = new Activity(id, name);
        activity.setRoles(actor);
        Activities.add(activity);
        ExcludedStates.put(activity, new Confidence());
        PendingStates.put(activity, new Confidence());

        return activity;
    }

    public void AddActivities(Activity... activities) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activities.addAll(Arrays.asList(activities));
    }

    /*
    public void AddActivity(String id, String name) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity act = new Activity(id, name);
        Activities.add(act);

        ArrayList<Activity> otherActivities = new ArrayList<>(Activities.stream().filter(x -> !x.equals(act)).collect(Collectors.toList()));

        // Add relation confidences from this newly added activity to all others
        IncludeExcludes.put(act, InitializeConfidenceMapping(otherActivities));
        Responses.put(act, InitializeConfidenceMapping(otherActivities));
        Conditions.put(act, InitializeConfidenceMapping(otherActivities));

        // Also add relations from all other activities to this new one
        for (Activity other : otherActivities)
        {
            IncludeExcludes.get(other).put(act, new Confidence());
            Responses.get(other).put(act, new Confidence());
            Conditions.get(other).put(act, new Confidence());
        }
    } */

    private HashMap<Activity, Confidence> InitializeConfidenceMapping(Iterable<Activity> activities) {
        HashMap<Activity, Confidence> res = new HashMap<>();
        for (Activity act : activities)
        {
            res.put(act, new Confidence());
        }
        return res;
    }

    public void AddRolesToActivity(String id, String roles)
    {
        getActivity(id).setRoles(roles);
    }

    /// <summary>
    /// Removes an activity and all the relations involving it from the DcrGraph.
    /// </summary>
    /// <param name="id">ID of the activity to remove from the graph.</param>
    /// <returns>The amount of relations that were removed by effect of removing the activity and all involved relations.</returns>
    public int RemoveActivity(String id) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity act = getActivity(id);

        Activities.removeAll(Activities.stream().filter(a -> a.getId().equals(id)).collect(Collectors.toList()));

        return RemoveFromRelation(Responses, act)
                + RemoveFromRelation(Conditions, act)
                + RemoveFromRelation(IncludeExcludes, act);
    }

    private static int RemoveFromRelation(HashMap<Activity, HashMap<Activity, Confidence>> relation, Activity act)
    {
        int removedRelations = 0;
        // All relations where act is a target
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> source : relation.entrySet()) {
            if (source.getValue().containsKey(act)) {
                removedRelations++;
                source.getValue().remove(act);
            }
        }

        // All outgoing relations
        HashMap<Activity, Confidence> out = relation.get(act);
        if (out != null) {
            removedRelations += out.keySet().size();
        }
        relation.remove(act);

        return removedRelations;
    }

    public void SetPending(boolean pending, String id) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        getActivity(id).setPending(pending);
    }

    public void SetIncluded(boolean included, String id) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        getActivity(id).setIncluded(included);
    }

    public void SetExecuted(boolean executed, String id) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        getActivity(id).setExecuted(executed);
    }

    // UTILITY-FUNCTION:
    public static void PutOrUpdateRelationMapping(HashMap<Activity, HashMap<Activity, Confidence>> map, Activity source, Activity target, Confidence conf) {
        HashMap<Activity, Confidence> targets = map.get(source);
        if (targets != null) {
            targets.put(target, conf);
        }
        else {
            HashMap<Activity, Confidence> innerMap = new HashMap<>();
            innerMap.put(target, conf);
            map.put(source, innerMap);
        }
    }

    public void AddIncludeExclude(boolean incl, String firstId, String secondId) throws Exception {
        AddIncludeExclude(incl ? new Confidence(1, 1) : new Confidence(),
                firstId,
                secondId);
    }

    private void AddIncludeExclude(Confidence confidence, String firstId, String secondId) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity fstActivity = getActivity(firstId);
        Activity sndActivity = getActivity(secondId);

        PutOrUpdateRelationMapping(IncludeExcludes, fstActivity, sndActivity, confidence);
    }

    //addresponce Condition and milestone should probably be one AddRelation method, that takes an enum.
    public boolean AddResponse(String firstId, String secondId) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        if (firstId == secondId) //because responce to one self is not healthy.
            return false;

        Activity fstActivity = getActivity(firstId);
        Activity sndActivity = getActivity(secondId);

        PutOrUpdateRelationMapping(Responses, fstActivity, sndActivity, new Confidence());

        return true;
    }

    public void AddCondition(String firstId, String secondId) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity fstActivity = getActivity(firstId);
        Activity sndActivity = getActivity(secondId);

        PutOrUpdateRelationMapping(Conditions, fstActivity, sndActivity, new Confidence());
    }

    public void RemoveCondition(String firstId, String secondId) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to remove relations from a Graph, that is Running. :$");

        Activity fstActivity = getActivity(firstId);
        Activity sndActivity = getActivity(secondId);

        HashMap<Activity, Confidence> targets = Conditions.get(fstActivity);
        if (targets != null) {
            targets.remove(sndActivity);
        }
    }

    public void RemoveIncludeExclude(String firstId, String secondId) throws Exception {
        if (Running)
            throw new Exception("It is not permitted to add relations to a Graph, that is Running. :$");

        Activity fstActivity = getActivity(firstId);
        Activity sndActivity = getActivity(secondId);

        HashMap<Activity, Confidence> targets = IncludeExcludes.get(fstActivity);
        if (targets != null) {
            targets.remove(sndActivity);
        }
    }


    public HashSet<Activity> GetRunnableActivities()
    {
        //if the activity is included.
        HashSet<Activity> included = GetIncludedActivities();

        HashSet<Activity> conditionConstrainedTargets = new HashSet<>();
        for (Activity source : included)
        {
            HashMap<Activity, Confidence> unfiltered = Conditions.get(source);

            // no other included and non-executed activity has a condition to it
            if (!source.isExecuted() && unfiltered != null) {
                conditionConstrainedTargets.addAll(FilterHashMapByThreshold(unfiltered));
            }
        }

        included.removeAll(conditionConstrainedTargets);

        return included;
    }

    public boolean Execute(Activity a) throws Exception {
        if(!Running)
            throw new Exception("It is not permitted to execute an Activity on a Graph, that is not Running.");

        if(a == null)
            throw new IllegalArgumentException();

        //if the activity is not runnable
        if (!GetRunnableActivities().contains(a))
            return false;

        Activity act = getActivity(a.getId());

        //the activity is now executed
        act.setExecuted(true);

        //it is not pending
        act.setPending(false);

        //its response relations are now pending.
        HashMap<Activity,Confidence> respTargets = Responses.get(act);
        if (respTargets != null)
        {
            for (Activity respActivity : FilterHashMapByThreshold(respTargets))
            {
                getActivity(respActivity.getId()).setPending(true);
            }
        }

        //its include/exclude relations are now included/excluded.
        HashMap<Activity, Confidence> incExcTargets = IncludeExcludes.get(act);
        if (incExcTargets != null)
        {
            for (Map.Entry<Activity, Confidence> keyValuePair : incExcTargets.entrySet()) {
                boolean isInclusion = keyValuePair.getValue().get() > Threshold.getValue();
                getActivity(keyValuePair.getKey().getId()).setIncluded(isInclusion);
            }
        }

        return true;
    }


    public boolean IsFinalState() {
        return Activities.stream().noneMatch(a -> a.isIncluded() && a.isPending());
    }

    public HashSet<Activity> GetIncludedActivities() {
        HashSet<Activity> activitiesToReturn = new HashSet<>();
        for (Activity act : Activities)
        {
            if (act.isIncluded())
            {
                activitiesToReturn.add(act);
            }
        }
        return activitiesToReturn;
    }

    // UTILITY:
    public static HashSet<Activity> FilterHashMapByThreshold(HashMap<Activity, Confidence> map) {
        return new HashSet<>(map.entrySet().stream().filter(ac -> ac.getValue().get() <= Threshold.getValue()).map(a -> a.getKey()).collect(Collectors.toList()));
    }

    // UTILITY:
    public static HashMap<Activity, Confidence> FilterHashMapByThresholdAsHashMap(HashMap<Activity, Confidence> map) {
        return new HashMap<>(map.entrySet().stream().filter(ac -> ac.getValue().get() <= Threshold.getValue()).collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())));
    }

    public static <T> HashMap<Activity, HashSet<Activity>> ConvertToHashMapActivityHashSetActivity(HashMap<Activity, HashMap<Activity, T>> inputHashMap) {
        HashMap<Activity, HashSet<Activity>> resultHashMap = new HashMap<>();
        for (Map.Entry<Activity, HashMap<Activity, T>> includeExclude : inputHashMap.entrySet())
        {
            Activity source = includeExclude.getKey();
            for (Map.Entry<Activity, T> keyValuePair : includeExclude.getValue().entrySet())
            {
                Activity target = keyValuePair.getKey();
                HashSet<Activity> targets = resultHashMap.get(source);
                if (targets == null) {
                    resultHashMap.put(source, new HashSet<>(Arrays.asList(target)));
                }
                else {
                    resultHashMap.get(source).add(target);
                }
            }
        }
        return resultHashMap;
    }

    public boolean ActivityHasRelations(Activity a) {
        return InRelation(a, IncludeExcludes, true)
                || InRelation(a, Responses, false)
                || InRelation(a, Conditions, false);
    }

    public boolean InRelation(Activity activity, HashMap<Activity, HashSet<Activity>> map) {
        return map.entrySet().stream().anyMatch(x -> x.getKey().equals(activity) && !x.getValue().isEmpty())
                || (map.entrySet().stream().anyMatch(x -> x.getValue().contains(activity)));
    }

    public boolean InRelation(Activity activity, HashMap<Activity, HashMap<Activity, Confidence>> map, boolean isIncludeExclude)
    {
        return
        // any outgoing relations?
                map.entrySet().stream().anyMatch(x -> x.getKey().equals(activity)
                    && (isIncludeExclude && !x.getValue().isEmpty() // <- If "isIncludeExclude", then non-empty means InRelation
                        // else, for reponses or conditions:
                        || x.getValue().entrySet().stream()
                        .anyMatch(y -> !y.getValue().isAboveThreshold()))) // Any not above threshold -> any that is not contradicted -> any relations
        // any incoming relations?
                || (map.entrySet().stream().anyMatch(x -> x.getValue().containsKey(activity) && x.getValue().get(activity).isAboveThreshold()));
    }

    /// <summary>
    /// Enumerates source DcrGraph's activities and looks for differences in states between the source and the target (compared DcrGraph)
    /// </summary>
    /// <param name="comparedDcrGraph">The DcrGraph that the source DcrGraph is being compared to</param>
    /// <returns></returns>
    public boolean AreInEqualState(DcrGraph comparedDcrGraph)
    {
        for (Activity activity : Activities)
        {
            // Get corresponding activity
            Optional<Activity> comparedActivityOpt = comparedDcrGraph.Activities.stream().filter(a -> a.getId().equals(activity.getId())).findFirst();
            if(!comparedActivityOpt.isPresent()) {
                return false;
            }
            Activity comparedActivity = comparedActivityOpt.get();

            // Compare values
            if (activity.isExecuted() != comparedActivity.isExecuted() || activity.isIncluded() != comparedActivity.isIncluded() ||
                    activity.isPending() != comparedActivity.isPending())// ||
                   // ((activity.IsNestedGraph && comparedActivity.IsNestedGraph)
                   //         ? !activity.NestedGraph.AreInEqualState(comparedActivity.NestedGraph)
                   //         : (activity.IsNestedGraph != comparedActivity.IsNestedGraph)))
            {
                return false;
            }
        }
        return true;
    }

    // ASSUMPTION: If any difference in activity-set-size, then comparisonGraph has more activities than inputGraph!
    public static Byte[] HashDcrGraph(DcrGraph graph, ByteDcrGraph comparisonGraph)
    {
        if (comparisonGraph != null)
        {
            return CloneByteArray(comparisonGraph.getState());
        }
        else
        {
            Byte[] array = new Byte[graph.getActivities().size()];
            int i = 0;
            HashSet<Activity> runnableActivities = graph.GetRunnableActivities();
            for (Activity act : graph.getActivitiesSortedById())
            {
                array[i++] = act.getHashedActivity(runnableActivities.contains(act));
            }
            return array;
        }
    }

    private HashMap<Activity, Confidence> CloneActivityConfidenceHashMap(HashMap<Activity, Confidence> original,
                                                                         Function<Activity, Activity> activityGetter) {
        return new HashMap<>(original.entrySet().stream()
                .map(x -> new AbstractMap.SimpleEntry<>(activityGetter.apply(x.getKey()), new Confidence(x.getValue())))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())));
    }

    public DcrGraph Copy()
    {
        DcrGraph newDcrGraph = new DcrGraph();

        // Activities
        newDcrGraph.Activities = CloneActivityHashSet(Activities);

        // Helper-function to look up actvity in the cloned activity-set:
        Function<Activity, Activity> getA = originalAct -> newDcrGraph.getActivity(originalAct.getId());

        // Responses
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> response : Responses.entrySet())
        {
            newDcrGraph.Responses.put(getA.apply(response.getKey()), CloneActivityConfidenceHashMap(response.getValue(), getA));
        }

        // Includes and Excludes
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> inclusionExclusion : IncludeExcludes.entrySet())
        {
            newDcrGraph.IncludeExcludes.put(getA.apply(inclusionExclusion.getKey()), CloneActivityConfidenceHashMap(inclusionExclusion.getValue(), getA));
        }

        // Conditions
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> condition : Conditions.entrySet())
        {
            newDcrGraph.Conditions.put(getA.apply(condition.getKey()), CloneActivityConfidenceHashMap(condition.getValue(), getA));
        }

        return newDcrGraph;
    }

    private HashSet<Activity> CloneActivityHashSet(HashSet<Activity> source)
    {
        HashSet<Activity> result = new HashSet<>();
        for (Activity activity : source)
        {
            result.add(new Activity(activity));
        }
        return result;
    }

    /*public HashSet<Activity> GetIncludeOrExcludeRelation(Activity source, boolean incl)
    {
        HashMap<Activity, Confidence> dict;
        if (IncludeExcludes.TryGetValue(source, out dict))
        {
            HashSet<Activity> set = new HashSet<Activity>();

            foreach (var target in dict)
            {
                if ((target.Value.Get > Threshold.Value) == incl)
                    set.Add(target.Key);
            }

            return set;
        }
        else
        {
            return new HashSet<Activity>();
        }

    }

    private HashMap<TKey, TValue> CloneHashMapCloningValues<TKey, TValue>
   (HashMap<TKey, TValue> original) where TValue : ICloneable
    {
        HashMap<TKey, TValue> ret = new HashMap<TKey, TValue>(original.count(),
                original.Comparer);
        foreach (KeyValuePair<TKey, TValue> entry in original)
        {
            ret.Add(entry.Key, (TValue)entry.Value.Clone());
        }
        return ret;
    }*/
}
