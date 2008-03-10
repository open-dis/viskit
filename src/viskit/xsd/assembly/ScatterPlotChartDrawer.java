/*
 * Program:      Viskit Discrete Event Simulation (DES) Tool
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      09 MAR 2008
 *
 * Filename:     ScatterPlotChartDrawer.java
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

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultMultiValueCategoryDataset;

/**
 * Create Scatter Plots from statistical data collected from experimental 
 * replications
 *
 * @version $Id:$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     10 MAR 2008
 *     Time:     0120Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.xsd.assembly.ScatterPlotChartDrawer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 * 
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class ScatterPlotChartDrawer {

    static Logger log = Logger.getLogger(ScatterPlotChartDrawer.class);
    
    /** Creates a new instance of ScatterPlotChartDrawer */
    public ScatterPlotChartDrawer() {}

    /**
     * Creates a scatterplot image in PNG format based on the parameters provided
     *
     * @param title 
     * @param label 
     * @param data an array of doubles that are to be plotted
     * @param fileName the name of the file to save the image out to
     * @return the path url of the created object
     */
    public String createScatterPlot(String title, String label, double[] data, String fileName) {
        String baseUrl = "charts/" + fileName + "ScatterPlot.png";
        String chartUrl = "./" + baseUrl;
        String fileLocation = "./AnalystReports/" + baseUrl;
        CategoryDataset dataset = createMultiValueCategoryDataset(label, data);
        try {
            saveChart(createChart(dataset, title, "Value"), fileLocation);
        } catch (IOException ioe) {
            log.error("Unable to create chart image: " + ioe.getMessage());
            ioe.printStackTrace();
        }
        return chartUrl;
    }

    private List<Double> listOfValues(double[] values) {
        List<Double> result = new ArrayList<Double>();
        for (double value : values) {
            result.add(value);
        }
        return result;
    }
    
    /**
     * Creates a data set that is used for making a scatter plot of the data
     * @param label
     * @param data
     * @return
     */
    private CategoryDataset createMultiValueCategoryDataset(String label, double[] data) {

        DefaultMultiValueCategoryDataset dataset = new DefaultMultiValueCategoryDataset();
        int count = 0;
        for (double datum : data) {
            dataset.add(listOfValues(new double[] {datum}), label, ++count);
        }

        return dataset;
    }

    /**
     * Creates the scatter plot chart
     * @param dataset
     * @param title
     * @param yLabel
     * @return a scatter plot chart
     */
    private JFreeChart createChart(CategoryDataset dataset, String title, String yLabel) {
//        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
//        renderer.setSeriesOutlinePaint(0, Color.black);
//        renderer.setUseOutlinePaint(true);
//        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
//        domainAxis.setAutoRangeIncludesZero(false);
//        domainAxis.setTickMarkInsideLength(2.0f);
//        domainAxis.setTickMarkOutsideLength(0.0f);
//        
//        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//        rangeAxis.setTickMarkInsideLength(2.0f);
//        rangeAxis.setTickMarkOutsideLength(0.0f);
        
        CategoryPlot plot = new CategoryPlot(
                dataset, 
                new CategoryAxis("Replications"), 
                new NumberAxis(yLabel), 
                new ScatterRenderer());
        NumberAxis na = (NumberAxis) plot.getRangeAxis();
        na.setAutoRangeIncludesZero(false);
        
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));
        JFreeChart chart = new JFreeChart(plot);
        chart.setTitle(title);
        chart.setBackgroundPaint(Color.white);
        
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