package com.dcr.export;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.DcrGraph;
import com.dcr.statistics.Confidence;
import com.dcr.statistics.Threshold;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> condition : graph.getConditions().entrySet()) // TODO: To ensure proper XML-comparison, need to sort iteration of relations
        {
            for (Activity target : DcrGraph.FilterHashMapByThreshold(condition.getValue()))
            {
                xml += String.format("<condition sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\"  />\n", condition.getKey().getId(), target.getId());
            }
        }
        xml += "</conditions>\n";

        // Responses
        xml += "<responses>\n";
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> response : graph.getResponses().entrySet())
        {
            for (Activity target : DcrGraph.FilterHashMapByThreshold(response.getValue()))
            {
                xml += String.format("<response sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", response.getKey().getId(), target.getId());
            }
        }
        xml += "</responses>\n";

        // Excludes
        xml += "<excludes>\n";
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> exclusion : graph.getIncludeExcludes().entrySet())
        {
            for (Map.Entry<Activity, Confidence> target : exclusion.getValue().entrySet())
            {
                if (target.getValue().get() <= Threshold.getValue()) // If it is an exclusion
                {
                    xml += String.format("<exclude sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", exclusion.getKey().getId(), target.getKey().getId());
                }
            }
        }
        xml += "</excludes>\n";

        // Includes
        xml += "<includes>\n";
        for (Map.Entry<Activity, HashMap<Activity, Confidence>> inclusion : graph.getIncludeExcludes().entrySet())
        {
            for (Map.Entry<Activity, Confidence> target : inclusion.getValue().entrySet())
            {
                if (target.getValue().get() > Threshold.getValue() && !inclusion.getKey().equals(target.getKey())) // If it is an inclusion and source != target (avoid self-inclusion)
                {
                    xml += String.format("<include sourceId=\"%s\" targetId=\"%s\" filterLevel=\"1\"  description=\"\"  time=\"\"  groups=\"\" />\n", inclusion.getKey().getId(), target.getKey().getId());
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
        for (Activity activity : graph.getActivities())
        {
            if (activity.isExecuted())
            {
                xml += String.format("<event id=\"%s\"/>\n", activity.getId());
            }
        }
        xml += "</executed>\n";
        // Incuded events
        xml += "<included>\n";
        for (Activity activity : graph.getActivities())
        {
            if (activity.isIncluded())
            {
                xml += String.format("<event id=\"%s\"/>\n", activity.getId());
            }
        }
        xml += "</included>\n";
        // Pending events
        xml += "<pendingResponses>\n";
        for (Activity activity : graph.getActivities())
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
