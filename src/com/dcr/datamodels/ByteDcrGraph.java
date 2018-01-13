package com.dcr.datamodels;

import com.dcr.statistics.Confidence;
import com.dcr.statistics.Threshold;

import java.util.*;
import java.util.stream.Collectors;

import static com.dcr.utils.Utility.CloneByteArray;

/// <summary>
/// A compressed DcrGraph implementation, that can only run activities, and which cannot be dynamically built
/// </summary>
public class ByteDcrGraph
{
    private HashMap<Integer, String> IndexToActivityId = new HashMap<>();
    public HashMap<Integer, String> getIndexToActivityId() { return IndexToActivityId; }

    private HashMap<String, Integer> ActivityIdToIndex = new HashMap<>();
    public HashMap<String, Integer> getActivityIdToIndex() { return ActivityIdToIndex; }

    private List<Byte> State;
    public List<Byte> getState() { return State; }

    private HashMap<Integer, HashSet<Integer>> Includes = new HashMap<>();
    private HashMap<Integer, HashSet<Integer>> Excludes = new HashMap<>();
    private HashMap<Integer, HashSet<Integer>> Responses = new HashMap<>();
    private HashMap<Integer, HashSet<Integer>> ConditionsReversed = new HashMap<>();


    public ByteDcrGraph(DcrGraph inputGraph, ByteDcrGraph comparisonGraph)
    {
        State = DcrGraph.HashDcrGraph(inputGraph, comparisonGraph);

        // Store the activities' IDs for potential lookup later
        if (comparisonGraph != null)
        {
            // Use same mappings
            IndexToActivityId = comparisonGraph.IndexToActivityId;
            ActivityIdToIndex = comparisonGraph.ActivityIdToIndex;
        }
        else
        {
            ArrayList<Activity> sorted = inputGraph.getActivitiesSortedById();
            for (int i = 0; i < sorted.size(); i++)
            {
                IndexToActivityId.put(i, sorted.get(i).getId());
                ActivityIdToIndex.put(sorted.get(i).getId(), i);
            }
        }

        // Set up relations
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> inclExcl : inputGraph.getIncludeExcludes().entrySet())
        {
            int source = ActivityIdToIndex.get(inclExcl.getKey().getId());
            for (Map.Entry<Activity, Confidence> targetPair : inclExcl.getValue().entrySet())
            {
                int target = ActivityIdToIndex.get(targetPair.getKey().getId());

                if (targetPair.getValue().get() > Threshold.getValue())
                {
                    // INCLUSION
                    if (Includes.containsKey(source))
                    {
                        Includes.get(source).add(target);
                    }
                    else
                    {
                        Includes.put(source, new HashSet<>(Arrays.asList(target)));
                    }
                }
                else // EXCLUSION
                {
                    if (Excludes.containsKey(source))
                    {
                        Excludes.get(source).add(target);
                    }
                    else
                    {
                        Excludes.put(source, new HashSet<>(Arrays.asList(target)));
                    }
                }
            }
        }

        for (Map.Entry<Activity, HashMap<Activity, Confidence>> response : inputGraph.getResponses().entrySet())
        {
            int source = ActivityIdToIndex.get(response.getKey().getId());

            for (Activity target : DcrGraph.FilterHashMapByThreshold(response.getValue()))
            {
                int targetIdx = ActivityIdToIndex.get(target.getId());
                if (Responses.containsKey(source))
                {
                    Responses.get(source).add(targetIdx);
                }
                else
                {
                    Responses.put(source, new HashSet<>(Arrays.asList(targetIdx)));
                }
            }
        }

        for (Map.Entry<Activity, HashMap<Activity, Confidence>> condition : inputGraph.getConditions().entrySet())
        {
            int source = ActivityIdToIndex.get(condition.getKey().getId());
            for (Activity target : DcrGraph.FilterHashMapByThreshold(condition.getValue()))
            {
                int targetIdx = ActivityIdToIndex.get(target.getId());
                if (ConditionsReversed.containsKey(targetIdx))
                {
                    ConditionsReversed.get(targetIdx).add(source);
                }
                else
                {
                    ConditionsReversed.put(targetIdx, new HashSet<>(Arrays.asList(source)));
                }
            }
        }
    }

    public ByteDcrGraph Copy()
    {
        return new ByteDcrGraph(this);
    }

    public ByteDcrGraph(ByteDcrGraph copyFrom)
    {
        // Deep copy of state (because value-typed array)
        State = CloneByteArray(copyFrom.getState());

        // Shallow copy of Id correspondences
        IndexToActivityId = copyFrom.IndexToActivityId;
        ActivityIdToIndex = copyFrom.ActivityIdToIndex;

        // Shallow copy of relations
        Includes = copyFrom.Includes;
        Excludes = copyFrom.Excludes;
        Responses = copyFrom.Responses;
        ConditionsReversed = copyFrom.ConditionsReversed;
    }

    public void RemoveActivity(String id)
    {
        int index = ActivityIdToIndex.get(id);

        // Exclude the activity
        State.set(index, (byte)0);
        // Ensure activity can never be included (removing it as an include-target from all activities)
        Includes = new HashMap<>(Includes.entrySet().stream()
                .map(x -> new AbstractMap.SimpleEntry<>(
                        x.getKey(),
                        new HashSet<>(x.getValue().stream().filter(ac -> ac != index).collect(Collectors.toList()))))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())));

        // Thus essentially removed (cheapest method)
    }

    /*private static HashMap<int, HashSet<int>> RemoveIndexFromCollection(HashMap<int, HashSet<int>> dict, int index)
    {
        // Remove imperatively (single-core performance, since nothing else is thread-safe anyways):
        dict.Remove(index);
        for (var kv : dict.ToHashMap(x -> x.Key, x -> x.Value)) // Cloned dictionary to allow modifications to given one
        {
            kv.Value.Remove(index);
            for (var val : kv.Value.OrderBy(x -> x).ToList()) // Ordered and cloned for secure updates
            {
                // For all mappings to indices larger than the removed one - decrease that index by one (remove and re-add)
                if (val > index)
                {
                    dict[kv.Key].Remove(val);
                    dict[kv.Key].Add(val - 1);
                }
            }
        }
        return dict;

        // Approach which creates NEW map:
        return dict.Where(x -> x.Key != index).ToHashMap(v -> v.Key, v -> new HashSet<int>(v.Value.Where(ac -> ac != index)));
    }*/

    public List<Integer> GetRunnableIndexes()
    {
        List<Integer> resList = new ArrayList<>();
        for (int i = 0; i < State.size(); i++)
        {
            if (CanByteRun(State.get(i)))
            {
                resList.add(i);
            }
        }
        return resList;
    }

    private boolean ActivityCanRun(int idx)
    {
        if (!IsByteIncluded(State.get(idx))) return false;
        if (ConditionsReversed.containsKey(idx))
        {
            return ConditionsReversed.get(idx).stream().allMatch(source -> IsByteExcludedOrExecuted(State.get(source)));
        }
        return true;
    }

    /// <summary>
    /// ASSUMES that idx is a runnable activity!
    /// </summary>
    /// <param name="idx">The Byte array index of the activity to be executed</param>
    public void ExecuteActivity(int idx)
    {
        // Executed = true
        int execInt = (State.get(idx)) | (1 << 2);
        State.set(idx, (byte)execInt);                                         // TODO: Does this work in runtime?
        // Pending = false
        int pendingInt = ((State.get(idx)) & (1 ^ Byte.MAX_VALUE));
        State.set(idx, (byte)pendingInt);                                      // TODO: Does this work in runtime?

        // Execute Includes, Excludes, Responses
        if (Includes.containsKey(idx))
        {
            for (int inclusion : Includes.get(idx)) // Outgoing Includes
            {
                SetActivityIncludedExcluded(true, inclusion);
            }
        }
        if (Excludes.containsKey(idx))
        {
            for (int exclusion : Excludes.get(idx)) // Outgoing Includes
            {
                SetActivityIncludedExcluded(false, exclusion);
            }
        }
        if (Responses.containsKey(idx))
        {
            for (int response : Responses.get(idx)) // Outgoing Includes
            {
                SetActivityPending(response);
            }
        }

        for (int i = 0; i < State.size(); i++)
        {
            if (ActivityCanRun(i))
            {
                SetActivityRunnable(i);
            }
            else
            {
                SetActivityNotRunnable(i);
            }
        }
    }

    private void SetActivityIncludedExcluded(boolean include, int idx)
    {
        if (include)
        {
            // Included = true
            int includedInt = ((State.get(idx)) | (1 << 1));
            State.set(idx, (byte)includedInt);                                      // TODO: Does this work in runtime?
        }
        else
        {
            // Included = false
            int includedInt = ((State.get(idx)) & ((1 << 1) ^ Byte.MAX_VALUE));
            State.set(idx, (byte)includedInt);                                      // TODO: Does this work in runtime?

        }
    }

    public static boolean IsFinalState(List<Byte> state) // OK
    {
        // Must not be any activity which is both Pending and Included
        return state.stream().noneMatch(t -> IsByteIncluded(t) && IsBytePending(t));
    }

    private void SetActivityPending(int idx) // OK
    {
        // Pending = true
        int pendingInt = ((State.get(idx)) | 1);
        State.set(idx, (byte)pendingInt);
    }

    private void SetActivityRunnable(int idx)
    {
        int runnableInt = ((State.get(idx)) | (1 << 3));
        State.set(idx, (byte)runnableInt);
    }

    private void SetActivityNotRunnable(int idx)
    {
        int notRunnableInt = ((State.get(idx)) & ((1 << 3) ^ Byte.MAX_VALUE));
        State.set(idx, (byte)notRunnableInt);
    }

    public static boolean IsByteIncluded(Byte b)
    {
        return (b & (1 << 1)) > 0;
    }

    public static boolean CanByteRun(Byte b)
    {
        return (b & (1 << 3)) > 0;
    }

    public static boolean IsBytePending(Byte b)
    {
        return (b & 1) > 0;
    }

    // FALSE = Condition is binding and the target cannot execute
    public static boolean IsByteExcludedOrExecuted(Byte b)
    {
        return (b & (1 << 1)) <= 0 || (b & (1 << 2)) > 0;
    }

    public static boolean IsByteExcludedOrNotPending(Byte b)
    {
        return (b & (1 << 1)) <= 0 || (b & 1) <= 0;
    }

    public static List<Byte> StateWithExcludedActivitiesEqual(List<Byte> bytes)
    {
        List<Byte> retB = new ArrayList<Byte>(bytes.size());
        for (Byte b : bytes)
        {
            retB.add((byte)(IsByteIncluded(b) ? b : 0));
        }
        return retB;
    }
}
