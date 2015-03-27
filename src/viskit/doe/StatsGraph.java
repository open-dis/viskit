/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package viskit.doe;

import java.awt.Font;
import java.util.Hashtable;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.renderer.category.IntervalBarRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.category.DefaultIntervalCategoryDataset;

/**
 * Shows a JFreeChart Statistics Graph for each named property.
 * Vertical axis bounded by maxObjs and minObs.
 * Horozontal each DesignPoint Statistic as a vertical bar, as these come in
 * the maxObjs and minObs scales are adjusted dynamically.
 * Each vertical bar shows standard deviation.
 * To consider, maybe clicking on a bar brings up replication stats for
 * that design point with horizontal line across the mean.
 * @author Patrick Sullivan
 * @version $Id$
 * @since April 20, 2006, 11:19 AM
 */
public class StatsGraph extends JPanel {

    String[] properties;
    int designPoints;
    int samples;
    Hashtable<String, DefaultStatisticalCategoryDataset> meanAndStandardDeviations;
    Hashtable<String, DefaultIntervalCategoryDataset> minMaxs;
    Hashtable<String, ChartPanel> chartPanels;
    JTabbedPane tabbedPane;
    ChartPanel chartPanel;

    public StatsGraph() {
        tabbedPane = new JTabbedPane();
        meanAndStandardDeviations = new Hashtable<>();
        minMaxs = new Hashtable<>();
        chartPanels = new Hashtable<>();

        add(tabbedPane);
        setVisible(true);
        reset();
    }

    /**
     *
     * @param properties
     * @param designPoints
     * @param samples
     */
    public void setProperties(String[] properties, int designPoints, int samples) {
        this.designPoints = designPoints;
        this.samples = samples;
        tabbedPane.removeAll();
        for (String prop : properties) {
            if (viskit.VStatics.debug) {
                System.out.println("StatsGraph: createDataSets for " + prop);
            }
            createDataSets(prop);
            tabbedPane.add(prop, chartPanels.get(prop));
        }
    }

    public final void reset() {
        setProperties(new String[] {"Viskit DOE Results"}, 0, 0);
    }

    /** add SampleStatistic to datasets at designPoint d and sample s
     * @param sample
     * @param d
     * @param s
     */
    public void addSampleStatistic(viskit.xsd.bindings.assembly.SampleStatistics sample, int d, int s) {
        String name = sample.getName();
        DefaultStatisticalCategoryDataset statsData = meanAndStandardDeviations.get(name);
        DefaultIntervalCategoryDataset minMax = minMaxs.get(name);
        if (viskit.VStatics.debug) {
            System.out.println("SampleStatisticType name: " + sample.getName());
        }
        statsData.add(Double.parseDouble(sample.getMean()),Double.parseDouble(sample.getStandardDeviation()), "Design Point " + d, "Sample " + s);
        minMax.setStartValue(d, "Sample " + s, Double.valueOf(sample.getMinObs()));
        minMax.setEndValue(d, "Sample " + s, Double.valueOf(sample.getMaxObs()));
        chartPanels.get(name).repaint();
    }

    public void createDataSets(String name) {

        meanAndStandardDeviations.put(name, createStatisticalDataset());
        minMaxs.put(name, createMinMaxDataset());

        JFreeChart chart = new JFreeChart("Statistics for " + name,
                new Font("Helvetica", Font.BOLD, 14),
                new CategoryPlot(),
                false);

        chartPanel = new ChartPanel(chart);
        chartPanels.put(name, chartPanel);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);

        NumberAxis numAx = new NumberAxis("Value of " + name);
        CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator();
        CategoryItemRenderer renderer;
        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        plot.setDomainAxis(new CategoryAxis("Latin Hypersquare Samples of Parameters from Design of Experiment Panel"));
        renderer = new StatisticalBarRenderer();
        ((BarRenderer) renderer).setItemMargin(.27d);
        renderer.setBaseItemLabelsVisible(true);
        renderer.setBaseItemLabelGenerator(generator);
        numAx = new NumberAxis("Value of "+name);
        numAx.setAutoRange(true);
        numAx.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(numAx);
        plot.setDataset(meanAndStandardDeviations.get(name));
        plot.setRenderer(renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);

        // now minmax
        renderer = new IntervalBarRenderer();
        ((BarRenderer) renderer).setDrawBarOutline(true);
        ((BarRenderer) renderer).setMaximumBarWidth(1.0d);
        renderer.setBaseShape(new java.awt.geom.RoundRectangle2D.Double());
        plot.setDataset(1, minMaxs.get(name));
        plot.setRenderer(1, renderer);
        //plot.setRowRenderingOrder(SortOrder.ASCENDING);
        plot.setRowRenderingOrder(SortOrder.DESCENDING);
    }

    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private DefaultStatisticalCategoryDataset createStatisticalDataset() {

        DefaultStatisticalCategoryDataset result = new DefaultStatisticalCategoryDataset();

        for (int dp = 0; dp < designPoints; dp++) {
            for (int s = 0; s < samples; s++) {
                result.add(0.0, 0.0, "Design Point " + dp, "Sample " + s);
            }
        }

        return result;
    }

    private DefaultIntervalCategoryDataset createMinMaxDataset() {

        String[] seriesKeys = new String[designPoints];
        String[] categoryKeys = new String[samples];
        Double[][] starts = new Double[designPoints][];
        Double[][] ends = new Double[designPoints][];
        for (int dp = 0; dp < designPoints; dp++) {
            seriesKeys[dp] = "Design Point " + dp;
            starts[dp] = new Double[samples];
            ends[dp] = new Double[samples];
            for (int s = 0; s < samples; s++) {
                starts[dp][s] = 0.0d;
                ends[dp][s] = 0.001d;
                categoryKeys[s] = "Sample " + s; // could de-loop
            }
        }
        DefaultIntervalCategoryDataset result = new DefaultIntervalCategoryDataset(seriesKeys, categoryKeys, starts, ends);
        return result;
    }
}
