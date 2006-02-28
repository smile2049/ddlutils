package org.apache.ddlutils.platform.firebird;

/*
 * Copyright 2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.DatabaseMetaDataWrapper;
import org.apache.ddlutils.platform.JdbcModelReader;

/**
 * The Jdbc Model Reader for Firebird.
 *
 * @author Martin van den Bemt
 * @author Thomas Dudziak
 * @version $Revision: $
 */
public class FirebirdModelReader extends JdbcModelReader
{
    /**
     * Creates a new model reader for Firebird databases.
     * 
     * @param platformInfo The platform specific settings
     */
    public FirebirdModelReader(PlatformInfo platformInfo)
    {
        super(platformInfo);
        setDefaultCatalogPattern(null);
        setDefaultSchemaPattern(null);
        setDefaultTablePattern("%");
    }

    /**
     * {@inheritDoc}
     */
    protected Table readTable(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        Table table = super.readTable(metaData, values);

        if (table != null)
        {
        	determineAutoIncrementColumns(table);
        }

        return table;
    }

    /**
     * {@inheritDoc}
     */
    protected Collection readColumns(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        ResultSet columnData = null;

        try
        {
            List columns = new ArrayList();

            if (getPlatformInfo().isUseDelimitedIdentifiers())
        	{
        		// Jaybird has a problem when delimited identifiers are used as
        		// it is not able to find the columns for the table
        		// So we have to filter manually below
        		columnData = metaData.getColumns(getDefaultTablePattern(), null);

        		while (columnData.next())
                {
                    Map values = readColumns(columnData, getColumnsForColumn());

                    if (tableName.equals(values.get("TABLE_NAME")))
                    {
                    	columns.add(readColumn(metaData, values));
                    }
                }
        	}
        	else
        	{
        		columnData = metaData.getColumns(tableName, null);

        		while (columnData.next())
                {
                    Map values = readColumns(columnData, getColumnsForColumn());

                    columns.add(readColumn(metaData, values));
                }
        	}

            return columns;
        }
        finally
        {
            if (columnData != null)
            {
                columnData.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
	protected Column readColumn(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
	{
		Column column = super.readColumn(metaData, values);

		if (column.getTypeCode() == Types.FLOAT)
		{
			column.setTypeCode(Types.REAL);
		}
		return column;
	}

	/**
     * Helper method that determines the auto increment status using Firebird's system tables.
     *
     * @param table The table
     */
    protected void determineAutoIncrementColumns(Table table) throws SQLException
    {
    	// Since for long table and column names, the generator name will be shortened
    	// we have to determine for each column whether there is a generator for it
    	FirebirdBuilder builder = new FirebirdBuilder(getPlatformInfo());
    	Column[]        columns = table.getColumns();
    	HashMap         names   = new HashMap();
        String          name;

    	for (int idx = 0; idx < columns.length; idx++)
    	{
    	    name = builder.getGeneratorName(table, columns[idx]);
            if (!getPlatformInfo().isUseDelimitedIdentifiers())
            {
                name = name.toUpperCase();
            }
    		names.put(name, columns[idx]);
    	}

    	Statement stmt = getConnection().createStatement();

    	try
    	{
            ResultSet rs = stmt.executeQuery("SELECT RDB$GENERATOR_NAME FROM RDB$GENERATORS");

            while (rs.next())
            {
                Column column = (Column)names.get(rs.getString(1).trim());

                if (column != null)
                {
                    column.setAutoIncrement(true);
                }
            }
    		rs.close();
    	}
        finally
        {
            stmt.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Collection readPrimaryKeyNames(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        List      pks   = new ArrayList();
        ResultSet pkData = null;

        try
        {
            if (getPlatformInfo().isUseDelimitedIdentifiers())
        	{
        		// Jaybird has a problem when delimited identifiers are used as
        		// it is not able to find the primary key info for the table
        		// So we have to filter manually below
	            pkData = metaData.getPrimaryKeys(getDefaultTablePattern());
	            while (pkData.next())
	            {
	                Map values = readColumns(pkData, getColumnsForPK());
	
                    if (tableName.equals(values.get("TABLE_NAME")))
                    {
                    	pks.add(readPrimaryKeyName(metaData, values));
                    }
	            }
        	}
            else
            {
	            pkData = metaData.getPrimaryKeys(tableName);
	            while (pkData.next())
	            {
	                Map values = readColumns(pkData, getColumnsForPK());
	
	                pks.add(readPrimaryKeyName(metaData, values));
	            }
            }
        }
        finally
        {
            if (pkData != null)
            {
                pkData.close();
            }
        }
        return pks;
    }

    /**
     * {@inheritDoc}
     */
    protected Collection readForeignKeys(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        Map       fks    = new ListOrderedMap();
        ResultSet fkData = null;

        try
        {
            if (getPlatformInfo().isUseDelimitedIdentifiers())
        	{
        		// Jaybird has a problem when delimited identifiers are used as
        		// it is not able to find the foreign key info for the table
        		// So we have to filter manually below
	            fkData = metaData.getForeignKeys(getDefaultTablePattern());
	            while (fkData.next())
	            {
	                Map values = readColumns(fkData, getColumnsForFK());
	
                    if (tableName.equals(values.get("FKTABLE_NAME")))
                    {
                    	readForeignKey(metaData, values, fks);
                    }
	            }
        	}
            else
            {
	            fkData = metaData.getForeignKeys(tableName);
	            while (fkData.next())
	            {
	                Map values = readColumns(fkData, getColumnsForFK());
	
	                readForeignKey(metaData, values, fks);
	            }
            }
        }
        finally
        {
            if (fkData != null)
            {
                fkData.close();
            }
        }
        return fks.values();
    }

    /**
     * {@inheritDoc}
     */
    protected Collection readIndices(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        // Jaybird is not able to read indices when delimited identifiers are turned on,
        // so we gather the data manually using Firebird's system tables
        Map          indices = new ListOrderedMap();
        StringBuffer query   = new StringBuffer();
        
        query.append("SELECT a.RDB$INDEX_NAME INDEX_NAME, b.RDB$RELATION_NAME TABLE_NAME, b.RDB$UNIQUE_FLAG NON_UNIQUE,");
        query.append(" a.RDB$FIELD_POSITION ORDINAL_POSITION, a.RDB$FIELD_NAME COLUMN_NAME, 3 INDEX_TYPE");
        query.append(" FROM RDB$INDEX_SEGMENTS a, RDB$INDICES b WHERE a.RDB$INDEX_NAME=b.RDB$INDEX_NAME AND b.RDB$RELATION_NAME = ?");

        PreparedStatement stmt      = getConnection().prepareStatement(query.toString());
        ResultSet         indexData = null;

        stmt.setString(1, getPlatformInfo().isUseDelimitedIdentifiers() ? tableName : tableName.toUpperCase());

        try 
        {
        	indexData = stmt.executeQuery();

            while (indexData.next())
            {
                Map values = readColumns(indexData, getColumnsForIndex());

                // we have to reverse the meaning of the unique flag
                values.put("NON_UNIQUE", Boolean.FALSE.equals(values.get("NON_UNIQUE")) ? Boolean.TRUE : Boolean.FALSE);
                // and trim the names
                values.put("INDEX_NAME",  ((String)values.get("INDEX_NAME")).trim());
                values.put("TABLE_NAME",  ((String)values.get("TABLE_NAME")).trim());
                values.put("COLUMN_NAME", ((String)values.get("COLUMN_NAME")).trim());
                readIndex(metaData, values, indices);
            }
        }
        finally
        {
            if (indexData != null)
            {
                indexData.close();
            }
        }
        return indices.values();
    }

    /**
	 * {@inheritDoc}
	 */
	protected boolean isInternalPrimaryKeyIndex(Table table, Index index)
	{
		// Firebird generates an unique index for the pks of the form "RDB$PRIMARY825"
		return index.getName().startsWith("RDB$PRIMARY");
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isInternalForeignKeyIndex(Table table, ForeignKey fk, Index index)
	{
		// Firebird generates a normal index that has the same name as the fk
		return fk.getName().equals(index.getName());
	}


}
