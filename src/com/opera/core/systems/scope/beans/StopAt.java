//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.01.11 at 10:36:39 AM CET 
//


package com.opera.core.systems.scope.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element ref="{}no"/>
 *           &lt;element ref="{}yes"/>
 *         &lt;/choice>
 *         &lt;element ref="{}stop-type"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "no",
    "yes",
    "stopType"
})
@XmlRootElement(name = "stop-at")
public class StopAt {

    protected No no;
    protected Yes yes;
    @XmlElement(name = "stop-type", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String stopType;

    /**
     * Gets the value of the no property.
     * 
     * @return
     *     possible object is
     *     {@link No }
     *     
     */
    public No getNo() {
        return no;
    }

    /**
     * Sets the value of the no property.
     * 
     * @param value
     *     allowed object is
     *     {@link No }
     *     
     */
    public void setNo(No value) {
        this.no = value;
    }

    /**
     * Gets the value of the yes property.
     * 
     * @return
     *     possible object is
     *     {@link Yes }
     *     
     */
    public Yes getYes() {
        return yes;
    }

    /**
     * Sets the value of the yes property.
     * 
     * @param value
     *     allowed object is
     *     {@link Yes }
     *     
     */
    public void setYes(Yes value) {
        this.yes = value;
    }

    /**
     * Gets the value of the stopType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStopType() {
        return stopType;
    }

    /**
     * Sets the value of the stopType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStopType(String value) {
        this.stopType = value;
    }

}