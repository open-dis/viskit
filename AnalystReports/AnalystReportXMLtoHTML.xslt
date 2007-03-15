<?xml version="1.0" encoding="UTF-8"?>
<!--
  <head>
   <meta name="filename"    content="AnalystReport.xslt" />
   <meta name="author"      content="Patrick Sullivan" />
   <meta name="created"     content="21 July 2006" />
   <meta name="description" content="XSLT stylesheet, converts AnalystReportXML output into xhtml format>
  </head>
  
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:saxon="http://icl.com/saxon" saxon:trace="yes">
  <xsl:strip-space elements="*"/>
  <xsl:output encoding="UTF-8" media-type="text/html" indent="yes" cdata-section-elements="Script"
              omit-xml-declaration="no" method="xml"/>

  <xsl:template match="/">
    <!-- TODO:  fix
<xsl:text>
<![CDATA[
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
]]>
</xsl:text>
-->

    <html>
      <head>
        <meta http-equiv="Content-Language" content="en-us"/>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
        <title>Viskit Simulation Analysis Report</title>
      </head>
      <body>
        <!--Title information-->
        <xsl:apply-templates select="/AnalystReport"/>
        <hr/>
        <!--Executive Summary : NOTE: omitted if analyst did not include comments  -->
        <xsl:apply-templates select="//ExecutiveSummary"/>
        <p/>
        <p/>
        <hr/>
        <!--Simulation Location-->
        <xsl:apply-templates select="//SimulationLocation"/>
        <p/>
        <p/>
        <hr/>
        <!--Simulation Configuration-->
        <xsl:apply-templates select="//SimulationConfiguration" mode="ConfigHeader"/>
        <p/>
        <p/>
        <hr/>
        <!--Entity Parameters -->
        <xsl:apply-templates select="//EntityParameters" mode="ParamHeader"/>
        <p/>
        <xsl:apply-templates select="//EntityParameters/ParameterTables/EntityParameterTable"/>
        <p/>
        <p/>
        <hr/>
        <!--Behavior Definitions -->
        <xsl:apply-templates select="//BehaviorDefinitions" mode="BehaviorHeader"/>
        <p/>
        <xsl:apply-templates select="//BehaviorDefinitions/BehaviorList"/>
        <p/>
        <p/>
        <p/>
        <p/>
        <hr/>
        <!--Statistical Reports-->
        <xsl:apply-templates select="//StatisticalResults" mode="StatsHeader"/>
        <xsl:apply-templates select="//StatisticalResults/ReplicationReports/SimEntity" mode="RepStats"/>
        <xsl:apply-templates select="//StatisticalResults/SummaryReport" mode="SumStats"/>
        <p/>
        <p/>
        <hr/>
        <!--Conclusions Recommendations -->
        <xsl:apply-templates select="//ConclusionsRecommendations"/>
      </body>
    </html>
  </xsl:template>


  <!--Title information template-->
  <xsl:template match="AnalystReport">
    <p align="center">
      <font size="2">
        <b>***</b>
        THIS REPORT IS:
        <b>
          <b>
            <xsl:value-of select="@classification"/>
            ***
          </b>
        </b>
      </font>
    </p>
    <p align="center">
      <font size="6">
        <xsl:value-of select="@name"/>
      </font>
    </p>
    <p align="center">Analyst:
      <b>
        <xsl:value-of select="@author"/>
      </b>
    </p>
    <p align="center">Analysis date:
      <b>
        <xsl:value-of select="@date"/>
      </b>
    </p>
  </xsl:template>

  <!--Executive Summary template -->
  <xsl:template match="ESComments">
    <p align="left">
      <b>Executive Summary</b>
    </p>
    <p align="left">
      
        <i>Analyst Executive Summary.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>

  <!--Simulation Location templates -->
  <xsl:template match="SLComments">
    <p align="left">
      <b>Simulation Location</b>
    </p>
    <p align="left">
      
        <i>Description of Location Features.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="SLConclusions">
    <p align="left">
      
        <i>Post-Experiment Analysis of Significant Location Features.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="LocationImage">
    <p align="center">
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:value-of select="@dir"/>
        </xsl:attribute>
        <xsl:attribute name="style">
          <xsl:text>border:0</xsl:text>
        </xsl:attribute>
          <xsl:element name="img">
            <xsl:attribute name="border">
              <xsl:text>1</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="src"><xsl:value-of select="@dir"/></xsl:attribute>
            <xsl:attribute name="description">
              <!-- TODO:  more info here -->
              <xsl:text>location</xsl:text>
            </xsl:attribute>
          </xsl:element>
      </xsl:element>
    </p>
  </xsl:template>

  <!--SimulationConfiguration templates-->
  <xsl:template match="SCComments" mode="ConfigHeader">
    <p align="left">
      <b>Assembly Configuration for Viskit Simulation</b>
    </p>
    <p align="left">
      
        <i>Assembly Design Considerations.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="SCConclusions" mode="ConfigHeader">
    <p align="left">
      
        <i>Post-Experiment Analysis of Simulation Assembly Design.</i>
      
      <font color="#000099">
        <xsl:value-of select="@text"/>
      </font>
    </p>
    <p align="center">
      <b>Summary of Simulation Entities</b>
    </p>
    <div align="center">
      <table border="1">
        <tr>
          <td bgcolor="#FFFFCC">Entity Name</td>
          <td bgcolor="#FFFFCC">Behavior Definition</td>
        </tr>
        <xsl:apply-templates select="//SimulationConfiguration/EntityTable/SimEntity" mode="EntitiesTable"/>
      </table>
    </div>
  </xsl:template>

  <xsl:template match="AssemblyImage" mode="ConfigHeader">
    <p align="center">
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:value-of select="@dir"/>
        </xsl:attribute>
        <xsl:attribute name="style">
          <xsl:text>border:0</xsl:text>
        </xsl:attribute>
        <xsl:element name="img">
          <xsl:attribute name="border">
            <xsl:text>1</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="src"><xsl:value-of select="@dir"/></xsl:attribute>
          <xsl:attribute name="title">
            <xsl:text>Assembly graph</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="alt">
            <xsl:text>Assembly graph</xsl:text>
          </xsl:attribute>
        </xsl:element>
      </xsl:element>
    </p>
  </xsl:template>
  <xsl:template match="SimEntity" mode="EntitiesTable">
    <tr>
      <td>
        <xsl:value-of select="@name"/>
      </td>
      <td>
        <xsl:value-of select="@behaviorDefinition"/>
      </td>
    </tr>
  </xsl:template>


  <!--EntityParameter templates-->
  <xsl:template match="EPComments" mode="ParamHeader">
    <p align="left">
      <b>Entity Parameters</b>
    </p>
    <p align="left">
      
        <i>Entity Parameters Overview.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="EPConclusions" mode="ParamHeader">
    <p align="left">
      
        <i>Post-Experiment Analysis of Entities.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>

  <!--Entity Parameter Tables-->
  <xsl:template match="EntityParameterTable">
    <p/>
    <p/>
    <p/>
    <xsl:text>Simulation Parameters for </xsl:text>
    <b>
      <xsl:value-of select="@name"/>
    </b>
    <!--  TODO:  add uniquely identifying information for this header -->
    <table border="1" width="75%" cellpadding="0" cellspacing="1">

      <!--Classification Values -->
      <xsl:for-each select="Classification">
        <tr>
          <td width="132" bgcolor="#CCCCCC">Classification</td>
          <td width="132"></td>
          <td width="190"></td>
          <!--
              <td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
              <td width="190"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
          -->
        </tr>
        <xsl:for-each select="parameter">
          <tr>
            <td width="132"></td>
            <!--	<td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>-->
            <td width="190">
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@value"/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>

      <!--Identification Values -->
      <xsl:for-each select="Identification">
        <tr>
          <td width="132" bgcolor="#CCCCCC">Identification</td>
          <td width="132"></td>
          <td width="190"></td>
          <!--
              <td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
              <td width="190"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
          -->
        </tr>
        <xsl:for-each select="parameter">
          <tr>
            <td width="132"></td>
            <!--<td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>-->
            <td width="190">
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@value"/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>

      <!--Physical Constraints -->
      <xsl:for-each select="PhysicalConstraints">
        <tr>
          <td width="132" bgcolor="#CCCCCC">Physical Constraints</td>
          <td width="132"></td>
          <td width="190"></td>
          <!--
              <td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
              <td width="190"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
          -->
        </tr>
        <xsl:for-each select="parameter">
          <tr>
            <td width="132"></td>
            <!--	<td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>-->
            <td width="190">
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@value"/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>

      <!--Dynamic Response Constraints -->
      <xsl:for-each select="DynamicResponseConstraints">
        <tr>
          <td width="132" bgcolor="#CCCCCC">Dynamic Response Constraints</td>
          <td width="132"></td>
          <td width="190"></td>
          <!--
              <td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
              <td width="190"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
          -->
        </tr>
        <xsl:for-each select="parameter">
          <tr>
            <td width="132"></td>
            <!--						<td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>-->
            <td width="190">
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@value"/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>

      <!--Tactical Constraints -->
      <xsl:for-each select="TacticalConstraints">
        <tr>
          <td width="132" bgcolor="#CCCCCC">Tactical Constraints</td>
          <td width="132"></td>
          <td width="190"></td>
          <!--
              <td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
              <td width="190"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>
          -->
        </tr>
        <xsl:for-each select="parameter">
          <tr>
            <td width="132"></td>

            <!--					<td width="132"><xsl:text disable-output-escaping="yes">&nbsp;</xsl:text></td>-->
            <td width="190">
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@value"/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!--Behavior Definition templates-->
  <xsl:template match="BCComments" mode="BehaviorHeader">
    <p align="left">
      <b>Behavior Definitions</b>
    </p>
    <p align="left">
        <i>Description of Behavior Design.</i>
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="BCConclusions" mode="BehaviorHeader">
    <p align="left">
      <i>Post-Experiment Analysis of Entity Behaviors.</i>
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>

  <!--Event Graph image and details -->
  <xsl:template match="Behavior">
    <p/>
    <p/>
    <hr/>
    <p/>
    <p align="left">
      <b>Behavior: </b>
      <xsl:value-of select="@name"/>
    </p>

    <!--Add the description -->
    <xsl:for-each select="description">
      <p align="left">
        <b>Description: </b>
        <xsl:value-of select="@text"/>
      </p>
    </xsl:for-each>

    <!--Add the image of the event graph -->
    <xsl:for-each select="EventGraphImage">
      <p align="center">
          <xsl:element name="a">
            <xsl:attribute name="href">
              <xsl:value-of select="@dir"/>
            </xsl:attribute>
            <xsl:attribute name="style">
              <xsl:text>border:0</xsl:text>
            </xsl:attribute>
            <xsl:element name="img">
              <xsl:attribute name="border">
                <xsl:text>1</xsl:text>
              </xsl:attribute>
              <xsl:attribute name="src"><xsl:value-of select="@dir"/></xsl:attribute>
                <xsl:attribute name="description">
                  <!-- TODO:  more info here -->
                  <xsl:text>location</xsl:text>
                </xsl:attribute>
            </xsl:element>
        </xsl:element>
      </p>
    </xsl:for-each>
    <p align="center">
      <table border="1">
        <tr>
          <td bgcolor="#FFFFCC">Parameter</td>
          <td bgcolor="#FFFFCC">Parameter Type</td>
          <td bgcolor="#FFFFCC">Description</td>
        </tr>
        <!--Add parameter and state variable table for each event graph-->
        <xsl:for-each select="parameter">
          <tr>
            <td>
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@type"/>
            </td>
            <td>
              <xsl:value-of select="@description"/>
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </p>
    <p/>
    <p/>
    <p align="center">
      <table border="1">
        <tr>
          <td bgcolor="#FFFFCC">State Variable</td>
          <td bgcolor="#FFFFCC">Variable Type</td>
          <td bgcolor="#FFFFCC">Description</td>
        </tr>
        <xsl:for-each select="stateVariable">
          <tr>
            <td>
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="@type"/>
            </td>
            <td>
              <xsl:value-of select="@description"/>
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </p>
  </xsl:template>

  <!--Statistical Results -->
  <xsl:template match="SRComments" mode="StatsHeader">
    <p align="left">
      <b>Statistical Results</b>
    </p>
    <p align="left">
      
        <i>Description of Expected Results.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="SRConclusions" mode="StatsHeader">
    <p align="left">
      
        <i>Analysis of Experimental Results.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>

  <xsl:template match="SimEntity" mode="RepStats">
    <p/>
    <p align="left">
      
        <b>Replication Report</b>
      
    </p>
    <p align="left">Entity:
      <b>
        <xsl:value-of select="@name"/>
      </b>
      <p align="left">Property:
        <b>
          <xsl:value-of select="@property"/>
        </b>
      </p>
      <xsl:for-each select="chartURL">
        <p align="center">
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:value-of select="@dir"/>
        </xsl:attribute>
        <xsl:attribute name="style">
          <xsl:text>border:0</xsl:text>
        </xsl:attribute>
          <xsl:element name="img">
            <xsl:attribute name="border">
              <xsl:text>1</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="src"><xsl:value-of select="@dir"/></xsl:attribute>
            <xsl:attribute name="description">
                <!-- TODO:  better description -->
              <xsl:text>statistical output</xsl:text>
            </xsl:attribute>
          </xsl:element>
         </xsl:element>
        </p>
      </xsl:for-each>
      <p align="center">
        <table border="1" width="60%">
          <tr>
            <td bgcolor="#FFFFCC">
              <b>Run#</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>Count</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>Min</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>Max</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>Mean</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>StdDev</b>
            </td>
            <td bgcolor="#FFFFCC">
              <b>Variance</b>
            </td>
          </tr>
          <xsl:for-each select="Replication">
            <tr>
              <td>
                <xsl:value-of select="@number"/>
              </td>
              <td>
                <xsl:value-of select="@count"/>
              </td>
              <td>
                <xsl:value-of select="@minObs"/>
              </td>
              <td>
                <xsl:value-of select="@maxObs"/>
              </td>
              <td>
                <xsl:value-of select="@mean"/>
              </td>
              <td>
                <xsl:value-of select="@stdDeviation"/>
              </td>
              <td>
                <xsl:value-of select="@variance"/>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </p>
    </p>

  </xsl:template>

  <xsl:template match="SummaryReport" mode="SumStats">
    <p/>
    <p align="left">
      
        <b>Summary Report</b>
      
    </p>
    <table border="1" width="80%">
      <tr>
        <td bgcolor="#FFFFCC">
          <b>Entity</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Property</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Count</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Min</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Max</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Mean</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>StdDev</b>
        </td>
        <td bgcolor="#FFFFCC">
          <b>Variance</b>
        </td>
      </tr>
      <xsl:for-each select="SummaryRecord">
        <tr>
          <td>
            <xsl:value-of select="@entity"/>
          </td>
          <td>
            <xsl:value-of select="@property"/>
          </td>
          <td>
            <xsl:value-of select="@count"/>
          </td>
          <td>
            <xsl:value-of select="@minObs"/>
          </td>
          <td>
            <xsl:value-of select="@maxObs"/>
          </td>
          <td>
            <xsl:value-of select="@mean"/>
          </td>
          <td>
            <xsl:value-of select="@stdDeviation"/>
          </td>
          <td>
            <xsl:value-of select="@variance"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!--Conclusions Recommendations -->
  <xsl:template match="CRComments">
    <p align="left">
      <b>Conclusions and Recommendations</b>
    </p>
    <p align="left">
      
        <i>Conclusions.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>
  <xsl:template match="CRConclusions">
    <p align="left">
      
        <i>Recommendations for Future Work.</i>
      
      <font color="#00006C">
        <xsl:value-of select="@text"/>
      </font>
    </p>
  </xsl:template>


</xsl:stylesheet>