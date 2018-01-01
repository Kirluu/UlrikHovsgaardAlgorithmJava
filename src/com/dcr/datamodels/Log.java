package com.dcr.datamodels;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XParserRegistry;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Log {
    public String Id;
    public String getId() { return Id; }
    public void setId(String value) { Id = value; }

    private final HashSet<LogEvent> Alphabet = new HashSet<>();
    public HashSet<LogEvent> getAlphabet() { return new HashSet<>(Alphabet); }

    private final List<LogTrace> Traces = new ArrayList<>();
    public List<LogTrace> getTraces() { return Traces; }

    public void AddTrace(LogTrace trace)
    {
        Alphabet.addAll(trace.getEvents());
        Traces.add(trace);
    }

    public void AddEventToTrace(String traceId, LogEvent eve) throws Exception {
        List<LogTrace> tracesWithId = Traces.stream().filter(x -> x.getId().equals(traceId)).collect(Collectors.toList());
        if (tracesWithId.size() > 0)
        {
            tracesWithId.get(0).getEvents().add(eve);
        }
        else
        {
            throw new Exception("No such trace");
        }
    }

    public Log FilterByActor(String actorName)
    {
        Log newLog = new Log();

        for (LogTrace trace : Traces.stream()
                .filter(t -> t.getEvents().stream().anyMatch(e -> e.getActorName().equals(actorName)))
                .collect(Collectors.toList()))
        {
            newLog.AddTrace(trace);
        }

        return newLog;
    }

    public Log FilterByNoOfActivities(int maxActivities)
    {
        Log newLog = new Log();

        for (LogTrace trace : Traces.stream()
                .filter(t -> t.getEvents().stream().distinct().count() <= maxActivities)
                .collect(Collectors.toList()))
        {
            newLog.AddTrace(trace);
        }

        return newLog;
    }

    public static XLog parseXESLog(String filename) {
        try {
            File sourceFile = new File(filename);
            for (XParser parser : XParserRegistry.instance().getAvailable()) {
                if (parser.canParse(sourceFile)) {
                    System.out.println("Using input parser: " + parser.name());
                    List<XLog> logs = parser.parse(sourceFile);

                    if (!logs.isEmpty())
                        return logs.get(0);

                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static XLog parseXESLogFirst(String filename) {
        try {
            XFactory factory = XFactoryRegistry.instance().currentDefault();
            XParser parser;
            if (filename.toLowerCase().endsWith(".xes") || filename.toLowerCase().endsWith(".xez")
                    || filename.toLowerCase().endsWith(".xes.gz")) {
                parser = new XesXmlParser(factory);
            } else {
                parser = new XMxmlParser(factory);
            }
            System.out.println("Using input parser: " + parser.name());
            List<XLog> xLogs = parser.parse(new File(filename));
            if (xLogs == null || xLogs.isEmpty())
                return null;

            return xLogs.get(0); // Assume first one is the log we want, if multiple
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Log parseLog(String pathToFile) {
        XLog xLog = parseXESLog(pathToFile);
        if (xLog == null) {
            return null;
        }
        try {
            Log log = new Log();
            // Add traces
            for (XTrace xTrace : xLog) {
                LogTrace trace = new LogTrace();
                trace.setId(UUID.randomUUID().toString()); // Give globally unique ID
                // Add events
                for (XEvent xEvent : xTrace) {
                    String idAndName = xEvent.getAttributes().get("concept:name").toString();
                    trace.Add(new LogEvent(idAndName, idAndName));
                }
                log.AddTrace(trace);
            }

            return log;
        }
        catch (Exception e) {
            System.out.println("Failed to parse log using XES-parser!" + e);
            return null;
        }
    }

    public static String ExportToXml(Log log)
    {
        String logXml = "<log>\n";

        for (LogTrace trace : log.Traces)
        {
            logXml += "\t<trace>\n";
            logXml += String.format("\t\t<String key=\"id\" value=\"{0}\"/>\n", trace.getId());
            for (LogEvent logEvent : trace.getEvents())
            {
                logXml += "\t\t<event>\n";
                logXml += String.format("\t\t\t<String key=\"id\" value=\"{0}\"/>\n", logEvent.getIdOfActivity());
                logXml += String.format("\t\t\t<String key=\"name\" value=\"{0}\"/>\n", logEvent.getName());
                logXml += String.format("\t\t\t<String key=\"roleName\" value=\"{0}\"/>\n", logEvent.getRoleName());
                logXml += "\t\t</event>\n";
            }
            logXml += "\t</trace>\n";
        }
        logXml += "</log>";
        return logXml;
    }
}
