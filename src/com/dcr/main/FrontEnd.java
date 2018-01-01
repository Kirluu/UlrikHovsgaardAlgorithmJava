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
import java.util.stream.Collectors;

// Run as console application, otherwise to be used as inspiration on how to use the miner, etc.
public class FrontEnd {

    public static void main(String[] args) {

        // TODO: Make smart-ish console front-end

    }

    public static Log ParseLog(String pathToFile) throws Exception {
        return Log.parseLog(pathToFile);
    }

    public static String RunMiner(Log log) {
        ContradictionApproach miner = new ContradictionApproach(
                new HashSet<>(log.getAlphabet().stream()
                        .map(ev -> new Activity(ev.EventId))
                        .collect(Collectors.toList())));
        miner.addLog(log);
        return DcrGraphExporter.ExportToXml(miner.getMinedGraph());
    }

    public static String MineGraph(String pathToLogFile, double constraintViolationThreshold, int nestedGraphMinimumSize) {
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
