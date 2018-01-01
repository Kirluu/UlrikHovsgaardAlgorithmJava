package com.dcr.main;

import com.dcr.datamodels.DcrGraph;
import com.dcr.datamodels.QualityDimensions;
import com.dcr.export.DcrGraphExporter;

/// <summary>
/// Return format for the library
/// </summary>
public class MiningResult
{
    public MiningResult(DcrGraph graph, QualityDimensions measures)
    {
        DCR = graph;
        Measures = measures;
    }

    public final DcrGraph DCR;
    public final QualityDimensions Measures;

    public String ExportToXml()
    {
        String xml = "<ulrikhovsgaardoutput>\n";

        xml += "\t<measures>\n";
        xml += String.format("\t\t<fitness>%f</fitness>\n", Measures == null ? 0 : Measures.getFitness());
        xml += String.format("\t\t<precision>%f</precision>\n", Measures == null ? 0 : Measures.getPrecision());
        xml += String.format("\t\t<simplicity>%f</simplicity>\n", Measures == null ? 0 : Measures.getSimplicity());
        xml += "\t</measures>\n";

        xml += DcrGraphExporter.ExportToXml(DCR);

        xml += "</ulrikhovsgaardoutput>";

        return xml;
    }
}
