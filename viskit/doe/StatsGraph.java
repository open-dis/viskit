/*
 * StatsGraph.java
 *
 * Created on April 20, 2006, 11:19 AM
 *
 * Shows a JFreeChart Statistics Graph for each named property.
 * Vertical axis bounded by maxObjs and minObs.
 * Horozontal each DesignPoint Statistic as a vertical bar, as these come in
 * the maxObjs and minObs scales are adjusted dynamically.
 * Each vertical bar shows standard deviation.
 * To consider, maybe clicking on a bar brings up replication stats for 
 * that design point with horizontal line across the mean. 
 */

package viskit.doe;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.jfree.data.category.DefaultIntervalCategoryDataset;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.renderer.category.IntervalBarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.util.SortOrder;
import java.util.Hashtable;

public class StatsGraph extends JFrame {
    String[] properties;
    int designPoints;
    int samples;
    Hashtable meanAndStandardDeviations;
    Hashtable minMaxs;
    Hashtable chartPanels;
    JTabbedPane tabbedPane;
    
    public StatsGraph(String expName, final String[] properties, int designPoints, int samples) {
        this("Gridkit Statistics "+expName);
        this.properties = properties;
        this.designPoints = designPoints;
        this.samples = samples;
        tabbedPane = new JTabbedPane();
        meanAndStandardDeviations = new Hashtable();
        minMaxs = new Hashtable();
        chartPanels = new Hashtable();
        for (int i = 0; i < properties.length; i++) {
            System.out.println("StatsGraph: createDataSets for "+properties[i]);
            createDataSets(properties[i]);
            tabbedPane.add(properties[i],(ChartPanel)chartPanels.get(properties[i]));  
        }
        setContentPane(tabbedPane);
        pack();
        show();
    }
    /**
     *
     * @param title  the frame title.
     */
    public StatsGraph(final String title) {
        super(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    // add SampleStatistic to datasets at designPoint d and sample s
    public void addSampleStatistic(viskit.xsd.bindings.assembly.SampleStatisticsType sample, int d, int s) {
        String name = sample.getName();
        DefaultStatisticalCategoryDataset statsData = (DefaultStatisticalCategoryDataset)meanAndStandardDeviations.get(name);
        DefaultIntervalCategoryDataset minMax = (DefaultIntervalCategoryDataset)minMaxs.get(name);
        System.out.println("SampleStatisticType name: "+sample.getName());
        statsData.add(Double.parseDouble(sample.getMean()),Double.parseDouble(sample.getStandardDeviation()),"Design Point "+d,"Sample "+s);
        minMax.setStartValue(d,"Sample "+s,Double.valueOf(sample.getMinObs()));
        minMax.setEndValue(d,"Sample "+s,Double.valueOf(sample.getMaxObs()));
        ((ChartPanel)chartPanels.get(name)).repaint();
    }
    
    public void createDataSets(String name) {
        
        meanAndStandardDeviations.put(name,createStatisticalDataset());
        minMaxs.put(name,createMinMaxDataset());
        
        JFreeChart chart = new JFreeChart("Statistics for "+name, 
                new Font("Helvetica",Font.BOLD, 14),
                new CategoryPlot(),
                false);
       
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanels.put(name,chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(800,600));
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
   
   
        NumberAxis numAx = new NumberAxis("Value of "+name); 
        CategoryItemLabelGenerator generator 
            = new StandardCategoryItemLabelGenerator();
        CategoryItemRenderer renderer;
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        
        plot.setDomainAxis(new CategoryAxis("Latin Hypersquare Samples of Parameters from Design of Experiment Panel"));
        renderer = new StatisticalBarRenderer();
        ((StatisticalBarRenderer)renderer).setItemMargin(.27);
        renderer.setItemLabelsVisible(true);
        renderer.setItemLabelGenerator(generator);
        numAx = new NumberAxis("Value of "+name);
        numAx.setAutoRange(true);
        numAx.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(numAx);
        plot.setDataset((StatisticalCategoryDataset)meanAndStandardDeviations.get(name));
        plot.setRenderer(renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        
        // now minmax
        
        renderer = new IntervalBarRenderer();
        ((IntervalBarRenderer)renderer).setDrawBarOutline(true);
        ((IntervalBarRenderer)renderer).setMaximumBarWidth(1.0);
        renderer.setShape(new java.awt.geom.RoundRectangle2D.Double());
        plot.setDataset(1,(IntervalCategoryDataset)minMaxs.get(name));
        plot.setRenderer(1,renderer);
        plot.setRowRenderingOrder(SortOrder.ASCENDING);
        
    }
    
    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private StatisticalCategoryDataset createStatisticalDataset() {

        DefaultStatisticalCategoryDataset result = new DefaultStatisticalCategoryDataset();
        
        for ( int dp = 0; dp < designPoints; dp ++) {
            for ( int s = 0; s < samples; s++) {
                result.add(0.0,0.0,"Design Point "+dp,"Sample "+s);
            }
        }

        return result;

    }

    private IntervalCategoryDataset createMinMaxDataset() {

        
        String[] seriesKeys = new String[designPoints];
        String[] categoryKeys = new String[samples];
        Double[][] starts = new Double[designPoints][];
        Double[][] ends = new Double[designPoints][];
        for ( int dp = 0; dp < designPoints; dp ++) {
            seriesKeys[dp] = "Design Point "+dp;
            starts[dp] = new Double[samples];
            ends[dp] = new Double[samples];
            for ( int s = 0; s < samples; s++) {
                starts[dp][s] = new Double(0.0);
                ends[dp][s] = new Double(0.001);
                categoryKeys[s] = "Sample "+s; // could de-loop
            }
        }
        DefaultIntervalCategoryDataset result = new DefaultIntervalCategoryDataset(seriesKeys,categoryKeys,starts,ends);
        return result;

    }
    
}
