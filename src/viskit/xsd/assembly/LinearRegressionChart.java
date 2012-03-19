/*
 * Program:      Viskit Discrete Event Simulation (DES) Tool
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      09 MAR 2008
 *
 * Filename:     LinearRegressionChart.java
 *
 * Compiler:     JDK1.6
 * O/S:          Windows XP Home Ed. (SP2)
 *
 * Description:  Create Scatter Plots from statistical data collected from 
 *               experimental replications
 *
 * References:   
 *
 * URL:          
 *
 * Requirements: 1) 
 *
 * Assumptions:  1) 
 *
 * TODO:         
 *
 * Copyright (c) 1995-2008 held by the author(s).  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer
 *       in the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the names of the Naval Postgraduate School (NPS)
 *       Modeling Virtual Environments and Simulation (MOVES) Institute
 *       (http://www.nps.edu and http://www.movesinstitute.org)
 *       nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package viskit.xsd.assembly;

import edu.nps.util.LogUtils;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import viskit.VGlobals;
import viskit.ViskitProject;

/**
 * Create Scatter Plots from statistical data collected from experimental 
 * replications
 *
 * @version $Id$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     10 MAR 2008
 *     Time:     0120Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.xsd.assembly.LinearRegressionChart">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 * 
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class LinearRegressionChart {

    /** Creates a new instance of LinearRegressionChart */
    public LinearRegressionChart() {}

    /**
     * Creates a scatterplot image in PNG format based on the parameters provided
     *
     * @param title the title of the ScatterPlot
     * @param label the name of the ScatterPlot
     * @param data an array of doubles that are to be plotted
     * @return the path url of the created object
     */
    public String createChart(String title, String label, double[] data) {
        ViskitProject vkp = VGlobals.instance().getCurrentViskitProject();
        File fileLocation = new File(vkp.getAnalystReportChartsDir(), label + "LinearRegression.png");
        XYDataset dataset = createDataset(label, data);
        saveChart(createChart(dataset, title, "Value"), fileLocation);
        return fileLocation.getAbsolutePath().replaceAll("\\\\", "/");
    }

    /**
     * Creates a data set that is used for making a scatter plot of the data
     * @param label the name of the ScatterPlot
     * @param data an array of doubles that are to be plotted
     * @return
     */
    private XYDataset createDataset(String label, double[] data) {
        XYSeries series = new XYSeries(label);        
        int count = 0;
        for (double datum : data) {
            series.add(++count, datum);
        }        
        return new XYSeriesCollection(series);
    }

    /**
     * Creates the scatter plot chart
     * @param dataset
     * @param title
     * @param yLabel
     * @return a scatter plot chart
     */
    private JFreeChart createChart(XYDataset dataset, String title, String yLabel) {
        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                "Replications", 
                yLabel, 
                dataset, 
                PlotOrientation.VERTICAL, 
                true, 
                false, 
                false);
 
        XYPlot plot = (XYPlot) chart.getPlot();
       
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);
                
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        
        // calculate the regression and create subplot 2...
        double[] coefficients = Regression.getOLSRegression(dataset, 0);
        Function2D curve = new LineFunction2D(coefficients[0], coefficients[1]);
        XYDataset regressionData = DatasetUtilities.sampleFunction2D(
                curve,
                ((XYSeriesCollection) dataset).getDomainLowerBound(true), 
                ((XYSeriesCollection) dataset).getDomainUpperBound(true), 
                dataset.getItemCount(0), 
                "Fitted Regression Line");

        plot.setDataset(1, regressionData);
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesPaint(0, Color.blue);
        plot.setRenderer(1, renderer2);

        chart.setTitle(title);
        
        return chart;
    }

    /**
     * Saves a chart to PNG format
     * @param chart the created JFreeChart instance
     * @param outFile the path to save the generated PNG to
     */
    private void saveChart(JFreeChart chart, File outFile) {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            ChartUtilities.saveChartAsPNG(outFile, chart, 969, 641);
        } catch (IOException ioe) {
            LogUtils.getLogger(LinearRegressionChart.class).error(ioe);
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException ioe) {}
        }
    }
}