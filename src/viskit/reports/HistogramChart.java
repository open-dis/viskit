package viskit.reports;

import edu.nps.util.Log4jUtilities;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.IntervalXYDataset;
import static viskit.ViskitProject.ANALYST_REPORTS_DIRECTORY_NAME;
import static viskit.ViskitProject.ANALYST_REPORT_CHARTS_DIRECTORY_NAME;
import viskit.ViskitUserConfiguration;

/**
 *
 * This class creates chart objects using the JFreeChart package
 *
 * @author Patrick Sullivan
 * @version $Id$
 * @since August 3, 2006, 10:21 AM
 */
public class HistogramChart
{
    static final Logger LOG = LogManager.getLogger();

    /** Creates a new instance of HistogramChart */
    public HistogramChart() {}

    /**
     * Creates a histogram image in PNG format based on the parameters provided
     *
     * @param title the title of the Histogram
     * @param plotLabel the label for the Histogram
     * @param dataArray an array of doubles that are to be plotted
     * @return the path url of the created object
     */
    public String createChart(String title, String plotLabel, double[] dataArray) 
    {
        File chartsDirectory = new File(ViskitUserConfiguration.instance().getViskitProjectDirectory(),
                ANALYST_REPORTS_DIRECTORY_NAME + "/" + ANALYST_REPORT_CHARTS_DIRECTORY_NAME);
        File fileLocation = new File(chartsDirectory, plotLabel + "Histogram.png");
//      File fileLocation = new File(viskitProject.getAnalystReportChartsDirectory(), plotLabel + "Histogram.png");
        IntervalXYDataset dataset = createDataset(plotLabel, dataArray);
        saveChart(createChart(dataset, title, "Value"), fileLocation);

        // Return relative path only
        return ANALYST_REPORT_CHARTS_DIRECTORY_NAME + "/" + fileLocation.getName();
    }

    /**
     * Creates a data set that is used for making a relative-frequency histogram.
     * @param label
     * @param data
     * @return
     */
    private IntervalXYDataset createDataset(String label, double[] data)
    {
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);

        double[] dataCopy = data.clone();
        Arrays.sort(dataCopy);
        double max = dataCopy[dataCopy.length - 1];
        double min = dataCopy[0];

        // From: http://www.isixsigma.com/library/forum/c031022_number_bins_histogram.asp
        double result = 1 + (3.3 * Math.log(dataCopy.length));
        int binNum = (int) Math.rint(result); // Math.rint will roundoff double to nearest integer

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
     * @param outFile the path to save the generated PNG to
     */
    private void saveChart(JFreeChart chart, File outFile) {

        try {
            ChartUtils.saveChartAsPNG(outFile, chart, 969, 641);
        } catch (IOException ioe) {
            LOG.error(ioe);
        }
    }
}