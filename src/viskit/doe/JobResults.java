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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Vector;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Aug 9, 2005
 * @since 10:42:36 AM
 * @version $Id$
 */
public class JobResults extends JFrame {

    Vector<JobLauncher.Gresults> data;
    MyDataSet dset;
    XYPlot plot;
    NumberAxis repAxis;

    public JobResults(JFrame mother, String title) {
        this(mother, title, new Vector<JobLauncher.Gresults>()); // empty data
    }

    public JobResults(JFrame mother, String title, Vector<JobLauncher.Gresults> data) {
        super(title);
        this.data = data;
        dset = (MyDataSet) createDataset(data);
        JFreeChart jFreeChart = createChart(dset);
        ChartPanel cpan = new ChartPanel(jFreeChart);
        cpan.setPreferredSize(new Dimension(640, 270));
        cpan.setDomainZoomable(true);
        cpan.setRangeZoomable(true);
        cpan.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(cpan);

        pack();
        if (mother != null) {
            Dimension moms = mother.getSize();
            Point momp = mother.getLocation();

            setLocation(momp.x + moms.width, momp.y);    // to left of mother frame
        }
        /*
        Dimension mine = getSize();
        setLocation(momp.x + (moms.width - mine.width) / 2,  // centered on mother frame
        momp.y + (moms.height - mine.height) / 2);
         */
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void addPoint(JobLauncher.Gresults res) {
        dset.addToTail(res);
    }

    public void addPoint(JobLauncherTab.Gresults res) {
    //todo should these be null methods?
    }

    public void addPoint(JobLauncherTab2.Gresults res) {
    //todo should these be null methods?
    }
    JFreeChart chart;

    // TODO: JFreeChart not a generic observing version
    @SuppressWarnings("unchecked")
    private JFreeChart createChart(XYDataset dset) {
        chart = ChartFactory.createScatterPlot("GridKit Output", "Replication",
                "Platform Failure", dset, PlotOrientation.VERTICAL, false, false, false);

        Font f = chart.getTitle().getFont();
        chart.getTitle().setFont(f.deriveFont(Font.PLAIN)); // lose the bold

        TextTitle tt = new TextTitle("1 Terrorist successful, 0 Platform defended, -1 Results inconclusive");
        tt.setBackgroundPaint(new Color(255, 255, 255, 192)); // translucent white

        // JFreeChart v 1.2.0
        tt.setFrame(new BlockBorder(Color.black));

        // JFreeChart v 1.0.4
//        tt.setBorder(new BlockBorder(Color.black));
        f = tt.getFont();
        tt.setFont(f.deriveFont(Font.PLAIN));
        chart.getSubtitles().add(0, tt);

        plot = chart.getXYPlot();
        NumberAxis.createIntegerTickUnits();
        final NumberAxis replicationAxis = new NumberAxis("Replication");
        replicationAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        replicationAxis.setAutoRangeStickyZero(false);
        replicationAxis.setUpperMargin(0.025d);
        replicationAxis.setLowerMargin(0.025d);
        plot.setDomainAxis(replicationAxis);

        // TODO: can set multiple domain axes
        NumberAxis yAxis = new NumberAxis("Terrorist success");
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setAutoRangeStickyZero(false);
        yAxis.setUpperMargin(0.025d);
        yAxis.setLowerMargin(0.025d);
        plot.setRangeAxis(yAxis);

        plot.getRenderer().setBaseShape(new Ellipse2D.Double(-2.0, -2.0, 5.0, 5.0));
        plot.getRenderer().setBasePaint(new Color(255, 0, 0, 128));
        plot.getRenderer().setBaseToolTipGenerator(
                new XYToolTipGenerator() {

                    StringBuffer sb = new StringBuffer();

                    @Override
                    public String generateToolTip(XYDataset dataset, int series, int item) {
                        MyDataSet mds = (MyDataSet) dataset;
                        JobLauncher.Gresults res = (JobLauncher.Gresults) mds.getCanonicalDataItem(item);
                        if (!res.resultsValid) {
                            return "Inconclusive results";
                        }

                        sb.setLength(0);
                        sb.append("<html><u>");
                        sb.append(res.listener);
                        sb.append("</u><br>run: ");
                        sb.append(res.run);
                        sb.append("<br>design point: ");
                        sb.append(res.dp);
                        sb.append("<br>attack result: ");
                        sb.append(dataset.getYValue(series, item) == 0.0 ? "<b>repulsed" : "<b>compromised");

                        return sb.toString();
                    }
                });
        return chart;
    }

    private XYDataset createDataset(Vector<JobLauncher.Gresults> v) {
        return new MyDataSet(this, v);
    }
}

class MyDataSet extends AbstractXYDataset implements XYDataset {

    private static int sequence = 0;
    private Vector<JobLauncher.Gresults> v;
    private int myseq;
    private JobResults mom;

    MyDataSet(JobResults mom, Vector<JobLauncher.Gresults> v) {
        this.v = v;
        this.mom = mom;
        myseq = MyDataSet.sequence++;
    }

    public void addToTail(JobLauncher.Gresults res) {
        v.add(res);
        if (v.size() > 200) {
            mom.plot.getRenderer().setBaseShape(new Ellipse2D.Double(-1.0, -1.0, 3.0, 3.0));
//      mom.repAxis.setTickUnit(new NumberTickUnit(20.0d));
        } else if (v.size() > 100) {
            mom.plot.getRenderer().setBaseShape(new Ellipse2D.Double(-1.0, -1.0, 3.0, 3.0));
//      mom.repAxis.setTickUnit(new NumberTickUnit(10.0d));
        }
        this.fireDatasetChanged();
    }

    public Object getCanonicalDataItem(int idx) {
        return v.get(idx);
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable getSeriesKey(int series) {
        return "" + myseq;
    }

    @Override
    public int getItemCount(int series) {
        return v.size();
    }

    @Override
    public Number getX(int series, int item) {
        return item;
    }

    @Override
    public Number getY(int series, int item) {
        JobLauncher.Gresults results = v.get(item);
        if (!results.resultsValid) {
            return -1d;
        }
        return (results.resultsMean < 1.0d) ? new Double(1d) : new Double(0d);
    }
}