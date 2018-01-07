package com.dcr.traversal;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.ByteDcrGraph;
import com.dcr.datamodels.DcrGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class UniqueStateFinder {
    public static Integer Counter;

    private static List<DcrGraph> _seenStates;
    private static HashMap<List<Byte>, Integer> _seenStatesWithRunnableActivityCount;


    public static HashMap<List<Byte>, Integer> GetUniqueStatesWithRunnableActivityCount(DcrGraph inputGraph)
    {
        // Start from scratch
        _seenStates = new ArrayList<>();
        _seenStatesWithRunnableActivityCount = new HashMap<>();

        FindUniqueStatesInclRunnableActivityCountDepthFirstBytes(new ByteDcrGraph(inputGraph, null));

        return _seenStatesWithRunnableActivityCount;
    }

    private static void FindUniqueStatesInclRunnableActivityCount(DcrGraph inputGraph) throws Exception {
        Counter++;
        HashSet<Activity> activitiesToRun = inputGraph.GetRunnableActivities();
        List<DcrGraph> iterations = new ArrayList<>();

        _seenStates.add(inputGraph);

        List<Byte> hashed = DcrGraph.HashDcrGraph(inputGraph, null);
        if (! _seenStatesWithRunnableActivityCount.containsKey(hashed)) {
            _seenStatesWithRunnableActivityCount.put(hashed, ((int) activitiesToRun.stream().map(Activity::getId).count()));
        }

        for (Activity activity : activitiesToRun) {
            // Spawn new work
            DcrGraph inputGraphCopy = inputGraph.Copy();
            inputGraphCopy.setRunning(true);
            inputGraphCopy.Execute(inputGraphCopy.getActivity(activity.getId()));

            boolean stateSeen = _seenStates.stream().anyMatch(seenState -> seenState.AreInEqualState(inputGraphCopy));
            if (!stateSeen)
            {
                // Register wish to continue
                iterations.add(inputGraphCopy);
            }
        }

        // For each case where we want to go deeper, recurse
        for (DcrGraph unseenState : iterations)
        {
            FindUniqueStatesInclRunnableActivityCount(unseenState);
        }
    }

    private static void FindUniqueStatesInclRunnableActivityCountDepthFirstBytes(ByteDcrGraph inputGraph)
    {
        Counter++;
        List<Integer> activitiesToRun = inputGraph.GetRunnableIndexes();

        List<Byte> clone = new ArrayList<>(inputGraph.getState());
        _seenStatesWithRunnableActivityCount.put(clone, activitiesToRun.size());

        for (int activityIdx : activitiesToRun)
        {
            // Spawn new work
            ByteDcrGraph inputGraphCopy = inputGraph.Copy();
            inputGraphCopy.ExecuteActivity(activityIdx);

            boolean stateSeen = _seenStatesWithRunnableActivityCount.containsKey(inputGraphCopy.getState());
            if (!stateSeen)
            {
                // Register wish to continue
                FindUniqueStatesInclRunnableActivityCountDepthFirstBytes(inputGraphCopy);
            }
        }
    }

    private static void FindUniqueStatesInclRunnableActivityCountDepthFirst(DcrGraph inputGraph) throws Exception {
        Counter++;
        HashSet<Activity> activitiesToRun = inputGraph.GetRunnableActivities();

        List<Byte> hashed = DcrGraph.HashDcrGraph(inputGraph, null);
        _seenStatesWithRunnableActivityCount.put(hashed, (int)activitiesToRun.stream().map(Activity::getId).count());


        for (Activity activity : activitiesToRun)
        {
            // Spawn new work
            DcrGraph inputGraphCopy = inputGraph.Copy();
            inputGraphCopy.setRunning(true);
            inputGraphCopy.Execute(inputGraphCopy.getActivity(activity.getId()));

            boolean stateSeen = _seenStatesWithRunnableActivityCount.containsKey(DcrGraph.HashDcrGraph(inputGraphCopy, null));
            if (!stateSeen)
            {
                // Register wish to continue
                FindUniqueStatesInclRunnableActivityCountDepthFirst(inputGraphCopy);
            }
        }
    }
}
