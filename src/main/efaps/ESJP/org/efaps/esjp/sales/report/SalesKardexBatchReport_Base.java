/*
 * Copyright 2003 - 2018 The eFaps Team
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
 */

package org.efaps.esjp.sales.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("25511cee-f877-4c38-a2d9-7b377d596964")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesKardexBatchReport_Base
    extends AbstractCommon
{
    private static final Logger LOG = LoggerFactory.getLogger(SalesKardexBatchReport.class);

    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue(
                        CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateFrom.name));
        final DateTime dateTo = new DateTime(_parameter.getParameterValue(
                        CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateTo.name));

        final KardexReport baseResport = new KardexReport();
        final List<Instance> storageInst = baseResport.getStorageInstList(_parameter);
        final QueryBuilder queryBuilder = new QueryBuilder(CIProducts.TransactionAbstract);
        queryBuilder.addWhereAttrEqValue(CIProducts.TransactionAbstract.Storage, storageInst.toArray());
        final MultiPrintQuery multi = queryBuilder.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product).instance();
        final SelectBuilder selProdName = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product).attribute(
                        CIProducts.ProductAbstract.Name);
        multi.addSelect(selProdInst, selProdName);
        multi.executeWithoutAccessCheck();
        final Map<Instance, String> product2Name = new HashMap<>();
        while (multi.next()) {
            final Instance prodInst = multi.getSelect(selProdInst);
            final String prodName = multi.getSelect(selProdName);
            product2Name.putIfAbsent(prodInst, prodName);
        }
        final FileUtil fileUtil = new FileUtil();

        final Map<Instance, String> product2Path = new HashMap<>();
        for (final Entry<Instance, String> entry : product2Name.entrySet()) {
            final KardexReport report = new KardexReport();
            ParameterUtil.addParameterValues(_parameter,
                            CIFormSales.Sales_Products_Kardex_OfficialReportForm.product.name, entry.getKey().getOid());
            final Return temp = report.createReport(_parameter);
            final File file = (File) temp.get(ReturnValues.VALUES);
            final File tmpFile = fileUtil.getFile("SK" + entry.getKey().getOid() , "xlxs");
            try {
                Files.copy(file.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                LOG.error("Catched IOException", e);
            }
            product2Path.put(entry.getKey(), tmpFile.getAbsolutePath());
        }

        final Workbook targetWb = new XSSFWorkbook();
        final Sheet baseSheet = targetWb.createSheet("Inventario");
        for (final Entry<Instance, String> entry : product2Path.entrySet()) {
            Workbook workbook;
            try {
                workbook = WorkbookFactory.create(new File(entry.getValue()));
                final Sheet sheet = workbook.getSheetAt(0);
                final Sheet newSheet = targetWb.createSheet(product2Name.get(entry.getKey()));
                copySheet(sheet, newSheet, true);
                appendSheet(sheet, baseSheet, true, 2);
            } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
                LOG.error("Catched Exception", e);
            }
        }
        final Return ret = new Return();
        try {
            final File temp = fileUtil.getFile(baseResport.getReportName(_parameter, dateFrom, dateTo), "xlsx");
            final FileOutputStream fileOut = new FileOutputStream(temp);
            targetWb.write(fileOut);
            fileOut.close();
            ret.put(ReturnValues.VALUES, temp);
            ret.put(ReturnValues.TRUE, true);
        } catch (final IOException e) {
            LOG.error("Catched IOException", e);
        }
        return ret;
    }

    protected void appendSheet(final Sheet _sourceSheet, final Sheet _targetSheet, final boolean _copyStyle,
                               final int _offset)
    {
        int maxColumnNum = 0;
        final Map<Integer, CellStyle> styleMap = _copyStyle ? new HashMap<>() : null;
        final Set<CellRangeAddressWrapper> mergedRegions = new HashSet<>();
        final boolean first = _targetSheet.getLastRowNum() == 0;
        int j = first ? 0 : _targetSheet.getLastRowNum() + 1 + _offset;
        for (int i = _sourceSheet.getFirstRowNum(); i <= _sourceSheet.getLastRowNum(); i++) {
            final Row srcRow = _sourceSheet.getRow(i);
            final Row destRow = _targetSheet.createRow(j);
            if (srcRow != null) {
                copyRow(_sourceSheet, _targetSheet, srcRow, destRow, styleMap, mergedRegions, false);
                if (srcRow.getLastCellNum() > maxColumnNum) {
                    maxColumnNum = srcRow.getLastCellNum();
                }
            }
            j++;
        }
        if (first) {
            for (int i = 0; i <= maxColumnNum; i++) {
                _targetSheet.setColumnWidth(i, _sourceSheet.getColumnWidth(i));
            }
        }
    }

    protected void copySheet(final Sheet _sourceSheet, final Sheet _targetSheet, final boolean _copyStyle)
    {
        int maxColumnNum = 0;
        final Map<Integer, CellStyle> styleMap = _copyStyle ? new HashMap<>() : null;
        final Set<CellRangeAddressWrapper> mergedRegions = new HashSet<>();
        for (int i = _sourceSheet.getFirstRowNum(); i <= _sourceSheet.getLastRowNum(); i++) {
            final Row srcRow = _sourceSheet.getRow(i);
            final Row destRow = _targetSheet.createRow(i);
            if (srcRow != null) {
                copyRow(_sourceSheet, _targetSheet, srcRow, destRow, styleMap, mergedRegions, true);
                if (srcRow.getLastCellNum() > maxColumnNum) {
                    maxColumnNum = srcRow.getLastCellNum();
                }
            }
        }
        for (int i = 0; i <= maxColumnNum; i++) {
            _targetSheet.setColumnWidth(i, _sourceSheet.getColumnWidth(i));
        }
    }

    protected void copyRow(final Sheet _srcSheet, final Sheet _destSheet, final Row _srcRow, final Row _destRow,
                           final Map<Integer, CellStyle> _styleMap, final Set<CellRangeAddressWrapper> mergedRegions,
                           final boolean _copyFormular)
    {
        _destRow.setHeight(_srcRow.getHeight());
        final int deltaRows = _destRow.getRowNum() - _srcRow.getRowNum();
        for (int j = _srcRow.getFirstCellNum(); j <= _srcRow.getLastCellNum(); j++) {
            final Cell oldCell = _srcRow.getCell(j);
            Cell newCell = _destRow.getCell(j);
            if (oldCell != null) {
                if (newCell == null) {
                    newCell = _destRow.createCell(j);
                }
                copyCell(oldCell, newCell, _styleMap, _copyFormular);
                final CellRangeAddress mergedRegion = getMergedRegion(_srcSheet, _srcRow.getRowNum(), (short) oldCell
                                .getColumnIndex());

                if (mergedRegion != null) {
                    final CellRangeAddress newMergedRegion = new CellRangeAddress(mergedRegion.getFirstRow()
                                    + deltaRows, mergedRegion.getLastRow() + deltaRows, mergedRegion.getFirstColumn(),
                                    mergedRegion.getLastColumn());
                    final CellRangeAddressWrapper wrapper = new CellRangeAddressWrapper(newMergedRegion);
                    if (isNewMergedRegion(wrapper, mergedRegions)) {
                        mergedRegions.add(wrapper);
                        _destSheet.addMergedRegion(wrapper.range);
                    }
                }
            }
        }
    }

    /**
     * @param _sheet the sheet containing the data.
     * @param _rowNum the num of the row to copy.
     * @param _cellNum the num of the cell to copy.
     * @return the CellRangeAddress created.
     */
    protected CellRangeAddress getMergedRegion(final Sheet _sheet, final int _rowNum, final short _cellNum)
    {
        CellRangeAddress ret = null;
        for (int i = 0; i < _sheet.getNumMergedRegions(); i++) {
            final CellRangeAddress merged = _sheet.getMergedRegion(i);
            if (merged.isInRange(_rowNum, _cellNum)) {
                ret = merged;
                break;
            }
        }
        return ret;
    }

    /**
     * Check that the merged region has been created in the destination sheet.
     *
     * @param _newMergedRegion the new merged region
     * @param _mergedRegions the merged regions
     * @return true if the merged region is already in the list or not.
     */
    protected boolean isNewMergedRegion(final CellRangeAddressWrapper _newMergedRegion,
                                        final Set<CellRangeAddressWrapper> _mergedRegions)
    {
        return !_mergedRegions.contains(_newMergedRegion);
    }

    protected void copyCell(final Cell _oldCell, final Cell _newCell, final Map<Integer, CellStyle> _styleMap,
                            final boolean _copyFormular)
    {
        if (_styleMap != null) {
            if (_oldCell.getSheet().getWorkbook() == _newCell.getSheet().getWorkbook()) {
                _newCell.setCellStyle(_oldCell.getCellStyle());
            } else {
                final int stHashCode = _oldCell.getCellStyle().hashCode();
                CellStyle newCellStyle = _styleMap.get(stHashCode);
                if (newCellStyle == null) {
                    newCellStyle = _newCell.getSheet().getWorkbook().createCellStyle();
                    newCellStyle.cloneStyleFrom(_oldCell.getCellStyle());
                    _styleMap.put(stHashCode, newCellStyle);
                }
                _newCell.setCellStyle(newCellStyle);
            }
        }
        switch (_oldCell.getCellTypeEnum()) {
            case STRING:
                _newCell.setCellValue(_oldCell.getStringCellValue());
                break;
            case NUMERIC:
                _newCell.setCellValue(_oldCell.getNumericCellValue());
                break;
            case BLANK:
                _newCell.setCellType(CellType.BLANK);
                break;
            case BOOLEAN:
                _newCell.setCellValue(_oldCell.getBooleanCellValue());
                break;
            case ERROR:
                _newCell.setCellErrorValue(_oldCell.getErrorCellValue());
                break;
            case FORMULA:
                if (_copyFormular) {
                    _newCell.setCellFormula(_oldCell.getCellFormula());
                } else {
                    switch (_oldCell.getCachedFormulaResultTypeEnum()) {
                        case NUMERIC:
                            _newCell.setCellValue(_oldCell.getNumericCellValue());
                            break;
                        default:
                            break;
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * The Class CellRangeAddressWrapper.
     *
     * @author The eFaps Team
     */
    public static class CellRangeAddressWrapper
    {

        /** The range. */
        private final CellRangeAddress range;

        /**
         * @param _theRange the CellRangeAddress object to wrap.
         */
        public CellRangeAddressWrapper(final CellRangeAddress _theRange)
        {
            this.range = _theRange;
        }

        /**
         * @param _o the object to compare.
         * @return -1 the current instance is prior to the object in parameter,
         *         0: equal, 1: after...
         */
        @Override
        public boolean equals(final Object _object)
        {
            boolean ret;
            if (_object == null) {
                ret = super.equals(_object);
            } else {
                final CellRangeAddressWrapper wrapper = (CellRangeAddressWrapper) _object;
                ret = this.range.getFirstColumn() == wrapper.range.getFirstColumn() && this.range
                                .getFirstRow() == wrapper.range.getFirstRow() && this.range
                                                .getLastColumn() == wrapper.range.getLastColumn() && this.range
                                                                .getLastRow() == wrapper.range.getLastRow();
            }
            return ret;
        }

        @Override
        public int hashCode()
        {
            return this.range.hashCode();
        }

        @Override
        public String toString()
        {
            return this.range.toString();
        }
    }

    public static class KardexReport extends SalesKardexReport {

        @Override
        protected List<Instance> getStorageInstList(final Parameter _parameter)
            throws EFapsException
        {
            return super.getStorageInstList(_parameter);
        }
    }

}
