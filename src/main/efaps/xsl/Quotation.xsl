<?xml version="1.0" encoding="UTF-8" ?>
<!--

    Copyright © 2003 - 2024 The eFaps Team (-)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->






<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <xsl:output indent="yes" encoding="UTF-8"/>
    <xsl:template match="/">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="DinA4" page-width="210mm" page-height="297mm" margin-top="10mm" margin-bottom="10mm" margin-left="10mm" margin-right="10mm">
                    <fo:region-body/>
                    <fo:region-after region-name="foot" extent="1cm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="DinA4">
                <fo:static-content flow-name="foot">
                    <fo:block text-align="center">
                        <fo:page-number/>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body">
                    <xsl:apply-templates/>
                    <fo:block text-align="end" font-size="10pt" font-family="arial, verdana, helvetica, sans-serif" line-height="1pt" color="#000000" font-weight="bold" border-width="1mm" padding-after="20px">
                        <xsl:value-of select="/eFaps/form[3]/f_row[1]/f_cell[1]/value[1]"/>: <xsl:value-of select="/eFaps/form[3]/f_row[1]/f_cell[2]/value[1]"/>
                    </fo:block>
                    <fo:block text-align="end" font-size="10pt" font-family="arial, verdana, helvetica, sans-serif" line-height="1pt" color="#000000" font-weight="bold" border-width="1mm" padding-after="20px">
                        <xsl:value-of select="/eFaps/form[3]/f_row[2]/f_cell[1]/value[1]"/>: <xsl:value-of select="/eFaps/form[3]/f_row[2]/f_cell[2]/value[1]"/>
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
    <xsl:template match="title">
        <fo:block-container absolute-position="absolute">
            <fo:block font-size="18pt" font-family="arial, verdana, helvetica, sans-serif" line-height="1pt" color="#000000" font-weight="bold" border-width="1mm">
                <xsl:value-of select="/eFaps/title"/>
            </fo:block>
        </fo:block-container>
        <fo:block text-align="end" font-size="11pt" font-family="arial, verdana, helvetica, sans-serif" line-height="1pt" color="#000000" font-weight="bold" border-width="1mm" padding-after="20px">
            <xsl:value-of select="/eFaps/form[1]/f_row[1]/f_cell[1]/value[1]"/>: <xsl:value-of select="/eFaps/form[1]/f_row[1]/f_cell[2]/value[1]"/>
        </fo:block>
        <fo:block padding-before="75px" font-size="12pt" font-family="arial, verdana, helvetica, sans-serif" line-height="1pt" color="#000000" font-weight="bold" border-width="1mm" padding-after="20px">
            <xsl:value-of select="/eFaps/form[2]/f_row[1]/f_cell[1]/value[1]"/>: <xsl:value-of select="/eFaps/form[2]/f_row[1]/f_cell[2]/value[1]"/>
        </fo:block>
    </xsl:template>
    <xsl:template match="form"/>
    <xsl:template match="table">
        <fo:block font-size="10pt" padding-after="20px">
            <fo:table>
                <fo:table-column column-width="45px"/>
                <fo:table-column column-width="45px"/>
                <fo:table-column column-width="240px"/>
                <fo:table-column column-width="45px"/>
                <fo:table-column column-width="70px"/>
                <fo:table-column column-width="50px"/>
                <fo:table-column column-width="50px"/>
                
                <fo:table-header>
                    <fo:table-row>
                        <xsl:for-each select=".//t_header/t_cell">
                            <fo:table-cell>
                                <fo:block font-weight="bold" background-color="#008800" color="white" padding-left="2px" margin-right="2px" border-right-style="solid" border-right-width="2pt" border-right-color="#104800">
                                    <xsl:value-of select=".//value"/>
                                </fo:block>
                            </fo:table-cell>
                        </xsl:for-each>
                    </fo:table-row>
                </fo:table-header>
                <xsl:if test="count(.//t_body) &lt; 1">
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block font-size="9pt">
                                    <xsl:text>No Data available!</xsl:text>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </xsl:if>
                <xsl:for-each select=".//t_body">
                    <fo:table-body>
                        <xsl:for-each select=".//t_row">
                            <fo:table-row>
                                <xsl:for-each select=".//t_cell">
                                    <fo:table-cell>
                                        <fo:block font-size="9pt" border-bottom-style="dotted" border-bottom-width="1pt" border-bottom-color="#008800">
                                            <xsl:value-of select=".//value"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </xsl:for-each>
                            </fo:table-row>
                        </xsl:for-each>
                    </fo:table-body>
                </xsl:for-each>
            </fo:table>
        </fo:block>
    </xsl:template>
</xsl:stylesheet>