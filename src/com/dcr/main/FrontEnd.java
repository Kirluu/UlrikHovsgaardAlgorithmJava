package com.dcr.main;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.DcrGraph;
import com.dcr.datamodels.Log;
import com.dcr.datamodels.QualityDimensions;
import com.dcr.export.DcrGraphExporter;
import com.dcr.processmining.ContradictionApproach;
import com.dcr.qualitydimensions.QualityDimensionsRetriever;
import com.dcr.redundancyremoval.RedundancyRemover;
import com.dcr.statistics.Threshold;

import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

// Run as console application, otherwise to be used as inspiration on how to use the miner, etc.
public class FrontEnd {
    public static void main(String[] args) {
        boolean intendedExit = doMainLogic(args);

        // Wait for user to manually exit (allow user to read/copy output)
        if (!intendedExit)
            new Scanner(System.in).nextLine();
    }

    // Returns whether the user manually chose to exit
    private static boolean doMainLogic(String[] args) {
        if (args.length != 1) {
            System.out.println("Please supply a path to a log-file as a single argument.");
            return false;
        }

        System.out.println("Parsing log...");
        //Log log = LoadLogXES("D:\\Downloads\\Firefox Downloads\\SepsisCaseLog.xes"); // TODO: Replace with args[0]
        Log log = LoadLogXES(args[0]);
        if (log == null) return false;

        System.out.println("Process-mining graph from log...");
        DcrGraph graph = MineLog(log);
        if (graph == null) return false;

        // Get measures:
        QualityDimensions measuresBefore = GetMeasuresForGraph(graph, log);
        if (measuresBefore == null) return false;

        System.out.println("Removing redundancy from graph...");
        DcrGraph rrGraph = RemoveRedundancyForGraph(graph);
        if (rrGraph == null) return false;

        // Get measures for redundancy-removed graph:
        QualityDimensions measuresAfter = GetMeasuresForGraph(rrGraph, log);
        if (measuresAfter == null) return false;

        // Export pre-graph to XML with measures:
        String graphXMLWithMeasures = ExportGraphToXMLWithQualityMeasures(graph, measuresBefore);
        // Export post-graph to XML with measures:
        String rrGraphXMLWithMeasures = ExportGraphToXMLWithQualityMeasures(rrGraph, measuresAfter);

        while (true) {
            System.out.println("-------------------------------------------------------------------------------------");
            System.out.println("What would you like to see?");
            System.out.println("[1]: The mined graph's XML");
            System.out.println("[2]: The mined, redundancy-removed graph's XML");
            System.out.println("[3]: The mined graph's quality-measures");
            System.out.println("[4]: The mined, redundancy-removed graph's quality-measures");
            System.out.println("[5]: The mined graph's relation-counts etc.");
            System.out.println("[6]: The mined, redundancy-removed graph's relation-counts etc.");
            System.out.println("[exit]: Exit this program");

            String input = new Scanner(System.in).nextLine();
            if (input.contains("1")) {
                System.out.println(graphXMLWithMeasures);
            }
            else if (input.contains("2")) {
                System.out.println(rrGraphXMLWithMeasures);
            }
            else if (input.contains("3")) {
                System.out.println(measuresBefore);
            }
            else if (input.contains("4")) {
                System.out.println(measuresAfter);
            }
            else if (input.contains("5")) {
                System.out.println(graph.GetRelationCountsEtcString());
            }
            else if (input.contains("6")) {
                System.out.println(rrGraph.GetRelationCountsEtcString());
            }
            else if(input.toLowerCase().contains("exit")) {
                return true;
            }
            else {
                System.out.println("Command not recognized. Please try again.");
            }
        }
    }

    /// ----------------------------------------------------------------------------------------------------------------
    /// FUNCTIONS TO BE USED IN COMBINATION TO SERVE THE DESIRED ProM Tool FUNCTIONALITY:
    /// ----------------------------------------------------------------------------------------------------------------

    public static Log LoadLogXES(String pathToFile) {
        try { return Log.parseLog(pathToFile); }
        catch (Exception e) {
            System.out.println(String.format("Failed to parse the file \"%s\". Exception as follows:", pathToFile));
            e.printStackTrace();
            return null;
        }
    }

    public static DcrGraph MineLog(Log log) {
        List<Activity> activities = log.getAlphabet().stream()
                .map(ev -> new Activity(ev.getIdOfActivity()))
                .collect(Collectors.toList());

        ContradictionApproach miner = new ContradictionApproach(new HashSet<>(activities));
        miner.addLog(log);
        return miner.getMinedGraph();
    }

    public static DcrGraph RemoveRedundancyForGraph(DcrGraph graph) {
        try {
            return new RedundancyRemover().RemoveRedundancy(graph);
        }
        catch (Exception e) {
            System.out.println("Failed to remove redundancy.");
            e.printStackTrace();
            return null;
        }
    }

    public static QualityDimensions GetMeasuresForGraph(DcrGraph graph, Log log) {
        try { return QualityDimensionsRetriever.Retrieve(graph, log); }
        catch (Exception e) {
            System.out.println("Failed to fetch quality measures");
            e.printStackTrace();
            return null;
        }
    }

    public static String ExportGraphToXML(DcrGraph graph) {
        return DcrGraphExporter.ExportToXml(graph);
    }

    public static String ExportGraphToXMLWithQualityMeasures(DcrGraph graph, QualityDimensions measures) {
        return new MiningResult(graph, measures).ExportToXml();
    }

    /// ----------------------------------------------------------------------------------------------------------------
    /// END OF FUNCTIONS TO BE USED IN COMBINATION TO SERVE THE DESIRED ProM Tool FUNCTIONALITY:
    /// ----------------------------------------------------------------------------------------------------------------
}
