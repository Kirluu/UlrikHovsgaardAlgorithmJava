package com.dcr.export;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.DcrGraph;
import com.dcr.statistics.Confidence;
import com.dcr.statistics.Threshold;

import java.util.*;
import java.util.stream.Collectors;

public class DcrGraphExporter {

    public static String ExportActivityToXml(Activity act) {
        return String.format("<event id=\"%s\" scope=\"private\" >", act.getId()) +
                "<custom>\n" +
                "<visualization>\n" +
                "<location />\n" +
                "</visualization>\n" +
                "<roles>\n" +
                String.format("<role>%s</role>\n", act.getRoles()) +
                "</roles>\n" +
                "<groups>\n" +
                "<group />\n" +
                "</groups>\n" +
                "<eventType></eventType>\n" +
                "<eventDescription></eventDescription>\n" +
                "<level>1</level>\n" +
                "<eventData></eventData>\n" +
                "</custom>\n" +
                "</event>\n";
    }

    public static String ExportActivityLabelsToXml(Activity act)
    {
        return String.format("<label id =\"%s\"/>\n", act.getName());
    }

    public static String ExportActivityLabelMappingsToXml(Activity act)
    {
        return String.format("<labelMapping eventId =\"%s\" labelId = \"%s\"/>\n", act.getId(), act.getName());
    }

    // Flat XML-export (no fancy nesting)
    public static String ExportToXml(DcrGraph graph)
    {
        Comparator<Activity> comparator = new Comparator<Activity>() {
            public int compare(Activity o1, Activity o2) {
                return o1.getId().compareTo(o2.getId());
            }
        };

        String xml = "<dcrgraph>\n";

        xml += "<specification>\n<resources>\n<events>\n"; // Begin events
        // Event definitions
        for (Activity activity : graph.getActivitiesSortedById())
        {
            xml += ExportActivityToXml(activity);
        }

        xml += "</events>\n"; // End events
        xml += "<subProcesses></subProcesses>\n";
        xml += "<distribution>\n" +
                "<externalEvents></externalEvents>\n" +
                "</distribution>\n";

        // Labels
        xml += "<labels>\n";
        for (Activity activity : graph.getActivitiesSortedById())
        {
            xml += ExportActivityLabelsToXml(activity);
        }
        xml += "</labels>\n";

        // Label mappings
        xml += "<labelMappings>\n";
        for (Activity activity : graph.getActivitiesSortedById())
        {
            xml += ExportActivityLabelMappingsToXml(activity);
        }
        xml += "</labelMappings>\n";

        // Stuff (unsure whether required or not)
        xml += "<expressions></expressions>\n" +
                "<variables></variables>\n" +
                "<variableAccesses>\n" +
                "<writeAccesses />\n" +
                "</variableAccesses>\n" +
                "<custom>\n" +
                "<roles></roles>\n" +
                "<groups></groups>\n" +
                "<eventTypes></eventTypes>\n" +
                "<graphDetails></graphDetails>\n" +
                "<graphFilters>\n" +
                "<filteredGroups></filteredGroups>\n" +
                "<filteredRoles></filteredRoles>\n" +
                "</graphFilters>\n" +
                "</custom>\n" +
                "</resources>\n";

        // Constraints
        xml += "<constraints>\n";
        // Conditions
        xml += "<conditions>\n";
        SortedSet<Activity> conditionKeys = new TreeSet<>(comparator);
        conditionKeys.addAll(graph.getConditions().keySet());
        for (Activity source : conditionKeys) // To ensure proper XML-comparison, need to sort iteration of relations
        {
            HashMap<Activity, Confidence> targets = graph.getConditions().get(source);
            SortedSet<Activity> targetsSorted = new TreeSet<>(comparator);
            targetsSorted.addAll(DcrGraph.FilterHashMapByThreshold(targets));
            for (Activity target : targetsSorted)
            {
                xml += String.format("<condition sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\"  />\n", source.getId(), target.getId());
            }
        }
        xml += "</conditions>\n";

        // Responses
        xml += "<responses>\n";
        SortedSet<Activity> responseKeys = new TreeSet<>(comparator);
        responseKeys.addAll(graph.getResponses().keySet());
        for (Activity source : responseKeys)
        {
            HashMap<Activity, Confidence> targets = graph.getResponses().get(source);
            SortedSet<Activity> targetsSorted = new TreeSet<>(comparator);
            targetsSorted.addAll(DcrGraph.FilterHashMapByThreshold(targets));
            for (Activity target : targetsSorted)
            {
                xml += String.format("<response sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", source.getId(), target.getId());
            }
        }
        xml += "</responses>\n";

        // Excludes
        xml += "<excludes>\n";
        SortedSet<Activity> inclExclKeys = new TreeSet<>(comparator);
        inclExclKeys.addAll(graph.getIncludeExcludes().keySet());
        for (Activity source : inclExclKeys)
        {
            HashMap<Activity, Confidence> targets = graph.getIncludeExcludes().get(source);
            SortedSet<Activity> targetsSorted = new TreeSet<>(comparator);
            targetsSorted.addAll(targets.keySet());
            for (Activity target : targetsSorted)
            {
                Confidence conf = targets.get(target);
                if (conf.get() <= Threshold.getValue()) // If it is an exclusion
                {
                    xml += String.format("<exclude sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", source.getId(), target.getId());
                }
            }
        }
        xml += "</excludes>\n";

        // Includes
        xml += "<includes>\n";
        for (Activity source : inclExclKeys)
        {
            HashMap<Activity, Confidence> targets = graph.getIncludeExcludes().get(source);
            SortedSet<Activity> targetsSorted = new TreeSet<>(comparator);
            targetsSorted.addAll(targets.keySet());
            for (Activity target : targetsSorted)
            {
                Confidence conf = targets.get(target);
                if (conf.get() > Threshold.getValue()) // If it is an inclusion
                {
                    xml += String.format("<include sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", source.getId(), target.getId());
                }
            }
        }
        xml += "</includes>\n";

        // Milestones
        xml += "<milestones>\n";
        xml += "</milestones>\n";
        // Spawns
        xml += "<spawns></spawns>\n";
        xml += "</constraints>\n";
        xml += "</specification>\n";
        // End constraints

        // Start states
        xml += "<runtime>\n" +
                "<marking>\n" +
                "<globalStore></globalStore>\n";
        // Executed events
        xml += "<executed>\n";
        for (Activity activity : graph.getActivitiesSortedById())
        {
            if (activity.isExecuted())
            {
                xml += String.format("<event id=\"%s\"/>\n", activity.getId());
            }
        }
        xml += "</executed>\n";
        // Incuded events
        xml += "<included>\n";
        for (Activity activity : graph.getActivitiesSortedById())
        {
            if (activity.isIncluded())
            {
                xml += String.format("<event id=\"%s\"/>\n", activity.getId());
            }
        }
        xml += "</included>\n";
        // Pending events
        xml += "<pendingResponses>\n";
        for (Activity activity : graph.getActivitiesSortedById())
        {
            if (activity.isPending())
            {
                xml += String.format("<event id=\"%s\"/>\n", activity.getId());
            }
        }
        xml += "</pendingResponses>\n";
        xml += "</marking>\n" +
                "<custom />\n" +
                "</runtime>\n";
        // End start states

        // End DCR Graph
        xml += "\n</dcrgraph>";
        return xml;
    }
}
