<?xml version="1.0" encoding="UTF-8"?>
<!--
<head>
    <meta name="filename"    content="AnalystReportXMLtoHTML.xslt"/>
    <meta name="author"      content="Patrick Sullivan"/>
    <meta name="created"     content="21 July 2006"/>
    <meta name="description" content="XSLT stylesheet, converts AnalystReportXML output into xhtml format"/>
    <meta name="version"     content="$Id$"/>
</head>
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xalan="http://xml.apache.org/xslt"
                xmlns:java="http://xml.apache.org/xslt/java">
    <xsl:strip-space elements="*"/>
    <xsl:output method="xml"
                encoding="UTF-8"
                omit-xml-declaration="no"
                doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
                cdata-section-elements="Script"
                indent="yes"
                media-type="text/html"
                xalan:indent-amount="2"/>

    <!-- Attempt to establish an incremental counter variable -->
    <!-- From: http://osdir.com/ml/text.xml.xalan.java.user/2006-05/msg00014.html -->
    <xsl:variable name="javaCounter" select="java:java.util.ArrayList.new()"/>
    <xsl:template match="/">
        <html>
            <xsl:comment>
                <xsl:text>Generated using XSLT processor: </xsl:text>
                <xsl:value-of select="system-property('xsl:vendor')"/>
            </xsl:comment>
            <head>
                <title>Viskit Simulation Analysis Report</title>
                <meta http-equiv="Content-Language" content="en-us"/>
                <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
                <link rel="shortcut icon" href="Viskit.ico" type="image/x-icon"/>
                <style type="text/css">
                    <!-- Limit the width of the entire HTML page to be printer friendly -->
                    body {width: 1024px}
                </style>
            </head>
            <body>
                <!-- Link anchor for the top of the page -->
                <a name="top"/>
                <!-- Title information -->
                <xsl:apply-templates select="/AnalystReport"/>
                <!-- Bookmarks for various sections of the report -->
                <center>
                    <p>
                        <img src="ViskitSplash2.png"/><!-- TODO link this image to website -->
                    </p>
                    <p>
                        <font size="-1">
                            <a href="#ExecutiveSummary">Executive<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Summary</a> |
                            <a href="#ScenarioLocation">Scenario Location</a> |
                            <a href="#SimulationConfiguration">Simulation<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Configuration</a> |
                            <a href="#EntityParameters">Entity<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Parameters</a> |
                            <a href="#BehaviorDescriptions">Behavior<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Descriptions</a> |
                            <a href="#StatisticalResults">Statistical<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Results</a> |
                            <a href="#ConclusionsRecommendations">Conclusions<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>and<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>Recommendations</a>
                        </font>
                    </p>
                </center>
                <hr/>
                <!-- Executive Summary: NOTE - omitted if analyst did not include comments -->
                <xsl:apply-templates select="//ExecutiveSummaryDescription"/>
                <p/>
                <p/>
                <hr/>
                <!-- Scenario Location -->
                <xsl:apply-templates select="//ScenarioLocation"/>
                <p/>
                <p/>
                <hr/>
                <!-- Simulation Configuration -->
                <xsl:apply-templates select="//SimulationConfiguration" mode="ConfigHeader"/>
                <p/>
                <p/>
                <hr/>
                <!-- Entity Parameters -->
                <xsl:apply-templates select="//EntityParameters" mode="ParamHeader"/>
                <xsl:apply-templates select="//EntityParametersDescription" mode="ParamHeader"/>
                <xsl:apply-templates select="//EntityParametersConclusions" mode="ParamHeader"/>
                <xsl:apply-templates select="//ParameterTables"/>
                <p/>
                <p/>
                <hr/>
                <!-- Behavior Descriptions -->
                <xsl:apply-templates select="//BehaviorDescriptions" mode="BehaviorHeader"/>
                <xsl:apply-templates select="//BehaviorDescriptions/BehaviorList"/>
                <p/>
                <p/>
                <hr/>
                <!-- Statistical Reports -->
                <xsl:apply-templates select="//StatisticalResults" mode="StatisticsHeader"/>
                <xsl:apply-templates select="//StatisticalResults/ReplicationReports/SimEntity" mode="ReplicationStatistics"/>
                <xsl:apply-templates select="//StatisticalResults/SummaryReport" mode="SummaryStatistics"/>
                <p/>
                <p/>
                <hr/>
                <!-- Conclusions Recommendations -->
                <xsl:apply-templates select="//ConclusionsRecommendations"/>
                <!-- add Viskit/Simkit credit footer -->
                <p/>
                <p/>
                <hr/>
                <p>
                    This report was autogenerated by the Viskit Event Graph and Assembly
                    modeling tool using Simkit discrete-event simulation (DES) libraries.
                    Online at <a href="https://diana.nps.edu/Viskit">https://diana.nps.edu/Viskit</a>
                    and <a href="https://diana.nps.edu/Simkit">https://diana.nps.edu/Simkit</a>.
                </p>
            </body>
        </html>
    </xsl:template>

    <!-- Title information template -->
    <xsl:template match="AnalystReport">
        <xsl:if test="(string-length(@accessRights) > 0)">
            <p align="center">
                <font size="2">
                    Access rights for this report:
                    <b>
                        <xsl:value-of select="@accessRights"/>
                    </b>
                </font>
            </p>
        </xsl:if>
        <p align="center">
            <font size="6">
                <xsl:value-of select="@name"/>
            </font>
        </p>
        <xsl:choose>
            <xsl:when test="contains(@author, ',')">
                <xsl:element name="p">
                    <xsl:attribute name="align">
                        <xsl:text>center</xsl:text>
                    </xsl:attribute>
                    <xsl:text>Analysts: </xsl:text>
                    <xsl:element name="b">
                       <xsl:value-of select="@author"/>
                    </xsl:element>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="p">
                    <xsl:attribute name="align">
                        <xsl:text>center</xsl:text>
                    </xsl:attribute>
                    <xsl:text>Analyst: </xsl:text>
                    <xsl:element name="b">
                       <xsl:value-of select="@author"/>
                    </xsl:element>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
        <p align="center">Analysis date:
            <b>
                <xsl:value-of select="@date"/><!-- TODO problem with stray character when displaying date -->
            </b>
        </p>
    </xsl:template>

    <!-- Executive Summary template -->
    <xsl:template match="ExecutiveSummaryDescription">
        <p align="left">
            <font size="4">
                <b><a name="ExecutiveSummary">Executive Summary</a></b>
            </font>
        </p>
        <p align="left">
            <i>Assessment Overview</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>

    <!-- Scenario Location templates -->
    <xsl:template match="ScenarioLocation">
        <p align="left">
            <font size="4">
                <b><a name="ScenarioLocation">Scenario Location for the Simulation</a></b>
            </font>
        </p>
        <xsl:apply-templates select="*"/>
    </xsl:template>
    
    <xsl:template match="ScenarioLocationDescription">
        <p align="left">
            <i>Description of Scenario Location Features</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    <xsl:template match="ScenarioLocationProductionNotes">
        <p align="left">
            <i>Production Notes</i>
        </p>
        <p align="left">
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
        <p>
            All units are meters and degrees unless otherwise noted.
        </p>
    </xsl:template>
    
    <xsl:template match="ScenarioLocationConclusions">
        <p align="left">
            <i>Post-Experiment Analysis of Significant Scenario Location Features</i>
        </p>
        <p align="left">
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    <xsl:template match="ScenarioLocationImage">
        <div align="center">
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
                    <xsl:attribute name="src">
                        <xsl:value-of select="@dir"/>
                    </xsl:attribute>
                    <xsl:attribute name="description">
                        <!-- TODO:  more info here -->
                        <xsl:text>location</xsl:text>
                    </xsl:attribute>
                </xsl:element>
            </xsl:element>

            <!-- add an index to the array -->
            <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
            <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: 2D Overview of Simulation Study Area</p>
        </div>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>
    <xsl:template match="ChartImage">

        <!-- DNC views have to be manually screen captured.  Test for existence first -->
        <xsl:if test="@dir != ''">
            <p/>
            <p/>
            <div align="center">
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
                        <xsl:attribute name="src">
                            <xsl:value-of select="@dir"/>
                        </xsl:attribute>

                        <!-- Clamp the width of potentially big chart images -->
                        <xsl:attribute name="width">
                            <xsl:text>800px</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="description">
                            <!-- TODO:  more info here -->
                            <xsl:text>location</xsl:text>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:element>

                <!-- add an index to the array -->
                <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
                <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: Digital Chart View of Study Area</p>
            </div>
            <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
        </xsl:if>
    </xsl:template>

    <!-- Simulation Configuration templates -->
    <xsl:template match="SimulationConfiguration" mode="ConfigHeader">
        <p align="left">
            <font size="4">
                <b>
                    <a name="SimulationConfiguration">Simulation Configuration: Viskit Assembly Preparation</a>
                    <xsl:if test="(string-length(@assemblyName)> 0)">
                        for
                        <xsl:value-of select="@assemblyName"/>
                    </xsl:if>
                </b>
                <br/>
            </font>
        </p>
        <p align="left">
            Simulation configuration is defined by the Viskit Assembly which collects,
            lists, initializes, and connects all  Event Graphs for participating entity models
            within a single scenario.  The runnable assembly is then ready for repeated
            simulation replications, either for visual validation of behavior
            or statistical analysis of Measures of Effectiveness (MoEs).
        </p>
        <xsl:apply-templates select="//AssemblyImage"/>
        <xsl:apply-templates select="SimulationConfigurationDescription"/>
        <xsl:apply-templates select="SimulationConfigurationProductionNotes"/>
        <xsl:apply-templates select="SimulationConfigurationConclusions"/>
        <!-- ... which launches EntityTable templates -->
        <xsl:apply-templates select="EntityTable"/>
    </xsl:template>
    
    <xsl:template match="SimulationConfigurationDescription" mode="ConfigHeader">
        <p align="left">
            <i>Simulation Configuration Considerations</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    <xsl:template match="SimulationConfigurationProductionNotes" mode="ConfigHeader">
        <p align="left">
            <i>Simulation Configuration Production Notes</i>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
        <p>All units are meters and degrees unless otherwise noted.</p>
    </xsl:template>
    
    <xsl:template match="SimulationConfigurationConclusions" mode="ConfigHeader">
        <p align="left">
            <i>Post-Experiment Analysis of Simulation Configuration</i>
        </p>
        <p align="left">
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
                    <th bgcolor="#FFFFCC">Simulation Entity</th>
                    <th bgcolor="#FFFFCC">Behavior Definitions</th>
                </tr>
                <xsl:apply-templates select="EntityTable/*" mode="EntitiesTable"/>
            </table>
        </div>
        <p/>
        <p/>
    </xsl:template>
    
    <xsl:template match="AssemblyImage">
        <div align="center">
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
                    <xsl:attribute name="src">
                        <xsl:value-of select="@dir"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:text>Assembly graph</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="alt">
                        <xsl:text>Assembly graph</xsl:text>
                    </xsl:attribute>
                </xsl:element>
            </xsl:element>

            <!-- add an index to the array -->
            <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
            <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: Simulation Assembly Combining all Simulation Entities for this Scenario Experiment</p>
            <!-- TODO add Assembly name to XML, add linked name to figure caption -->
        </div>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>
    
    <xsl:template match="SimEntity" mode="EntitiesTable">
        <tr>
            <td>
                <!-- Now link to each Simulation Entity in this Table -->
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:text>#</xsl:text>
                        <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:value-of select="@name"/>
                </xsl:element>
            </td>
            <td>
                <!-- then link to each Behavior Definition -->
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:text>#</xsl:text>
                        <xsl:value-of select="@fullyQualifiedName"/>
                    </xsl:attribute>
                    <xsl:value-of select="@fullyQualifiedName"/>
                </xsl:element>
            </td>
        </tr>
    </xsl:template>

    <!-- EntityParameter templates -->
    <xsl:template match="EntityParameters" mode="ParamHeader">
        <p align="left">
            <font size="4">
                <b>
                    <a name="EntityParameters">Entity Parameters for configuring Event Graphs</a>
                </b>
                <br/>
            </font>
        </p>
        <p align="left">
            Entity parameters configure the behaviors of connected event graphs.
        </p>
        
        <xsl:apply-templates select="EntityParametersDescription"/>
        <xsl:apply-templates select="ParameterTables"/>
        <xsl:apply-templates select="EntityParametersConclusions"/>
    </xsl:template>

    <xsl:template match="EntityParametersDescription" mode="ParamHeader">
        <p align="left">
            <font size="4">
                <b>
                    <a name="EntityParameters">Entity Initialization Parameters for this Simulation Assembly</a>
                </b>
            </font>
        </p>
        <p align="left">
            Initialization parameters are applied to individualize generic
            behavior models.  These parameters customize the event-graph models.
        </p>
    </xsl:template>
        
    <xsl:template match="EntityParametersConclusions" mode="ParamHeader">
        <p align="left">
            <i>Entity Parameters Conclusions:
               Post-Experiment Analysis of Entity Behaviors</i>
        </p>
        <p align="left">
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    
    <!-- xsl:template match="EntityParametersConclusions" mode="ParamHeader">
        <p align="left">
            <i>Post-Experiment Analysis of Entity Behaviors</i><br/>
            <font color="#00006C">
              <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template-->

    <!-- Entity Parameter Tables -->
    <xsl:template match="ParameterTables">
        <p/>
        <xsl:text>Initialization Parameters for Simulation Entity </xsl:text>
        <b>
            <a>
                <xsl:attribute name="name">
                    <xsl:value-of select="@name"/>
                </xsl:attribute>
                <xsl:value-of select="@name"/>
            </a>
        </b>
        <!--  TODO:  add uniquely identifying information for this header -->
        <table border="1" width="95%" cellpadding="0" cellspacing="1">

            <!-- Classification Values -->
            <xsl:for-each select="Classification">
                <tr>
                    <th colspan="2" bgcolor="#CCCCCC">Classification</th>
                    <th colspan="1" width="132" align="center" bgcolor="#CCCCCC">Candidate Factor</th>
                </tr>
                <xsl:for-each select="parameter">
                    <tr colspan="4">
                        <td width="190">
                            <xsl:value-of select="@name"/>
                        </td>
                        <td>
                            <!-- URL-ize if @value contains http:// -->
                            <xsl:choose>
                                <xsl:when test="contains(@value, 'http://')">
                                    <!-- http:// found -->
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of select="@value"/>
                                        </xsl:attribute>
                                        <xsl:value-of select="@value"/>
                                    </xsl:element>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@value"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td width="132"/>
                    </tr>
                </xsl:for-each>
            </xsl:for-each>

            <!-- Identification Values -->
            <xsl:for-each select="Identification">
                <tr>
                    <th colspan="2" bgcolor="#CCCCCC">Identification</th>
                    <th width="132" bgcolor="#CCCCCC"/>
                </tr>
                <xsl:for-each select="parameter">
                    <tr>
                        <td width="190">
                            <xsl:value-of select="@name"/>
                        </td>
                        <td>
                            <xsl:value-of select="@value"/>
                        </td>
                        <td width="132"/>
                    </tr>
                </xsl:for-each>
            </xsl:for-each>

            <!-- Physical Constraints -->
            <xsl:for-each select="PhysicalConstraints">
                <xsl:if test="parameter/@name">
                    <tr>
                        <th colspan="2" bgcolor="#CCCCCC">Physical Constraints</th>
                        <th width="132" bgcolor="#CCCCCC"/>
                    </tr>
                    <xsl:for-each select="parameter">
                        <tr>
                            <td width="190">
                                <xsl:value-of select="@name"/>
                            </td>
                            <td>
                                <xsl:value-of select="@value"/>
                            </td>
                            <td width="132"/>
                        </tr>
                    </xsl:for-each>
                </xsl:if>
            </xsl:for-each>

            <!-- Dynamic Response Constraints -->
            <xsl:for-each select="DynamicResponseConstraints">
                <xsl:if test="parameter/@name">
                    <tr>
                        <th colspan="2" bgcolor="#CCCCCC">Dynamic Response Constraints</th>
                        <th width="132" bgcolor="#CCCCCC"/>
                    </tr>
                    <xsl:for-each select="parameter">
                        <tr>
                            <td width="190">
                                <xsl:value-of select="@name"/>
                            </td>
                            <td>
                                <xsl:value-of select="@value"/>
                            </td>
                            <td width="132"></td>
                        </tr>
                    </xsl:for-each>
                </xsl:if>
            </xsl:for-each>

            <!-- Tactical Constraints -->
            <xsl:for-each select="TacticalConstraints">
                <xsl:if test="parameter/@name">
                    <tr>
                        <th colspan="2" width="132" bgcolor="#CCCCCC">Tactical Constraints</th>
                        <th width="132" bgcolor="#CCCCCC"/>
                    </tr>
                    <xsl:for-each select="parameter">
                        <tr>
                            <td width="190">
                                <xsl:value-of select="@name"/>
                            </td>
                            <td>
                                <xsl:value-of select="@value"/>
                            </td>
                            <td width="132"></td>
                        </tr>
                    </xsl:for-each>
                </xsl:if>
            </xsl:for-each>
        </table>
        <p/>
        <p/>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>

    <!-- Behavior Description templates -->
    <xsl:template match="BehaviorDescriptionsDescription" mode="BehaviorHeader"><!-- TODO fix name -->
        <p align="left">
            <font size="4">
                <b><a name="BehaviorDescriptions">Behavior Descriptions</a></b>
            </font>
        </p>
        <p align="left">
            <i>Description of Behavior Design</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    <xsl:template match="BehaviorDescriptionConclusions" mode="BehaviorHeader">
        <p align="left">
            <i>Post-Experiment Analysis of Entity Behaviors</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>

    <!-- Event Graph image and details -->
    <xsl:template match="Behavior">

        <!-- Capture this Behavior's name for the Figure Caption -->
        <xsl:variable name="behavior" select="@name"/>
        <p/>
        <p/>
        <p align="left">
            <b>Event Graph Behavior: </b>
            <xsl:element name="a">
                <xsl:attribute name="name">
                    <xsl:value-of select="@name"/>
                </xsl:attribute>
                <xsl:value-of select="@name"/>
            </xsl:element>
        </p>

        <!-- Add the description -->
        <xsl:for-each select="description">
            <p align="left">
                <b>Description: </b>
                <xsl:value-of select="@text"/>
            </p>
        </xsl:for-each>

        <!-- Add the image of the Event Ggraph -->
        <xsl:for-each select="EventGraphImage">
            <div align="center">
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
                        <xsl:attribute name="src">
                            <xsl:value-of select="@dir"/>
                        </xsl:attribute>
                        <xsl:attribute name="description">
                            <!-- TODO:  more info here -->
                            <xsl:text>location</xsl:text>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:element>

                <!-- add an index to the array -->
                <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
                <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: Event Graph for <xsl:value-of select="$behavior"/></p>
            <!-- TODO add Event Graph name from XML, rename Behavior element, add linked name to figure caption -->
            </div>
        </xsl:for-each>
        <div align="center">
            <table border="1">
                <tr>
                    <th bgcolor="#FFFFCC">Initialization Parameter</th>
                    <th bgcolor="#FFFFCC">Parameter Type</th>
                    <th bgcolor="#FFFFCC">Description</th>
                </tr>
                <!-- Add parameter and state variable table for each event graph -->
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
        </div>
        <p/>
        <p/>
        <xsl:if test="stateVariable">
            <div align="center">
                <table border="1">
                    <tr>
                        <th bgcolor="#FFFFCC">State Variable</th>
                        <th bgcolor="#FFFFCC">Variable Type</th>
                        <th bgcolor="#FFFFCC">Description</th>
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
            </div>
        </xsl:if>
        <p/>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>

    <!-- Statistical Results -->
    <xsl:template match="StatisticalResults" mode="StatisticsHeader">
        <p align="left">
            <font size="4">
                <b><a name="StatisticalResults">Statistical Results for the Simulation</a></b>
            </font>
        </p>
        <p>
            Statistical results are produced by Property Change Listener (PCL) definitions
            in the Assembly model.
        </p>
        <xsl:apply-templates select="StatisticalResultsDescription"/>
        <xsl:apply-templates select="StatisticalResultsConclusions"/>
    </xsl:template>
    
    <xsl:template match="StatisticalResultsDescription" mode="StatisticsHeader">
        <font size="4">
            <p align="left">
                <font size="4">
                    <b><a name="StatisticalResults">Statistical Results</a></b>
                </font>
            </p>
        </font>
        <p align="left">
            <i>Description of Expected Results</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    <xsl:template match="StatisticalResultsConclusions" mode="StatisticsHeader">
        <p align="left">
            <i>Analysis of Experimental Results</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
        <p align="left">
            <b><u>Summary Statistics section</u>: Primary Measures of Effectiveness (MoEs) / Measures of Performance (MoPs) and corresponding statistical plots</b>
        </p>
    </xsl:template>
    
    <xsl:template match="SimEntity" mode="ReplicationStatistics">

        <!-- Capture this Entity's name for its Figure Caption -->
        <xsl:variable name="entityProperty" select="@property"/>
        <p/>
        <p align="left">
            <b>Assembly Simulation Replication Report</b>
        </p>
        <p align="left">
            <b>Measure of Effectiveness (MoE)</b>
        </p>
        <xsl:if test="@name != ''">
            <p align="left">Entity:
                <xsl:value-of select="@name"/>
            </p>
        </xsl:if>
        <p align="left">Property:
            <xsl:value-of select="@property"/>
        </p>
        <xsl:for-each select="HistogramChart">
            <div align="center">
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
                        <xsl:attribute name="src">
                            <xsl:value-of select="@dir"/>
                        </xsl:attribute>
                        <xsl:attribute name="description">
                            <xsl:text>replications histogram output</xsl:text>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:element>

                <!-- add an index to the array -->
                <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
                <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: Replications Histogram for <xsl:value-of select="$entityProperty"/></p>
            </div>
        </xsl:for-each>
        <xsl:for-each select="LinearRegressionChart">
            <div align="center">
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
                        <xsl:attribute name="src">
                            <xsl:value-of select="@dir"/>
                        </xsl:attribute>
                        <xsl:attribute name="description">
                            <xsl:text>replications scatter plot output</xsl:text>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:element>

                <!-- add an index to the array -->
                <xsl:variable name="addIndex" select="java:add($javaCounter, '1')"/>
                <p>Figure <xsl:number value="java:size($javaCounter)" format="1"/>: Replications Regression Plot for <xsl:value-of select="$entityProperty"/></p>
            </div>
        </xsl:for-each>
        <div align="center">
            <!--p>
                <xsl:variable name="allCountsZero">
                    <xsl:choose>
                        <xsl:when test="number(@count) != 0">
                            <xsl:text>allCountsZero: false </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>allCountsZero: true </xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:value-of select="$allCountsZero"/>
                <xsl:variable name="allCountsOne">
                    <xsl:choose>
                        <xsl:when test="number(@count) != 1">
                            <xsl:text>allCountsOne: false </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>allCountsOne: true </xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:value-of select="$allCountsOne"/>
                <xsl:variable name="allMeansEqual">
                    <xsl:variable name="firstMean" select="number(@mean)"/>
                    <xsl:choose>
                        <xsl:when test="number(Replication[@mean]) != $firstMean">
                            <xsl:text>allMeansEqual: false</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>allMeansEqual: true</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:value-of select=" $allMeansEqual"/>
            </p-->
            <table border="1" width="60%">
                <tr>
                    <th bgcolor="#FFFFCC">
                        <b>Replication #</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>
                            <xsl:value-of select="@property"/>
                        </b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Min</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Max</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Mean</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>StdDev</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Variance</b>
                    </th>
                </tr>
                <xsl:for-each select="Replication">
                    <xsl:if test="number(@count) = 0">
                        <tr>
                            <td>
                                <xsl:value-of select="@number"/>
                            </td>
                            <td>
                                <xsl:value-of select="@count"/>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                        </tr>
                    </xsl:if>
                    <xsl:if test="number(@count) = 1">
                        <tr>
                            <td>
                                <xsl:value-of select="@number"/>
                            </td>
                            <td>
                                <xsl:value-of select="@count"/>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                            <td>
                                <xsl:value-of select="@mean"/>
                            </td>
                            <td>
                            </td>
                            <td>
                            </td>
                        </tr>
                    </xsl:if>
                    <xsl:if test="number(@count) &gt; 1">
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
                    </xsl:if>
                </xsl:for-each>
            </table>
        </div>
        <p/>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>
    <xsl:template match="SummaryReport" mode="SummaryStatistics">
        <p/>
        <p align="left">
            <font size="4">
                <b><a name="SummaryReport">Summary Report</a></b>
            </font>
        </p>
        <div align="center">
            <table border="1" width="80%">
                <tr>
                    <th bgcolor="#FFFFCC">
                        <b>Entity</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>MoE / MoP</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b># Replications</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Min</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Max</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Mean</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>StdDev</b>
                    </th>
                    <th bgcolor="#FFFFCC">
                        <b>Variance</b>
                    </th>
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
                            <xsl:value-of select="@numRuns"/>
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
        </div>
        <p/>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>

    <!-- Conclusions and Recommendations -->
    <xsl:template match="ConclusionsRecommendations">
        <p align="left">
            <font size="4">
                <b><a name="ConclusionsRecommendations">Conclusions and Recommendations</a></b>
            </font>
        </p>
        <xsl:apply-templates select="*"/>
    </xsl:template>
    
    <xsl:template match="ConclusionsRecommendationsDescription"><!-- TODO rename -->
        <p align="left">
            <a name="Conclusions"></a><i>Conclusions</i>
            <br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
    </xsl:template>
    
    <xsl:template match="ConclusionsRecommendationsConclusions"><!-- TODO rename -->
        <p align="left">
            <a name="Recommendations"></a><i>Recommendations for Future Work</i><br/>
            <font color="#00006C">
                <xsl:value-of select="@text"/>
            </font>
        </p>
        <a href="#top"><font size="-1" color="#990000">Back to top</font></a>
    </xsl:template>
</xsl:stylesheet>