/*
 * Copyright 2003 - 2009 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.sales;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9d1c96f1-f43d-47ae-a0c0-6c61783ad3e6")
@EFapsRevision("$Rev$")
public abstract class TextModule_Base
{

    /**
     * Get the drop down for the account select of an invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return html snipplet
     * @throws EFapsException on error
     */
    public Return getTextElementsValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final Type type = Type.get(CISales.Invoice.uuid);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.TextModule);
        queryBldr.addWhereAttrEqValue(CISales.TextModule.ForTypeId, type.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.TextModule.Line);
        multi.execute();

        final Map<Long, Map<String, Long>> values = new TreeMap<Long, Map<String, Long>>();
        while (multi.next()) {
            final Instance instance = multi.getCurrentInstance();
            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TextElement);
            queryBldr2.addWhereAttrEqValue(CISales.TextElement.TextModule, instance.getId());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CISales.TextElement.ShortText);
            multi2.execute();
            final Map<String, Long> textele = new TreeMap<String, Long>();
            while (multi2.next()) {
                textele.put((String) multi2.getAttribute(CISales.TextElement.ShortText),
                                                                    multi2.getCurrentInstance().getId());
            }
            values.put(multi.<Long>getAttribute(CISales.TextModule.Line), textele);
        }

        final StringBuilder html = new StringBuilder();
        html.append("<table>");
        for (final Entry<Long, Map<String, Long>> entry : values.entrySet()) {
            html.append("<tr><td>");
            if (entry.getValue().size() > 1) {
                html.append("<select name=\"").append(fieldValue.getField().getName()).append("\" size=\"1\">");
                for (final Entry<String, Long> textEntry : entry.getValue().entrySet()) {
                    html.append("<option value=\"").append(textEntry.getValue())
                                    .append("\">").append(textEntry.getKey()).append("</option>");
                }
                html.append("</select>");
            } else {
                for (final Entry<String, Long> textEntry : entry.getValue().entrySet()) {
                    html.append("<span>").append(textEntry.getKey()).append("</span>")
                                    .append("<input type=\"hidden\" value=\"").append(textEntry.getValue()).append("\"")
                                    .append(" name=\"").append(fieldValue.getField().getName()).append("\" />");
                }
            }
            html.append("</td></tr>");
        }
        html.append("</table>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html.toString());
        return retVal;
    }

    /**
     * Get the drop down for the account select of an invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return html snipplet
     * @throws EFapsException on error
     */
    public Return getTextElements4ReadValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getCallInstance();
        final Map<Integer, String> line2Text = new TreeMap<Integer, String>();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2TextElement);
        queryBldr.addWhereAttrEqValue(CISales.Document2TextElement.Document, instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        if (!multi.getInstanceList().isEmpty()) {
            final SelectBuilder selLongText = new SelectBuilder()
                            .linkto(CISales.Document2TextElement.TextElement).attribute(CISales.TextElement.LongText);
            final SelectBuilder selLine = new SelectBuilder()
                            .linkto(CISales.Document2TextElement.TextElement)
                            .linkto(CISales.TextElement.TextModule).attribute(CISales.TextModule.Line);
            multi.addSelect(selLine, selLongText);
            multi.execute();
            while (multi.next()) {
                Integer line = multi.<Integer> getSelect(selLine);
                if (line == null) {
                    line = new Integer(multi.getInstanceList().size() + 100);
                }
                line2Text.put(line, multi.<String> getSelect(selLongText));
            }
        }
        final StringBuilder bldr = new StringBuilder();
        for (final String text : line2Text.values()) {
            bldr.append(text).append("<br/>");
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, bldr.toString());
        return retVal;
    }

    /**
     * Method to get a droopdown with the non abstract childtypes of the document abstract.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with the list.
     * @throws EFapsException on error
     */
    public Return getTypeFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);

        final Type transAbstract = CISales.DocumentAbstract.getType();
        final Map <String, Long> values = new TreeMap<String, Long>();
        for (final Type child : transAbstract.getChildTypes()) {
            if (!child.isAbstract()) {
                values.put(DBProperties.getProperty(child.getName() + ".Label"), child.getId());
            }
        }

        final StringBuilder html = new StringBuilder();

        html.append("<select size=\"1\" name=\"").append(fieldValue.getField().getName()).append("\">");
        for (final Map.Entry<String, Long> entry : values.entrySet()) {
            html.append("<option value=\"").append(entry.getValue()).append("\">").append(entry.getKey())
                .append("</option>");
        }
        html.append("</select>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * Method to get a the label of the type used..
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with the list.
     * @throws EFapsException on error
     */
    public Return getTypeFieldValueUI4Read(final Parameter _parameter)
        throws EFapsException
    {
        final Type transAbstract = CISales.DocumentAbstract.getType();
        final Map<Long, String> values = new TreeMap<Long, String>();
        for (final Type child : transAbstract.getChildTypes()) {
            if (!child.isAbstract()) {
                values.put(child.getId(), DBProperties.getProperty(child.getName() + ".Label"));
            }
        }

        final Instance instance = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CISales.TextModule.ForTypeId);
        print.execute();
        final String docName = values.get(print.<Long>getAttribute(CISales.TextModule.ForTypeId));
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, docName);
        return ret;
    }
}
