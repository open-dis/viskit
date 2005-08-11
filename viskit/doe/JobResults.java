/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Aug 9, 2005
 * @since 10:42:36 AM
 */

package viskit.doe;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
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

public class JobResults extends JFrame
{
  Vector data;
  MyDataSet dset;
  XYPlot plot;
  NumberAxis repAxis;

  public JobResults(JFrame mother)
  {
    this(mother,new Vector()); // empty data
  }
  public JobResults(JFrame mother, Vector data)
  {
    this.data = data;
    dset = (MyDataSet)createDataset(data);
    JFreeChart chart = createChart(dset);
    ChartPanel cpan = new ChartPanel(chart);
    cpan.setPreferredSize(new Dimension(500, 270));
    cpan.setDomainZoomable(true);
    cpan.setRangeZoomable(true);
    cpan.setBorder(new EmptyBorder(10, 10, 10, 10));
    setContentPane(cpan);

    pack();
    Dimension moms = mother.getSize();
    Point momp = mother.getLocation();
    Dimension mine = getSize();

    setLocation(momp.x+moms.width,momp.y);    // to left of mother frame

/*
    setLocation(momp.x + (moms.width - mine.width) / 2,  // centered on mother frame
        momp.y + (moms.height - mine.height) / 2);
*/
    setVisible(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  public void addPoint(JobLauncher.Gresults res)
  {
    dset.addToTail(res);
  }

  JFreeChart chart;
  private JFreeChart createChart(XYDataset dset)
  {
    chart = ChartFactory.createScatterPlot("Bremerton GridKit Output", "Replication",
        "Platform Failure", dset, PlotOrientation.VERTICAL, false, false, false);
    //LegendTitle legend = (LegendTitle)chart.getSubtitle(0);
    //legend.setPosition(RectangleEdge.BOTTOM);

    plot = chart.getXYPlot();
    repAxis = new NumberAxis("Replication");
    repAxis.setTickUnit(new NumberTickUnit(5.0d));
    repAxis.setAutoRangeStickyZero(false);
    repAxis.setUpperMargin(0.025d);
    repAxis.setLowerMargin(0.025d);
    plot.setDomainAxis(repAxis);
    //todo can set multiple domainaxes
    NumberAxis yAxis = new NumberAxis("Terrorist success");
    yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    yAxis.setAutoRangeStickyZero(false);
    yAxis.setUpperMargin(0.025d);
    yAxis.setLowerMargin(0.025d);
    plot.setRangeAxis(yAxis);
    plot.getRenderer().setShape(new Ellipse2D.Double(-2.0,-2.0,5.0,5.0));
    plot.getRenderer().setPaint(new Color(255,0,0,128));
    plot.getRenderer().setToolTipGenerator(
      new XYToolTipGenerator()
        {
          StringBuffer sb = new StringBuffer();
          public String generateToolTip(XYDataset dataset, int series, int item)
          {
            MyDataSet mds = (MyDataSet)dataset;
            JobLauncher.Gresults res = (JobLauncher.Gresults)mds.getCanonicalDataItem(item);
            sb.setLength(0);
            sb.append("<html><u>");
            sb.append(res.listener);
            sb.append("</u><br>run: ");
            sb.append(res.run);
            sb.append("<br>design point: ");
            sb.append(res.dp);
            sb.append("<br>attack result: ");
            sb.append(dataset.getYValue(series,item)==0.0?"<b>repulsed":"<b>compromised");

            return sb.toString();
          }
        });


    return chart;
  }

  private XYDataset createDataset(Vector v)
  {
    return new MyDataSet(this,v);
  }
}

class MyDataSet extends AbstractXYDataset implements XYDataset
{
  private static int sequence = 0;
  private Vector v;
  private int myseq;
  private JobResults mom;
  MyDataSet(JobResults mom,Vector v)
  {
    this.v = v;
    this.mom = mom;
    myseq = MyDataSet.sequence++;
  }
  public void addToTail(JobLauncher.Gresults res)
  {
    v.add(res);
    if(v.size()>200) {
      mom.plot.getRenderer().setShape(new Ellipse2D.Double(-1.0,-1.0,3.0,3.0));
      mom.repAxis.setTickUnit(new NumberTickUnit(20.0d));      
    }
    else if(v.size()>100) {
      mom.plot.getRenderer().setShape(new Ellipse2D.Double(-1.0,-1.0,3.0,3.0));
      mom.repAxis.setTickUnit(new NumberTickUnit(10.0d));
    }
    this.fireDatasetChanged();
  }
  public Object getCanonicalDataItem(int idx)
  {
    return v.get(idx);
  }
  public int getSeriesCount()
  {
    return 1;
  }

  public Comparable getSeriesKey(int series)
  {
    return "" + myseq;
  }

  public int getItemCount(int series)
  {
    return v.size();
  }

  public Number getX(int series, int item)
  {

    return new Integer(item);
  }

  public Number getY(int series, int item)
  {

      boolean[] results = ((JobLauncher.Gresults)v.get(item)).results;

      for(int i=0;i<results.length;i++) {
        if (results[i])
          return new Integer(1);
      }
      return new Double(0);
  }
}
