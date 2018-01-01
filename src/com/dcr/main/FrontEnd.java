package com.dcr.main;

import com.dcr.datamodels.Activity;
import com.dcr.datamodels.DcrGraph;
import com.dcr.datamodels.Log;
import com.dcr.datamodels.QualityDimensions;
import com.dcr.export.DcrGraphExporter;
import com.dcr.processmining.ContradictionApproach;
import com.dcr.statistics.Threshold;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

// Run as console application, otherwise to be used as inspiration on how to use the miner, etc.
public class FrontEnd {
    public static void main(String[] args) {
        doMainLogic(args);

        // Wait for user to manually exit (allow user to read/copy output)
        new Scanner(System.in).nextLine();
    }

    private static void doMainLogic(String[] args) {
        if (args.length != 1) {
            System.out.println("Please supply a path to a log-file as a single argument.");
            return;
        }

        // Parse log:
        Log log;
        try { log = Log.parseLog(args[0]); }
        catch (Exception e) { System.out.println(String.format("Failed to parse the file \"%s\". Exception as follows:\n%s", args[0], e)); return; }

        // Mine log:
        try { System.out.println(RunMiner(log)); }
        catch (Exception e) { System.out.println(String.format("Failed to mine the log. Exception as follows:\n%s", e)); return; }
    }

    public static String RunMiner(Log log) {
        ContradictionApproach miner = new ContradictionApproach(
                new HashSet<>(log.getAlphabet().stream()
                        .map(ev -> new Activity(ev.EventId))
                        .collect(Collectors.toList())));
        miner.addLog(log);
        return DcrGraphExporter.ExportToXml(miner.getMinedGraph());
    }

    public static String MineGraph(String pathToLogFile, double constraintViolationThreshold) {
        Threshold.setValue(constraintViolationThreshold);

        Log log = Log.parseLog(pathToLogFile);

        ContradictionApproach approach = new ContradictionApproach(new HashSet<Activity>(log.getAlphabet().stream()
                .map(logEvent -> new Activity(logEvent.getIdOfActivity(), logEvent.getName()))
                .collect(Collectors.toList())));
        approach.addLog(log);

        DcrGraph graph = approach.getMinedGraph();
        //QualityDimensions measures = QualityDimensionRetriever.Retrieve(graph, log); // TODO: Quality measures

        return new MiningResult(graph, null).ExportToXml();
    }

    /*public static String RemoveRedundancy(String dcrGraphXml) {
        var graph = XmlParser.ParseDcrGraph(dcrGraphXml);

        var redundancyRemovedGraph = new RedundancyRemover().RemoveRedundancy(graph);

        var onlySimplicity = new QualityDimensions
        {
            Fitness = -1,
                    Precision = -1,
                    Generality = -1,
                    Simplicity = QualityDimensionRetriever.GetSimplicityNew(redundancyRemovedGraph)
        };

        return new DCRResult(redundancyRemovedGraph, onlySimplicity).ExportToXml();
    }*/
}
