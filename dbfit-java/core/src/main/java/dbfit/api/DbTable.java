package dbfit.api;

import dbfit.fixture.StatementExecution;
import dbfit.util.DbParameterAccessor;
import dbfit.util.Direction;
import dbfit.util.NameNormaliser;

import java.sql.SQLException;
import java.util.Map;

import static dbfit.util.Direction.INPUT;
import static dbfit.util.Direction.OUTPUT;

public class DbTable implements DbObject {

    private DBEnvironment dbEnvironment;
    private String tableOrViewName;
    private Map<String, DbParameterAccessor> columns;

    public DbTable(DBEnvironment dbEnvironment, String tableName)
            throws SQLException {
        this.dbEnvironment = dbEnvironment;
        this.tableOrViewName = tableName;
        columns = dbEnvironment.getAllColumns(tableName);
        if (columns.isEmpty()) {
            throw new SQLException("Cannot retrieve list of columns for "
                    + tableName + " - check spelling and access rights");
        }
    }

    public StatementExecution buildPreparedStatement(
            DbParameterAccessor[] accessors) throws SQLException {
        StatementExecution statement = new StatementExecution(dbEnvironment
                .buildInsertPreparedStatement(tableOrViewName, accessors));

        for (int i = 0; i < accessors.length; i++) {
            accessors[i].bindTo(statement, i + 1);
        }
        return statement;
    }

    public DbParameterAccessor getDbParameterAccessor(String columnName,
            Direction expectedDirection) {
        String normalisedName = NameNormaliser.normaliseName(columnName);
        DbParameterAccessor accessor = columns.get(normalisedName);
        if (null == accessor) {
            throw new RuntimeException(
                    "No such database column or parameter: '" + normalisedName + "'");
        }

        if (accessor.hasDirection(INPUT)
                && expectedDirection == OUTPUT) {
            accessor = dbEnvironment
                    .createAutogeneratedPrimaryKeyAccessor(accessor);
        }
        return accessor;
    }

}
