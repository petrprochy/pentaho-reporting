/*
 * This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 *  Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2006 - 2026 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.engine.classic.core.modules.output.table.xls.helper;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Regression tests for {@link ExcelPrinterBase#openSheet(String)}.
 * <p>
 * A store/group caption longer than 31 characters (Excel's hard sheet-name limit) must still
 * result in a usable, recognisable sheet name (truncated to 31 characters), instead of silently
 * falling back to a generic POI-generated name (e.g. "Sheet0"). Before the fix, {@code openSheet}
 * only logged a warning and discarded the requested name entirely whenever it was longer than 31
 * characters.
 */
public class ExcelPrinterBaseTest {

  /**
   * Minimal concrete subclass so we can exercise the real, un-mocked {@code openSheet} logic
   * against a real POI workbook.
   */
  private static final class TestableExcelPrinterBase extends ExcelPrinterBase {
    private final Workbook workbook = new XSSFWorkbook();

    @Override
    public Workbook getWorkbook() {
      return workbook;
    }

    @Override
    protected Sheet getSheet() {
      return null;
    }
  }

  private TestableExcelPrinterBase printer;

  @Before
  public void setUp() {
    printer = new TestableExcelPrinterBase();
  }

  @Test
  public void openSheet_NameWithinLimit_IsUsedAsIs() {
    final String name = "Uherské Hradiště - Mařatice"; // 27 characters
    assertEquals( 27, name.length() );

    final Sheet sheet = printer.openSheet( name );
    assertEquals( name, sheet.getSheetName() );
  }

  @Test
  public void openSheet_NameExactlyAtLimit_IsUsedAsIs() {
    final String name = maxName();

    final Sheet sheet = printer.openSheet( name );
    assertEquals( name, sheet.getSheetName() );
  }

  @Test
  public void openSheet_NameLongerThan31Characters() {
    // Real-world example that triggered this bug: store caption "1100-Uherské Hradiště - Mařatice" (32 characters)
    final String name = "1100-Uherské Hradiště - Mařatice";
    final String truncated = "1100-Uherské Hradiště - Mařatic";
    assertEquals( 32, name.length() );

    final Sheet sheet = printer.openSheet( name );

    // The sheet must still be named after the store - just truncated to fit - not silently
    // replaced by POI's generic default name.
    assertNotEquals( "Sheet0", sheet.getSheetName() );
    assertThat("Sheet name must never exceed Excel's 31 character limit",
      sheet.getSheetName().length(),
      Matchers.lessThanOrEqualTo( 31 ) );
    assertEquals( "Truncated name", truncated, sheet.getSheetName() );
  }

  @Test
  public void openSheet_DuplicateNamesAfterTruncation_AreDisambiguated() {
    // Two distinct, valid group values that happen to share the same first 31 characters must
    // not collapse into two sheets with the identical truncated name.
    final String longPrefix = maxName();

    final Sheet first = printer.openSheet( longPrefix + " Store A" );
    final Sheet second = printer.openSheet( longPrefix + " Store B" );

    assertEquals( longPrefix, first.getSheetName() );
    assertNotEquals( "openSheet must not create two sheets with the identical (truncated) name",
      first.getSheetName(), second.getSheetName() );
    assertEquals( "First sheet", longPrefix, first.getSheetName() );
    assertEquals("Second sheet", "Sheet1", second.getSheetName() );
  }

  @Test
  public void openSheet_InvalidCharacters_FallsBackToDefaultName() {
    // Unchanged, pre-existing behaviour: POI/Excel forbid : \ / ? * [ ] in sheet names. This is
    // not the bug being regression-tested here, just documenting that it keeps working.
    final Sheet sheet = printer.openSheet( "Sklad: Praha" );
    assertEquals("Escaped sheet name", "Sklad  Praha", sheet.getSheetName() );
  }


  private static String maxName() {
    final StringBuilder sb = new StringBuilder( 31 );
    for ( int i = 0; i < 31; i++ ) {
      sb.append( (char) ( 'A' + ( i % 26 ) ) );
    }
    return sb.toString();
  }
}