package org.example;

public class ProcessingSettings {
    public double hueTolerance = 16.0;
    public double saturationTolerance = 0.30;
    public double brightnessTolerance = 0.30;

    public double minSaturation = 0.18;
    public double minBrightness = 0.15;

    public int minClusterSize = 25;
    public int maxClusterSize = 100000;
}
