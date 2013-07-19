<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:httpx="http://openrepose.org/repose/httpx/v1.0"
                version="1.0">

    <xsl:output method="xml"/>
    <!-- declare the parameters that will be passed into our translation chain -->
    <xsl:param name="input-headers-uri"/>
    <xsl:param name="output-headers-uri"/>

    <!-- read the input documents into variables -->
    <xsl:variable name="headersDoc" select="doc($input-headers-uri)"/>

    <xsl:template match="/">
        <xsl:copy-of select="."/>
        <xsl:apply-templates select="$headersDoc/*"/>
    </xsl:template>

    <xsl:template match="httpx:headers">
        <!-- we could specify the href as $output-headers-uri as well.  Here we use repose:output:headers.xml -->
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:headers.xml">
            <httpx:headers>
                <httpx:request>
                    <xsl:apply-templates/>
                </httpx:request>
            </httpx:headers>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:header">
    <xsl:choose>
        <xsl:when test="starts-with(@name, 'x-pp-user')">
            <xsl:element name="httpx:header">
                <xsl:attribute name="name">
                    <xsl:value-of select="'x-rax-username'"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@value"/>
                </xsl:attribute>
                <xsl:attribute name="quality">
                    <xsl:value-of select="@quality"/>
                </xsl:attribute>
            </xsl:element>
        </xsl:when>
        <xsl:when test="starts-with(@name, 'x-tenant-name')">
            <xsl:element name="httpx:header">
                <xsl:attribute name="name">
                    <xsl:value-of select="'x-rax-tenants'"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@value"/>
                </xsl:attribute>
                <xsl:attribute name="quality">
                    <xsl:value-of select="@quality"/>
                </xsl:attribute>
            </xsl:element>
        </xsl:when>
        <xsl:when test="starts-with(@name, 'x-roles')">
            <xsl:element name="httpx:header">
                <xsl:attribute name="name">
                    <xsl:value-of select="'x-rax-roles'"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@value"/>
                </xsl:attribute>
                <xsl:attribute name="quality">
                    <xsl:value-of select="@quality"/>
                </xsl:attribute>
            </xsl:element>
        </xsl:when>
        <xsl:otherwise>
            <xsl:element name="httpx:header">
                <xsl:attribute name="name">
                    <xsl:value-of select="@name"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@value"/>
                </xsl:attribute>
                <xsl:attribute name="quality">
                    <xsl:value-of select="@quality"/>
                </xsl:attribute>
            </xsl:element>
        </xsl:otherwise>            
    </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
