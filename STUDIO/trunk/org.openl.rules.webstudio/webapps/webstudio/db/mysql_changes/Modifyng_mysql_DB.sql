ALTER TABLE AccessControlEntry RENAME TO  AccessControlEntry_tmp ;

ALTER TABLE Group2Group RENAME TO  Group2Group_tmp ;

ALTER TABLE OpenLUser RENAME TO  OpenLUser_tmp ;

ALTER TABLE UserGroup RENAME TO  UserGroup_tmp ;

CREATE TABLE OpenLUser (
  UserID int NOT NULL AUTO_INCREMENT,
  FirstName varchar(200) DEFAULT NULL,
  LoginName varchar(200) NOT NULL,
  Password varchar(200) NOT NULL,
  UserPivileges varchar(200) DEFAULT NULL,
  Surname varchar(200) DEFAULT NULL,
  PRIMARY KEY (UserID),
  UNIQUE KEY LoginName (LoginName)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO  OpenLUser SELECT * FROM OpenLUser_tmp;

CREATE TABLE UserGroup (
  GroupID int NOT NULL AUTO_INCREMENT,
  Description varchar(200) DEFAULT NULL,
  GroupName varchar(200) NOT NULL,
  UserPrivileges varchar(200),
  PRIMARY KEY (GroupID),
  UNIQUE KEY GroupName (GroupName)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO  UserGroup SELECT * FROM UserGroup_tmp;

CREATE TABLE Group2Group (
  IncludedGroupID int NOT NULL,
  GroupID int NOT NULL,
  PRIMARY KEY (GroupID,IncludedGroupID)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO  Group2Group SELECT * FROM Group2Group_tmp;

CREATE TABLE AccessControlEntry (
  ACEID int NOT NULL AUTO_INCREMENT,
  object varchar(200) NOT NULL,
  permission varchar(200) NOT NULL,
  GroupID int(200) DEFAULT NULL,
  UserID int(200) DEFAULT NULL,
  PRIMARY KEY (ACEID)
 ) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO  AccessControlEntry SELECT * FROM AccessControlEntry_tmp;

DROP TABLE AccessControlEntry_tmp;

DROP TABLE  Group2Group_tmp;

SET FOREIGN_KEY_CHECKS=0;

DROP TABLE  OpenLUser_tmp ;

DROP TABLE  UserGroup_tmp;

SET FOREIGN_KEY_CHECKS=1;