/*
 * ChartDrawer.java
 *
 * Created on August 3, 2006, 10:21 AM
 */
package viskit.xsd.assembly;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.chart.ChartUtilities;
import java.io.File;
import java.io.FileOutputStream;
import java.io.*;

/**
 *
 * This class creates chart objects using the JFreeChart package.  
 *
 * @author Patrick Sullivan
 * @version $Id$
 */
public class ChartDrawer {

    /** Creates a new instance of ChartDrawer */
    public ChartDrawer() {
    }

    /**
     * Creates a histogram image in PNG format based on the parameters provided.
     *
     *@param data an array of doubles that are to be plotted
     *@param fileName the name of the file to save the image out to
     *@return the path name of the created object
     */
    public String createHistogram(String title, String label, double[] data, String fileName) {
        String fileLocation = "./AnalystReports/charts/" + fileName + ".png";
        String url = "./charts/" + fileName + ".png";
        IntervalXYDataset dataset = createIntervalXYDataset(label, data);
        try {
            saveChart(createChart(dataset, title, label), fileLocation);
        } catch (java.io.IOException e) {
            System.err.println("Unable to create chart image: " + e.getMessage());
            e.printStackTrace();
        }
        return url;
    }

    /**
     * Creates a data set that is used for making the histogram
     */
    private IntervalXYDataset createIntervalXYDataset(String label, double[] data) {

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries(label, data, 15);

        return dataset;
    }

    /**
     * Creates the histogram chart
     */
    private JFreeChart createChart(IntervalXYDataset dataset, String title, String label) {
        final JFreeChart chart = ChartFactory.createHistogram(title,
                label,
                "",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);

        chart.getXYPlot().setForegroundAlpha(0.75f);
        return chart;
    }

    /**
     * Saves a chart to PNG format
     */
    private void saveChart(JFreeChart chart, String path) throws FileNotFoundException, IOException {

        File outFile = new File(path);
        FileOutputStream fos = new FileOutputStream(outFile);
        ChartUtilities.saveChartAsPNG(outFile, chart, 969, 641);
        fos.close();
    }
}