package com.dcr.datamodels;

public class QualityDimensions {

    private double Fitness;
    public double getFitness() { return Fitness; }
    public void setFitness(double value) { Fitness = value; }

    private double Precision;
    public double getPrecision() { return Precision; }
    public void setPrecision(double value) { Precision = value; }

    private double Simplicity;
    public double getSimplicity() { return Simplicity; }
    public void setSimplicity(double value) { Simplicity = value; }

    @Override
    public String toString() {
        return String.format("Fitness:\t\t %f | \nPrecision:\t %f | \nSimplicity:\t %f",
                Fitness, Precision, Simplicity);
    }
}
