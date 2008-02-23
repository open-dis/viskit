package viskit.xsd.assembly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.chart.ChartUtilities;

/**
 *
 * This class creates chart objects using the JFreeChart package.  
 *
 * @author Patrick Sullivan
 * @version $Id: ChartDrawer.java 1662 2007-12-16 19:44:04Z tdnorbra $
 * @since August 3, 2006, 10:21 AM
 */
public class ChartDrawer {

    /** Creates a new instance of ChartDrawer */
    public ChartDrawer() {
    }

    /**
     * Creates a histogram image in PNG format based on the parameters provided.
     *
     * @param title 
     * @param label 
     * @param data an array of doubles that are to be plotted
     * @param fileName the name of the file to save the image out to
     * @param binNum the number of replications in the experiment
     * @return the path name of the created object
     */
    public String createHistogram(String title, String label, double[] data, String fileName, int binNum) {
        String fileLocation = "./AnalystReports/charts/" + fileName + ".png";
        String url = "./charts/" + fileName + ".png";
        IntervalXYDataset dataset = createIntervalXYDataset(label, data, binNum);
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
     * @param label
     * @param data
     * @param binNum the number of replications in the experiment
     * @return
     */
    private IntervalXYDataset createIntervalXYDataset(String label, double[] data, int binNum) {

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        
        // Bin size = the number of experiment replications
        dataset.addSeries(label, data, binNum);

        return dataset;
    }

    /**
     * Creates the histogram chart
     * @param dataset
     * @param title
     * @param label
     * @return 
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
     * @param chart
     * @param path
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException 
     */
    private void saveChart(JFreeChart chart, String path) throws FileNotFoundException, IOException {

        File outFile = new File(path);
        FileOutputStream fos = new FileOutputStream(outFile);
        ChartUtilities.saveChartAsPNG(outFile, chart, 969, 641);
        fos.close();
    }
}