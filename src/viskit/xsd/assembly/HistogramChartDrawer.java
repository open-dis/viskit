package viskit.xsd.assembly;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;

/**
 *
 * This class creates chart objects using the JFreeChart package
 *
 * @author Patrick Sullivan
 * @version $Id$
 * @since August 3, 2006, 10:21 AM
 */
public class HistogramChartDrawer {

    static Logger log = Logger.getLogger(HistogramChartDrawer.class);
    
    /** Creates a new instance of HistogramChartDrawer */
    public HistogramChartDrawer() {}

    /**
     * Creates a histogram image in PNG format based on the parameters provided
     *
     * @param title 
     * @param label 
     * @param data an array of doubles that are to be plotted
     * @param fileName the name of the file to save the image out to
     * @return the path url of the created object
     */
    public String createHistogram(String title, String label, double[] data, String fileName) {
        String baseUrl = "charts/" + fileName + "Histogram.png";
        String chartUrl = "./" + baseUrl;
        String fileLocation = "./AnalystReports/" + baseUrl;
        IntervalXYDataset dataset = createIntervalXYDataset(label, data);
        try {
            saveChart(createChart(dataset, title, "Value"), fileLocation);
        } catch (IOException ioe) {
            log.error("Unable to create chart image: " + ioe.getMessage());
            ioe.printStackTrace();
        }
        return chartUrl;
    }

    /**
     * Creates a data set that is used for making a relative frequency histogram
     * @param label
     * @param data
     * @return
     */
    private IntervalXYDataset createIntervalXYDataset(String label, double[] data) {

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        
        double[] dataCopy = data.clone();
        Arrays.sort(dataCopy);
        double max = dataCopy[dataCopy.length - 1];
        double min = dataCopy[0];
        
        // From: http://www.isixsigma.com/library/forum/c031022_number_bins_histogram.asp
        double result = 1 + (3.3 * Math.log((double) dataCopy.length));
        int binNum = (int) Math.rint(result);
                       
        dataset.addSeries(label, data, binNum, min, max);

        return dataset;
    }

    /**
     * Creates the relative frequency histogram chart
     * @param dataset
     * @param title
     * @param xLabel
     * @return a histogram chart
     */
    private JFreeChart createChart(IntervalXYDataset dataset, String title, String xLabel) {
        final JFreeChart chart = ChartFactory.createHistogram(
                title,
                xLabel,
                "Percentage of Occurrence",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);

        // NOW DO SOME OPTIONAL CUSTOMIZATION OF THE CHART...
        XYPlot plot = (XYPlot) chart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        
        renderer.setDrawBarOutline(true);
        renderer.setSeriesOutlinePaint(0, Color.red);
        plot.setForegroundAlpha(0.75f);

        // set the background color for the chart...
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.white);
        // OPTIONAL CUSTOMIZATION COMPLETED.
        
        return chart;
    }

    /**
     * Saves a chart to PNG format
     * @param chart the created JFreeChart instance
     * @param path the path to save the generated PNG to
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