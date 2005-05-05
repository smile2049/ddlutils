package org.apache.ddlutils.io;

import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.util.DDLExecutor;

public class TestAgainstAxionDb extends TestDatabaseWriterBase
{
    /** The database schema for testing all column types that Axion supports */
    public static final String COLUMN_TEST_SCHEMA =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"+
        "<database name=\"datatypetest\">\n"+
        "  <table name=\"coltype\">\n"+
        "    <column name=\"COL_BIGINT\"          type=\"BIGINT\"/>\n"+
        "    <column name=\"COL_BINARY\"          type=\"BINARY\"/>\n"+
        "    <column name=\"COL_BIT\"             type=\"BIT\"/>\n"+
        "    <column name=\"COL_BLOB\"            type=\"BLOB\"/>\n"+
        "    <column name=\"COL_BOOLEAN\"         type=\"BOOLEAN\"/>\n"+
        "    <column name=\"COL_CHAR\"            size=\"15\" type=\"CHAR\"/>\n"+
        "    <column name=\"COL_CLOB\"            type=\"CLOB\"/>\n"+
        "    <column name=\"COL_DATE\"            type=\"DATE\"/>\n"+
        "    <column name=\"COL_DECIMAL\"         type=\"DECIMAL\" scale=\"3\" size=\"15\"/>\n"+
        "    <column name=\"COL_DECIMAL_NOSCALE\" type=\"DECIMAL\" size=\"15\"/>\n"+
        "    <column name=\"COL_DOUBLE\"          type=\"DOUBLE\"/>\n"+
        "    <column name=\"COL_FLOAT\"           type=\"FLOAT\"/>\n"+
        "    <column name=\"COL_INTEGER\"         type=\"INTEGER\"/>\n"+
        "    <column name=\"COL_JAVA_OBJECT\"     type=\"JAVA_OBJECT\"/>\n"+
        "    <column name=\"COL_LONGVARBINARY\"   type=\"LONGVARBINARY\"/>\n"+
        "    <column name=\"COL_LONGVARCHAR\"     type=\"LONGVARCHAR\"/>\n"+
        "    <column name=\"COL_NUMERIC\"         type=\"NUMERIC\" size=\"15\" />\n"+
        "    <column name=\"COL_REAL\"            type=\"REAL\"/>\n"+
        "    <column name=\"COL_SMALLINT\"        type=\"SMALLINT\"/>\n"+
        "    <column name=\"COL_TIME\"            type=\"TIME\"/>\n"+
        "    <column name=\"COL_TIMESTAMP\"       type=\"TIMESTAMP\"/>\n"+
        "    <column name=\"COL_TINYINT\"         type=\"TINYINT\"/>\n"+
        "    <column name=\"COL_VARBINARY\"       size=\"15\" type=\"VARBINARY\"/>\n"+
        "    <column name=\"COL_VARCHAR\"         size=\"15\" type=\"VARCHAR\"/>\n"+
        "  </table>\n"+
        "</database>";

    /* (non-Javadoc)
     * @see org.apache.ddlutils.builder.TestBuilderBase#getDatabaseName()
     */
    protected String getDatabaseName()
    {
        return "Axion";
    }

    public void testCreation() throws Exception
    {
        Database testDb = parseDatabaseFromString(COLUMN_TEST_SCHEMA);

        getBuilder().createDatabase(testDb);

        DDLExecutor executor = new DDLExecutor(getDataSource());
        String      sql      = getBuilderOutput();

        System.out.println(sql);
        executor.evaluateBatch(sql);
    }
}
