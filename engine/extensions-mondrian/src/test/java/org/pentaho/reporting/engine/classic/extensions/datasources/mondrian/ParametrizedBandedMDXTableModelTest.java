package org.pentaho.reporting.engine.classic.extensions.datasources.mondrian;

import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.ParameterDataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.testsupport.DataSourceTestBase;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @since 2025-03-11
 */
public class ParametrizedBandedMDXTableModelTest extends DataSourceTestBase {
  private DataRow parameter;

  @Override protected DataFactory createDataFactory( String query ) throws ReportDataFactoryException {
    try {
      final BandedMDXDataFactory df = new BandedMDXDataFactory();
      final URL resource = getClass().getResource( "steelwheels_hierarchy.mondrian.xml" );
      assertNotNull( "steelwheels_hierarchy.mondrian.xml", resource );
      df.setCubeFileProvider( new DefaultCubeFileProvider( new File( resource.toURI() ).getAbsolutePath() ) );
      df.setDataSourceProvider( new JndiDataSourceProvider( "SampleData" ) );
      df.setQuery( getLogicalQueryForNextTest(), query );
      return df;
    } catch ( URISyntaxException e ) {
      throw new ReportDataFactoryException( e );
    }
  }

  @Override protected String getLogicalQueryForNextTest() {
    return getName() == null ? "default" : getName();
  }

  @Override protected DataRow getParameterForNextTest() {
    return parameter;
  }

  public void testSameHierarchy() throws Exception {
    parameter = new ParameterDataRow( new String[] {
      "time",
      "market",
      "city",
      "time_past"
    }, new Object[] {
      "[Time].[2004]",
      "[Markets].[Japan]",
      "[Markets.City].[Frankfurt]",
      "[Time].[past]"
    } );
    runTest( new String[][] {
      { "select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY [Product].[All Products].Children ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where Parameter(\"time\", [Time], [Time].[2005])", "parameter1-banded-results.txt" },
      { "select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY [Product].[All Products].Children ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where {Parameter(\"market\", [Markets], [Markets].[NA])}", "parameter2-banded-results.txt" },
      { "select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY [Product].[All Products].Children ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where {Parameter(\"city\", [Markets.City], [Markets.City].[Munich])}", "parameter3-banded-results.txt" },
      { "with member [Time].[All Years].[past] as '([Time].[2003] + [Time].[2004])'\n"
        + "select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY {[Product].[All Products]} ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where Parameter(\"time_past\", [Time], [Time].[2005])", "parameter4-banded-results.txt" },
    } );
  }

  public void testAnotherHierarchy() throws Exception {
    parameter = new ParameterDataRow( new String[] {
      "market"
    }, new Object[] {
      "[Markets].[Japan]"
    } );
    runTest(new String[][] {
      {"select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY [Product].[All Products].Children ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where {Parameter(\"market\", [Markets.City], [Markets.City].[Munich])}", "parameter2-banded-results.txt" },
    });
  }

  public void testDefinedByKey() throws Exception {
    parameter = new ParameterDataRow( new String[] {
      "market_name",
    }, new Object[] {
      "BC", // [Markets].[NA].[Canada].[BC]
    } );
    runTest( new String[][] {
      { "select NON EMPTY {[Measures].[Sales]} ON COLUMNS,\n"
        + "  NON EMPTY {[Product].[All Products]} ON ROWS\n"
        + "from [SteelWheelsSales]\n"
        + "where Parameter(\"market_name\", [Markets], [Markets].[Japan])", "parameter5-banded-results.txt" },
    } );
  }

  public static void main( String[] args ) throws Exception {
    if ( args.length == 0 || args.length % 2 != 0 ) {
      throw new IllegalArgumentException( "Wrong number of arguments! Must be grouped by <MDX> and <output file>" );
    }
    final ParametrizedBandedMDXTableModelTest test = new ParametrizedBandedMDXTableModelTest();
    test.setUp();

    // even = MDX, odd = output file name
    for ( int i = 0; i < args.length / 2; i++ ) {
      final int j = i * 3;
      final String[][] data = new String[ 1 ][ 2 ];
      data[ 0 ][ 0 ] = args[ j ];
      data[ 0 ][ 1 ] = args[ j + 1 ];
      test.parameter = new ParameterDataRow();
      test.runGenerate( data );
    }

  }

}
