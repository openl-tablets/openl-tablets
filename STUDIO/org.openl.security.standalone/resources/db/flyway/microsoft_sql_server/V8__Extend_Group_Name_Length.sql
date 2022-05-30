/* Drop previously created PK for OpenL_External_Groups. Because it was created without specific name */
DECLARE @SQL VARCHAR(4000)
SET @SQL = 'ALTER TABLE OpenL_External_Groups DROP CONSTRAINT |ConstraintName| '
SET @SQL = REPLACE(@SQL, '|ConstraintName|', (SELECT name
                                              FROM sysobjects
                                              WHERE xtype = 'PK'
                                                AND parent_obj = OBJECT_ID('OpenL_External_Groups')))
EXEC (@SQL);

/* Alter groupName column of OpenL_External_Groups table */
ALTER TABLE OpenL_External_Groups
    ALTER COLUMN groupName ${varchar}(65) NOT NULL;

/* Create new composite PK for OpenL_External_Groups table */
ALTER TABLE OpenL_External_Groups
    ADD CONSTRAINT PK_OpenL_External_Group PRIMARY KEY (loginName, groupName);

/* Alter groupName column of OpenL_External_Groups table */
ALTER TABLE OpenL_Groups
    ALTER COLUMN groupName ${varchar}(65) not null;
